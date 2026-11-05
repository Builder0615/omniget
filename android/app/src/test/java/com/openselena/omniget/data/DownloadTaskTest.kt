package com.openselena.omniget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadTaskTest {
    @Test
    fun jsonRoundTrip_preservesQueueState() {
        val original = DownloadTask(
            id = "task-1",
            url = "https://example.com/video",
            title = "A \"quoted\" title",
            platform = "youtube",
            uploader = "Creator",
            thumbnailUrl = "https://example.com/cover.jpg",
            durationSeconds = 123,
            mode = DownloadMode.AUDIO,
            quality = QualityPreset.P360,
            status = DownloadStatus.PAUSED,
            progress = 42.5f,
            speedBps = 1024,
            etaSeconds = 8,
            downloadedBytes = 2048,
            totalBytes = 4096,
            statusLine = "Paused",
            filePath = "/tmp/output.m4a",
            fileUri = "content://downloads/1",
            error = null,
            createdAt = 100,
            updatedAt = 200,
        )

        val restored = DownloadTask.listFromJson(DownloadTask.listToJson(listOf(original))).single()

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(DownloadMode.AUDIO, restored.mode)
        assertEquals(QualityPreset.P360, restored.quality)
        assertEquals(DownloadStatus.PAUSED, restored.status)
        assertEquals(42.5f, restored.progress, 0.01f)
        assertEquals(original.fileUri, restored.fileUri)
        assertNull(restored.error)
        assertEquals(100L, restored.createdAt)
        assertEquals(200L, restored.updatedAt)
    }

    @Test
    fun malformedState_restoresAsEmptyQueue() {
        assertTrue(DownloadTask.listFromJson("not json").isEmpty())
        assertTrue(DownloadTask.listFromJson("{}").isEmpty())
    }
}
