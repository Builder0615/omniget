package com.openselena.omniget.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSelectorTest {
    @Test
    fun select_prefersHighestQualityCombinedStream() {
        val selection = PlaybackSelector.select(
            listOf(
                format(url = "https://cdn.test/720.mp4", ext = "mp4", height = 720, tbr = 2500),
                format(url = "https://cdn.test/1080.webm", ext = "webm", height = 1080, tbr = 4000),
            ),
        )

        assertNotNull(selection)
        assertEquals("https://cdn.test/1080.webm", selection?.video?.url)
        assertNull(selection?.audio)
    }

    @Test
    fun select_ignoresVideoOnlyTracksWhenNoAudioTrackExists() {
        val selection = PlaybackSelector.select(
            listOf(
                format(
                    url = "https://cdn.test/video-only.mp4",
                    ext = "mp4",
                    height = 1080,
                    acodec = "none",
                ),
            ),
        )

        assertNull(selection)
    }

    @Test
    fun select_mergesBestVideoAndAudioStreams() {
        val selection = PlaybackSelector.select(
            listOf(
                format(
                    url = "https://cdn.test/video-720.mp4",
                    ext = "mp4",
                    height = 720,
                    acodec = "none",
                    tbr = 2000,
                ),
                format(
                    url = "https://cdn.test/video-1080.mp4",
                    ext = "mp4",
                    height = 1080,
                    acodec = "none",
                    tbr = 3500,
                ),
                format(
                    url = "https://cdn.test/audio-64.m4a",
                    ext = "m4a",
                    vcodec = "none",
                    tbr = 64,
                ),
                format(
                    url = "https://cdn.test/audio-128.m4a",
                    ext = "m4a",
                    vcodec = "none",
                    tbr = 128,
                ),
            ),
        )

        assertEquals("https://cdn.test/video-1080.mp4", selection?.video?.url)
        assertEquals("https://cdn.test/audio-128.m4a", selection?.audio?.url)
        assertEquals("audio/mp4", selection?.audio?.mimeType)
    }

    @Test
    fun select_usesGenericFallbackUrlForSingleFileMedia() {
        val selection = PlaybackSelector.select(
            formats = emptyList(),
            fallback = PlaybackFormat(
                url = "https://cdn.test/movie.mp4?token=abc",
                extension = "mp4",
                height = 360,
            ),
        )

        assertEquals("https://cdn.test/movie.mp4?token=abc", selection?.video?.url)
        assertEquals("video/mp4", selection?.video?.mimeType)
    }

    @Test
    fun select_supportsManifestOnlyFormats() {
        val selection = PlaybackSelector.select(
            listOf(
                PlaybackFormat(
                    manifestUrl = "https://cdn.test/master.m3u8?token=abc",
                    extension = "m3u8",
                ),
            ),
        )

        assertEquals("https://cdn.test/master.m3u8?token=abc", selection?.video?.url)
        assertEquals("application/x-mpegURL", selection?.video?.mimeType)
    }

    @Test
    fun selectVariants_exposesDistinctPlaybackQualities() {
        val variants = PlaybackSelector.selectVariants(
            listOf(
                format(url = "https://cdn.test/1080.mp4", ext = "mp4", height = 1080, tbr = 4000),
                format(url = "https://cdn.test/720.webm", ext = "webm", height = 720, tbr = 2500),
                format(url = "https://cdn.test/720-backup.mp4", ext = "mp4", height = 720, tbr = 2200),
            ),
        )

        assertEquals(listOf(1080, 720), variants.map { it.height })
        assertEquals("https://cdn.test/720-backup.mp4", variants[1].video.url)
    }

    private fun format(
        url: String,
        ext: String,
        height: Int = 0,
        tbr: Int = 0,
        vcodec: String = "avc1",
        acodec: String = "mp4a",
    ) = PlaybackFormat(
        url = url,
        extension = ext,
        videoCodec = vcodec,
        audioCodec = acodec,
        height = height,
        bitrateKbps = tbr,
    )
}
