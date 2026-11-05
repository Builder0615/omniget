package com.openselena.omniget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformDetectorTest {
    @Test
    fun normalizeUrl_addsHttpsToBareDomains() {
        assertEquals("https://youtu.be/example", PlatformDetector.normalizeUrl(" youtu.be/example "))
        assertEquals("https://example.com/file.zip", PlatformDetector.normalizeUrl("example.com/file.zip"))
    }

    @Test
    fun extractFirstUrl_findsLinkInsideSharedText() {
        val shared = "看看这个视频 https://www.bilibili.com/video/BV12345，很有意思。"

        assertEquals("https://www.bilibili.com/video/BV12345", PlatformDetector.extractFirstUrl(shared))
        assertNull(PlatformDetector.extractFirstUrl("没有链接"))
    }

    @Test
    fun isValidUrl_rejectsUnsupportedInputs() {
        assertTrue(PlatformDetector.isValidUrl("https://example.com/watch?v=1"))
        assertFalse(PlatformDetector.isValidUrl("ftp://example.com/file"))
        assertFalse(PlatformDetector.isValidUrl("not a url"))
    }

    @Test
    fun detect_recognizesKnownDomainsAndSubdomains() {
        assertEquals("youtube", PlatformDetector.detect("https://m.youtube.com/watch?v=1").id)
        assertEquals("bilibili", PlatformDetector.detect("b23.tv/abc").id)
    }

    @Test
    fun detect_doesNotMatchLookalikeDomains() {
        assertEquals("generic", PlatformDetector.detect("https://youtube.com.example.org/watch").id)
    }

    @Test
    fun detect_recognizesDirectMediaAndArchives() {
        assertEquals("direct", PlatformDetector.detect("https://cdn.example.com/video.MP4?token=1").id)
        assertEquals("direct", PlatformDetector.detect("https://cdn.example.com/archive.zip#download").id)
    }

    @Test
    fun infoForUnknownId_returnsStableFallback() {
        val info = PlatformDetector.infoForId("unknown")

        assertEquals("unknown", info.id)
        assertEquals("Unknown", info.label)
    }
}
