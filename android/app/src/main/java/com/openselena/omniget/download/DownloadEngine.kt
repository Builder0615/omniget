package com.openselena.omniget.download

import android.content.Context
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

object DownloadEngine {
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

        val request = YoutubeDLRequest(url).apply {
            addCommonOptions(settings)
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

        MediaPreview(
            url = url,
            title = info.title?.takeIf(String::isNotBlank)
                ?: info.fulltitle?.takeIf(String::isNotBlank)
                ?: platform.label,
            uploader = info.uploader?.takeIf(String::isNotBlank)
                ?: info.extractorKey?.takeIf(String::isNotBlank)
                ?: "",
            platform = platform.id,
            thumbnailUrl = info.thumbnail,
            durationSeconds = info.duration,
            fileSizeApprox = approxSize,
            availableHeights = heights,
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
            addCommonOptions(settings)
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
            ?: "bundled"

    suspend fun update(context: Context): Boolean = withContext(Dispatchers.IO) {
        initialize(context)
        val result = YoutubeDL.updateYoutubeDL(context.applicationContext)
        result == YoutubeDL.UpdateStatus.DONE
    }

    private fun YoutubeDLRequest.addCommonOptions(settings: AppSettings) {
        addOption("--socket-timeout", "20")
        addOption("--retries", "5")
        addOption("--fragment-retries", "5")
        addOption("--no-warnings")
        settings.cookiePath?.takeIf { File(it).isFile }?.let { addOption("--cookies", it) }
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
