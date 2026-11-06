package com.openselena.omniget.download

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.PlaybackStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.net.URL

object MediaThumbnails {
    suspend fun fromUrl(
        context: Context,
        url: String,
        headers: Map<String, String>,
        key: String,
    ): String? = withContext(Dispatchers.IO) {
        val target = thumbnailFile(context, key)
        if (target.isFile && target.length() > 0L) return@withContext target.asUriString()

        val host = runCatching { URI(url).host?.lowercase() }.getOrNull().orEmpty()
        val proxies = if (host.endsWith(".xhscdn.com")) {
            listOf(Proxy.NO_PROXY, null)
        } else {
            listOf(null)
        }
        proxies.firstNotNullOfOrNull { proxy ->
            download(target, url, headers, proxy)
        }
    }

    suspend fun fromRemote(
        context: Context,
        stream: PlaybackStream,
        key: String,
    ): String? = withContext(Dispatchers.IO) {
        val target = thumbnailFile(context, key)
        if (target.isFile && target.length() > 0L) return@withContext target.asUriString()
        extract(target) { retriever ->
            retriever.setDataSource(stream.url, stream.headers)
        }
    }

    suspend fun fromLocalFile(
        context: Context,
        file: File,
        key: String,
    ): String? = withContext(Dispatchers.IO) {
        val target = thumbnailFile(context, key)
        if (target.isFile && target.length() > 0L) return@withContext target.asUriString()
        extract(target) { retriever ->
            retriever.setDataSource(file.absolutePath)
        }
    }

    suspend fun fromTask(context: Context, task: DownloadTask): String? =
        withContext(Dispatchers.IO) {
            val target = thumbnailFile(context, "task-${task.id}")
            if (target.isFile && target.length() > 0L) return@withContext target.asUriString()
            extract(target) { retriever ->
                val uri = task.fileUri?.let(Uri::parse)
                when {
                    uri != null -> retriever.setDataSource(context, uri)
                    task.filePath != null -> retriever.setDataSource(task.filePath)
                    else -> error("No media source for thumbnail")
                }
            }
        }

    private fun download(
        target: File,
        url: String,
        headers: Map<String, String>,
        proxy: Proxy?,
    ): String? {
        val partial = File(target.parentFile, "${target.name}.part")
        return runCatching {
            val connection = (
                if (proxy == null) URL(url).openConnection()
                else URL(url).openConnection(proxy)
                ) as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            headers.forEach(connection::setRequestProperty)
            connection.connect()
            check(connection.responseCode in 200..299)
            check(connection.contentLengthLong <= MAX_REMOTE_BYTES)

            target.parentFile?.mkdirs()
            connection.inputStream.buffered().use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        check(total <= MAX_REMOTE_BYTES)
                        output.write(buffer, 0, read)
                    }
                }
            }
            connection.disconnect()
            check(partial.renameTo(target))
            target.asUriString()
        }.getOrNull().also {
            partial.delete()
        }
    }

    private fun extract(
        target: File,
        setDataSource: (MediaMetadataRetriever) -> Unit,
    ): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            setDataSource(retriever)
            val frame = retriever.getFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: retriever.frameAtTime ?: return null
            val cover = scaleDown(frame)
            target.parentFile?.mkdirs()
            target.outputStream().buffered().use { output ->
                cover.compress(Bitmap.CompressFormat.JPEG, 84, output)
            }
            if (cover !== frame) cover.recycle()
            frame.recycle()
            target.takeIf { it.length() > 0L }?.asUriString()
        } catch (_: Throwable) {
            target.delete()
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= MAX_COVER_EDGE) return bitmap
        val scale = MAX_COVER_EDGE.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun thumbnailFile(context: Context, key: String): File {
        val directory = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val safeKey = key.hashCode().toUInt().toString(16)
        return File(directory, "cover-$safeKey.jpg")
    }

    private fun File.asUriString(): String = Uri.fromFile(this).toString()

    private const val MAX_COVER_EDGE = 1280
    private const val MAX_REMOTE_BYTES = 12L * 1024L * 1024L
}
