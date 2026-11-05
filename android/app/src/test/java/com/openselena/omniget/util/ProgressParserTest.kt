package com.openselena.omniget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressParserTest {
    @Test
    fun parse_readsYtDlpDownloadLine() {
        val result = ProgressParser.parse(
            "[download]  42.5% of 10.00MiB at 1.50MiB/s ETA 00:04",
            callbackPercent = 0f,
            callbackEta = -1L,
        )

        assertEquals(42.5f, result.percent, 0.01f)
        assertEquals(1_572_864L, result.speedBps)
        assertEquals(4L, result.etaSeconds)
        assertEquals(10_485_760L, result.totalBytes)
        assertEquals(4_456_448L, result.downloadedBytes)
        assertFalse(result.processing)
    }

    @Test
    fun parse_supportsLongEtaAndMebibytes() {
        val result = ProgressParser.parse(
            "[download] 100.0% of 2.5GiB at 8.2MiB/s ETA 01:02:03",
            callbackPercent = 0f,
            callbackEta = -1L,
        )

        assertEquals(3723L, result.etaSeconds)
        assertEquals(2_684_354_560L, result.totalBytes)
        assertEquals(8_598_323L, result.speedBps)
    }

    @Test
    fun parse_keepsCallbackValuesWhenLineHasNoMetrics() {
        val result = ProgressParser.parse(
            "[Merger] Merging formats into output.mkv",
            callbackPercent = 73.5f,
            callbackEta = 12L,
        )

        assertEquals(73.5f, result.percent, 0.01f)
        assertEquals(12L, result.etaSeconds)
        assertTrue(result.processing)
    }

    @Test
    fun parse_clampsOutOfRangePercent() {
        val result = ProgressParser.parse("[download] 125.0%", 0f, -1L)

        assertEquals(100f, result.percent, 0.01f)
    }
}
