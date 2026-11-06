package com.openselena.omniget.download

import com.openselena.omniget.data.PlaybackStream
import com.openselena.omniget.data.PlaybackVariant

data class PlaybackFormat(
    val url: String? = null,
    val manifestUrl: String? = null,
    val extension: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val height: Int = 0,
    val bitrateKbps: Int = 0,
    val headers: Map<String, String> = emptyMap(),
)

data class PlaybackSelection(
    val video: PlaybackStream,
    val audio: PlaybackStream? = null,
)

object PlaybackSelector {
    fun selectVariants(
        formats: List<PlaybackFormat>,
        fallback: PlaybackFormat? = null,
    ): List<PlaybackVariant> {
        val usable = formats.filter { it.resolvedUrl != null }
        val combined = usable
            .filter { it.isCombined }
            .groupBy { it.playbackHeight }
            .mapNotNull { (height, candidates) ->
                candidates.maxWithOrNull(combinedComparator)?.let { format ->
                    PlaybackVariant(
                        height = height,
                        video = format.asStream(audioOnly = false),
                    )
                }
            }
        val audio = usable
            .filter { it.isAudioOnly }
            .maxWithOrNull(audioComparator)
        val videoOnly = if (audio == null) {
            emptyList()
        } else {
            usable
                .filter { it.isVideoOnly }
                .groupBy { it.playbackHeight }
                .mapNotNull { (height, candidates) ->
                    val video = candidates.maxWithOrNull(videoComparator) ?: return@mapNotNull null
                    PlaybackVariant(
                        height = height,
                        video = video.asStream(audioOnly = false),
                        audio = audio.asStream(audioOnly = true),
                    )
                }
        }

        val variants = buildList {
            addAll(combined)
            videoOnly
                .filterNot { separate -> combined.any { it.height == separate.height } }
                .let(::addAll)
        }.sortedWith(
            compareByDescending<PlaybackVariant> { it.height > 0 }
                .thenByDescending(PlaybackVariant::height),
        )

        if (variants.isNotEmpty()) return variants

        return fallback
            ?.takeIf { it.resolvedUrl != null && !it.isLikelyAudioOnly() }
            ?.let {
                listOf(
                    PlaybackVariant(
                        height = it.playbackHeight,
                        video = it.asStream(audioOnly = false),
                    ),
                )
            }
            .orEmpty()
    }

    fun select(
        formats: List<PlaybackFormat>,
        fallback: PlaybackFormat? = null,
    ): PlaybackSelection? =
        selectVariants(formats, fallback)
            .firstOrNull()
            ?.let {
                PlaybackSelection(
                    video = it.video,
                    audio = it.audio,
                )
            }

    private val PlaybackFormat.playbackHeight: Int
        get() = height.coerceAtLeast(0)

    private val combinedComparator = compareBy<PlaybackFormat>(PlaybackFormat::height)
        .thenBy { extensionScore(it.effectiveExtension) }
        .thenBy(PlaybackFormat::bitrateKbps)

    private val videoComparator = compareBy<PlaybackFormat>(PlaybackFormat::height)
        .thenBy { extensionScore(it.effectiveExtension) }
        .thenBy(PlaybackFormat::bitrateKbps)

    private val audioComparator = compareBy<PlaybackFormat> {
        audioExtensionScore(it.effectiveExtension)
    }.thenBy(PlaybackFormat::bitrateKbps)

    private val PlaybackFormat.resolvedUrl: String?
        get() = url.normalizedHttpUrl() ?: manifestUrl.normalizedHttpUrl()

    private val PlaybackFormat.effectiveExtension: String?
        get() = extension.normalizedExtension()
            ?: resolvedUrl?.let(::extensionFromUrl)

    private val PlaybackFormat.videoCodecIsPresent: Boolean
        get() = videoCodec.isCodecPresent()

    private val PlaybackFormat.audioCodecIsPresent: Boolean
        get() = audioCodec.isCodecPresent()

    private val PlaybackFormat.isCombined: Boolean
        get() {
            if (videoCodecIsPresent && audioCodecIsPresent) return true
            return videoCodec.isNullOrBlank() &&
                audioCodec.isNullOrBlank() &&
                effectiveExtension in setOf("m3u8", "mpd")
        }

    private val PlaybackFormat.isVideoOnly: Boolean
        get() = videoCodecIsPresent && audioCodec.equals("none", ignoreCase = true)

    private val PlaybackFormat.isAudioOnly: Boolean
        get() = audioCodecIsPresent && videoCodec.equals("none", ignoreCase = true)

    private fun PlaybackFormat.isLikelyAudioOnly(): Boolean =
        effectiveExtension in setOf("m4a", "mp3", "opus", "ogg", "oga", "aac", "flac", "wav")

    private fun PlaybackFormat.asStream(audioOnly: Boolean): PlaybackStream =
        PlaybackStream(
            url = requireNotNull(resolvedUrl),
            mimeType = mimeTypeFor(effectiveExtension, audioOnly),
            headers = headers.filterKeys(String::isNotBlank).mapValues { it.value.trim() },
        )

    private fun String?.isCodecPresent(): Boolean =
        !isNullOrBlank() && !equals("none", ignoreCase = true)

    private fun String?.normalizedHttpUrl(): String? {
        val value = this?.trim()?.takeIf(String::isNotBlank) ?: return null
        return value.takeIf {
            it.startsWith("https://", ignoreCase = true) ||
                it.startsWith("http://", ignoreCase = true)
        }
    }

    private fun String?.normalizedExtension(): String? =
        this?.trim()?.removePrefix(".")?.lowercase()?.takeIf(String::isNotBlank)

    private fun extensionFromUrl(url: String): String? {
        val path = url.substringBefore('?').substringBefore('#')
        return path.substringAfterLast('/', "")
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf(String::isNotBlank)
    }

    private fun extensionScore(extension: String?): Int = when (extension) {
        "mp4", "m4v" -> 60
        "webm" -> 50
        "mkv" -> 40
        "mov" -> 30
        "m3u8" -> 20
        "mpd" -> 10
        else -> 0
    }

    private fun audioExtensionScore(extension: String?): Int = when (extension) {
        "m4a", "mp4", "m4b" -> 50
        "opus" -> 40
        "webm" -> 38
        "mp3" -> 30
        "aac" -> 25
        "ogg", "oga" -> 20
        "flac", "wav" -> 10
        else -> 0
    }

    private fun mimeTypeFor(extension: String?, audioOnly: Boolean): String? = when (extension) {
        "mp4", "m4v" -> if (audioOnly) "audio/mp4" else "video/mp4"
        "m4a", "m4b" -> "audio/mp4"
        "webm" -> if (audioOnly) "audio/webm" else "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "m3u8" -> "application/x-mpegURL"
        "mpd" -> "application/dash+xml"
        "mp3" -> "audio/mpeg"
        "opus" -> "audio/opus"
        "aac" -> "audio/aac"
        "ogg", "oga" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        else -> null
    }
}
