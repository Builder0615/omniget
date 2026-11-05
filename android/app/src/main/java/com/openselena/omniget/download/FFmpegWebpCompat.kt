package com.openselena.omniget.download

import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal object FFmpegWebpCompat {
    private const val SIXTEEN_KB = 16 * 1024
    private val libraries = listOf(
        "libsharpyuv.so",
        "libwebp.so",
        "libwebpdecoder.so",
        "libwebpdemux.so",
        "libwebpmux.so",
    )
    private val supportedAbis = setOf("arm64-v8a", "x86_64")

    fun applyIfNeeded(context: Context) {
        if (Os.sysconf(OsConstants._SC_PAGESIZE) < SIXTEEN_KB) return

        val abi = Build.SUPPORTED_ABIS.firstOrNull(supportedAbis::contains)
            ?: error("No 16 KB WebP compatibility build for this ABI")
        val libraryDirectory = File(
            context.noBackupFilesDir,
            "youtubedl-android/packages/ffmpeg/usr/lib",
        )
        check(libraryDirectory.isDirectory) {
            "FFmpeg libraries were not extracted"
        }

        libraries.forEach { name ->
            val assetPath = "ffmpeg16k/$abi/$name"
            val target = File(libraryDirectory, name)
            check(target.isFile) { "Missing extracted FFmpeg library: $name" }

            val expectedHash = context.assets.open(assetPath).use { it.sha256() }
            if (target.inputStream().use { it.sha256() }.contentEquals(expectedHash)) {
                return@forEach
            }

            val replacement = File(libraryDirectory, ".$name.16k.tmp")
            try {
                context.assets.open(assetPath).use { input ->
                    replacement.outputStream().use { output -> input.copyTo(output) }
                }
                Files.move(
                    replacement.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                replacement.delete()
            }
        }
    }

    private fun InputStream.sha256(): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest()
    }
}
