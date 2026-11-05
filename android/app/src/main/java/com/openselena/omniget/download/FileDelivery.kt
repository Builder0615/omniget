package com.openselena.omniget.download

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.openselena.omniget.data.DownloadTask
import java.io.File

data class DeliveredFile(
    val localPath: String?,
    val uri: String,
)

data class DeliveredBatch(
    val primary: DeliveredFile,
    val allPublished: Boolean,
)

object FileDelivery {
    fun stagingDirectory(context: Context): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download")
        return File(root, "OmniGet").apply { mkdirs() }
    }

    fun taskStagingDirectory(context: Context, taskId: String): File =
        File(stagingDirectory(context), taskId).apply { mkdirs() }

    fun deliverAll(context: Context, files: List<File>): DeliveredBatch {
        val ordered = files.filter(File::isFile).sortedByDescending(File::length)
        require(ordered.isNotEmpty()) { "No completed files to deliver" }
        val delivered = ordered.map { deliver(context, it) }
        return DeliveredBatch(
            primary = delivered.first(),
            allPublished = delivered.all { it.localPath == null },
        )
    }

    fun deliver(context: Context, file: File): DeliveredFile {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return DeliveredFile(file.absolutePath, fileProviderUri(context, file).toString())
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType(file.name))
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniGet")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: run {
            return DeliveredFile(file.absolutePath, fileProviderUri(context, file).toString())
        }

        return runCatching {
            resolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output) { "Unable to open MediaStore output" }
                file.inputStream().use { input -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
            }
            val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, ready, null, null)
            file.delete()
            DeliveredFile(null, uri.toString())
        }.getOrElse {
            resolver.delete(uri, null, null)
            DeliveredFile(file.absolutePath, fileProviderUri(context, file).toString())
        }
    }

    fun openIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolveMimeType(context, uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun shareIntent(context: Context, uri: Uri, title: String): Intent {
        val displayName = displayName(context, uri) ?: title
        return Intent(Intent.ACTION_SEND).apply {
            type = resolveMimeType(context, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, displayName)
            clipData = ClipData.newUri(context.contentResolver, displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun uriForTask(context: Context, task: DownloadTask): Uri? {
        task.fileUri?.let { return Uri.parse(it) }
        val local = task.filePath?.let(::File)?.takeIf(File::isFile) ?: return null
        return runCatching { fileProviderUri(context, local) }.getOrNull()
    }

    fun delete(context: Context, localPath: String?, uriString: String?) {
        localPath?.let { runCatching { File(it).delete() } }
        uriString?.let {
            val uri = Uri.parse(it)
            if (uri.scheme == "content") {
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }

    fun deleteStagingFiles(context: Context, taskId: String) {
        val root = stagingDirectory(context)
        File(root, taskId).deleteRecursively()
        root
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith("$taskId-") }
            .forEach { runCatching { it.delete() } }
    }

    private fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    private fun resolveMimeType(context: Context, uri: Uri): String =
        context.contentResolver.getType(uri)
            ?.takeUnless { it == "application/octet-stream" }
            ?: mimeType(uri.lastPathSegment.orEmpty())

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(0)
            }
    }.getOrNull()

    private fun mimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "mkv" -> "video/x-matroska"
                "m4a" -> "audio/mp4"
                "opus" -> "audio/opus"
                "webm" -> "video/webm"
                "srt" -> "application/x-subrip"
                "vtt" -> "text/vtt"
                else -> "application/octet-stream"
            }
    }
}
