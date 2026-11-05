package com.openselena.omniget.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class DownloadedFilesTest {
    @Test
    fun collectDownloadedFiles_keepsCompletedFilesAndSortsLargestFirst() {
        val directory = Files.createTempDirectory("omniget-files").toFile()
        try {
            val video = directory.resolve("video.mp4").apply { writeBytes(ByteArray(10)) }
            val subtitle = directory.resolve("video.srt").apply { writeText("subtitle") }
            directory.resolve("video.mp4.part").writeText("partial")
            directory.resolve("video.mp4.ytdl").writeText("state")

            val result = collectDownloadedFiles(
                output = "${subtitle.absolutePath}\n${video.absolutePath}\n${video.absolutePath}",
                directory = directory,
            )

            assertEquals(listOf(video.absolutePath, subtitle.absolutePath), result.map { it.absolutePath })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun collectDownloadedFiles_emptyOutputFindsMediaAfterProcessExit() {
        val directory = Files.createTempDirectory("omniget-files").toFile()
        try {
            val video = directory.resolve("downloaded.webm").apply { writeBytes(ByteArray(4)) }

            val result = collectDownloadedFiles("", directory)

            assertEquals(1, result.size)
            assertEquals(video.absolutePath, result.single().absolutePath)
            assertTrue(result.single().isFile)
        } finally {
            directory.deleteRecursively()
        }
    }
}
