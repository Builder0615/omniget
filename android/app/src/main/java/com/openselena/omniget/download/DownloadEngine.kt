package com.openselena.omniget.download

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.openselena.omniget.data.AppSettings
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.EngineDownloadResult
import com.openselena.omniget.data.MediaPreview
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.util.PlatformDetector
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.net.URL

object DownloadEngine {
    private const val TAG = "DownloadEngine"
    private const val PREVIEW_PROCESS_ID = "omniget-preview"
    private val initializationMutex = Mutex()

    @Volatile
    private var initialized = false

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initializationMutex.withLock {
            if (initialized) return@withLock
            YoutubeDL.init(context.applicationContext)
            FFmpeg.init(context.applicationContext)
            FFmpegWebpCompat.applyIfNeeded(context.applicationContext)
            initialized = true
        }
    }

    suspend fun inspect(
        context: Context,
        url: String,
        settings: AppSettings,
    ): MediaPreview = withContext(Dispatchers.IO) {
        initialize(context)
        YoutubeDL.destroyProcessById(PREVIEW_PROCESS_ID)
        val resolvedUrl = resolveUrlForExtraction(url)

        val request = YoutubeDLRequest(resolvedUrl).apply {
            addCommonOptions(context, settings)
            addOption("--dump-json")
            addOption("--no-playlist")
            addOption("--skip-download")
        }
        val response = YoutubeDL.getInstance().execute(request, PREVIEW_PROCESS_ID)
        val info = YoutubeDL.getInstance().objectMapper.readValue(response.out, VideoInfo::class.java)
        val heights = info.formats.orEmpty()
            .mapNotNull { it.height.takeIf { height -> height > 0 } }
            .distinct()
            .sortedDescending()
        val approxSize = info.fileSizeApproximate.takeIf { it > 0L } ?: info.fileSize
        val platform = PlatformDetector.detect(url)
        val playbackFormats = info.formats.orEmpty().map { format ->
            PlaybackFormat(
                url = format.url,
                manifestUrl = format.manifestUrl,
                extension = format.ext,
                videoCodec = format.vcodec,
                audioCodec = format.acodec,
                height = format.height,
                bitrateKbps = format.tbr.takeIf { it > 0 } ?: format.abr,
                headers = format.httpHeaders.orEmpty(),
            )
        }
        val playbackFallback = PlaybackFormat(
            url = info.url,
            manifestUrl = info.manifestUrl,
            extension = info.ext,
            height = info.height,
            headers = info.httpHeaders.orEmpty(),
        )
        val playbackVariants = PlaybackSelector.selectVariants(playbackFormats, playbackFallback)
        val thumbnailUrl = info.thumbnail?.takeIf(String::isNotBlank)?.let { remote ->
            MediaThumbnails.fromUrl(
                context = context,
                url = remote,
                headers = info.httpHeaders.orEmpty(),
                key = "preview-cover:$resolvedUrl:$remote",
            )
        } ?: playbackVariants.firstOrNull()?.let { variant ->
                MediaThumbnails.fromRemote(
                    context = context,
                    stream = variant.video,
                    key = "preview:${url}:${variant.video.url}",
                )
            }

        MediaPreview(
            url = resolvedUrl,
            title = info.title?.takeIf(String::isNotBlank)
                ?: info.fulltitle?.takeIf(String::isNotBlank)
                ?: platform.label,
            uploader = info.uploader?.takeIf(String::isNotBlank)
                ?: info.extractorKey?.takeIf(String::isNotBlank)
                ?: "",
            platform = PlatformDetector.detect(resolvedUrl).id,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = info.duration,
            fileSizeApprox = approxSize,
            availableHeights = heights,
            playbackVariants = playbackVariants,
        )
    }

    suspend fun download(
        context: Context,
        task: DownloadTask,
        settings: AppSettings,
        onProgress: (Float, Long, String) -> Unit,
    ): EngineDownloadResult = withContext(Dispatchers.IO) {
        initialize(context)
        val outputDirectory = FileDelivery.taskStagingDirectory(context, task.id)
        val request = YoutubeDLRequest(task.url).apply {
            addCommonOptions(context, settings)
            addOption("--newline")
            addOption("--no-mtime")
            addOption("--trim-filenames", "180")
            addOption("-o", "${outputDirectory.absolutePath}/%(title).120B.%(ext)s")
            addOption("--print", "after_move:filepath")

            if (!settings.allowPlaylist) {
                addOption("--no-playlist")
            }

            if (task.mode == DownloadMode.AUDIO) {
                addOption("-f", "ba/b")
                addOption("-x")
                addOption("--audio-format", "m4a")
                addOption("--audio-quality", "0")
            } else {
                addOption("-f", formatSelector(task.quality))
                addOption("--merge-output-format", "mp4")
                if (settings.writeSubtitles) {
                    addOption("--write-subs")
                    addOption("--write-auto-subs")
                    addOption("--sub-langs", "all,-live_chat")
                    addOption("--convert-subs", "srt")
                }
            }
        }

        val response = YoutubeDL.getInstance().execute(request, task.id) { progress, eta, line ->
            onProgress(progress, eta, line.orEmpty())
        }
        val files = collectDownloadedFiles(response.out, outputDirectory)
            .map(File::getAbsolutePath)

        EngineDownloadResult(
            filePaths = files,
            output = response.out,
        )
    }

    fun cancel(taskId: String) {
        YoutubeDL.destroyProcessById(taskId)
    }

    fun cancelPreview() {
        if (initialized) {
            YoutubeDL.destroyProcessById(PREVIEW_PROCESS_ID)
        }
    }

    fun version(context: Context): String =
        runCatching { YoutubeDL.versionName(context) }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
            ?.removePrefix("yt-dlp ")
            ?: "bundled"

    suspend fun update(context: Context): Boolean = withContext(Dispatchers.IO) {
        initialize(context)
        val result = YoutubeDL.updateYoutubeDL(context.applicationContext)
        result == YoutubeDL.UpdateStatus.DONE
    }

    private fun YoutubeDLRequest.addCommonOptions(context: Context, settings: AppSettings) {
        addOption("--socket-timeout", "20")
        addOption("--retries", "5")
        addOption("--fragment-retries", "5")
        addOption("--no-warnings")
        systemHttpProxy(context)?.let { proxy ->
            Log.d(TAG, "Using system HTTP proxy for yt-dlp: $proxy")
            addOption("--proxy", proxy)
        }
        settings.cookiePath?.takeIf { File(it).isFile }?.let { addOption("--cookies", it) }
    }

    private fun systemHttpProxy(context: Context): String? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivity?.activeNetwork
        val proxyInfo = network?.let(connectivity::getLinkProperties)?.httpProxy
        if (proxyInfo != null && proxyInfo.host.isNotBlank()) {
            formatProxyUrl(proxyInfo.host, proxyInfo.port)?.let { return it }
        }

        val selected = ProxySelector.getDefault()
            ?.select(URI("https://www.youtube.com"))
            ?.firstOrNull { it.type() == Proxy.Type.HTTP }
        val address = selected?.address() as? InetSocketAddress ?: return null
        val host = address.hostString ?: return null
        return formatProxyUrl(host, address.port)
    }

    private fun resolveUrlForExtraction(url: String): String {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull()
            ?: return url
        if (host != "xhslink.com" && host != "xhslink.cn") return url

        return runCatching {
            val connection = URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36",
            )
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")
            connection.connect()
            connection.responseCode
            connection.url.toString()
        }.getOrNull()?.takeIf { candidate ->
            candidate.contains("/explore/") || candidate.contains("/discovery/item/")
        } ?: url
    }

    private fun formatProxyUrl(host: String, port: Int): String? {
        if (host.isBlank() || port !in 1..65535) return null
        val normalizedHost = if (':' in host && !host.startsWith("[")) "[$host]" else host
        return "http://$normalizedHost:$port"
    }

    private fun formatSelector(quality: QualityPreset): String {
        val height = quality.maxHeight
        return if (height == null) {
            "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b"
        } else {
            "bv*[height<=$height][ext=mp4]+ba[ext=m4a]/b[height<=$height][ext=mp4]/bv*[height<=$height]+ba/b[height<=$height]"
        }
    }
}
