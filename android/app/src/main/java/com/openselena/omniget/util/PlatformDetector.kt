package com.openselena.omniget.util

import java.net.URI

data class PlatformInfo(
    val id: String,
    val label: String,
    val color: Long,
)

object PlatformDetector {
    private data class KnownPlatform(
        val info: PlatformInfo,
        val domains: List<String>,
    )

    private val platforms = listOf(
        KnownPlatform(PlatformInfo("youtube", "YouTube", 0xFFFF0033), listOf("youtube.com", "youtube-nocookie.com", "youtu.be")),
        KnownPlatform(PlatformInfo("bilibili", "Bilibili", 0xFFFB7299), listOf("bilibili.com", "bilibili.tv", "b23.tv")),
        KnownPlatform(PlatformInfo("tiktok", "TikTok", 0xFF25F4EE), listOf("tiktok.com")),
        KnownPlatform(PlatformInfo("instagram", "Instagram", 0xFFE1306C), listOf("instagram.com", "ddinstagram.com")),
        KnownPlatform(PlatformInfo("twitter", "X", 0xFF8B98A5), listOf("x.com", "twitter.com", "vxtwitter.com", "fixvx.com")),
        KnownPlatform(PlatformInfo("reddit", "Reddit", 0xFFFF4500), listOf("reddit.com", "redd.it", "v.redd.it")),
        KnownPlatform(PlatformInfo("twitch", "Twitch", 0xFF9146FF), listOf("twitch.tv")),
        KnownPlatform(PlatformInfo("pinterest", "Pinterest", 0xFFE60023), listOf("pinterest.com", "pin.it")),
        KnownPlatform(PlatformInfo("bluesky", "Bluesky", 0xFF0085FF), listOf("bsky.app")),
        KnownPlatform(PlatformInfo("vimeo", "Vimeo", 0xFF1AB7EA), listOf("vimeo.com")),
        KnownPlatform(PlatformInfo("telegram", "Telegram", 0xFF2AABEE), listOf("t.me", "telegram.me", "telegram.org")),
        KnownPlatform(PlatformInfo("douyin", "抖音", 0xFFFE2C55), listOf("douyin.com", "iesdouyin.com", "amemv.com")),
        KnownPlatform(PlatformInfo("xiaohongshu", "小红书", 0xFFFF2442), listOf("xiaohongshu.com", "xhslink.com")),
        KnownPlatform(PlatformInfo("kuaishou", "快手", 0xFFFF5000), listOf("kuaishou.com")),
        KnownPlatform(PlatformInfo("youku", "优酷", 0xFF1E90FF), listOf("youku.com")),
        KnownPlatform(PlatformInfo("iqiyi", "爱奇艺", 0xFF00BE06), listOf("iqiyi.com")),
        KnownPlatform(PlatformInfo("qq", "腾讯视频", 0xFF00A4FF), listOf("v.qq.com")),
        KnownPlatform(PlatformInfo("mgtv", "芒果TV", 0xFFFF7A00), listOf("mgtv.com")),
        KnownPlatform(PlatformInfo("udemy", "Udemy", 0xFFA435F0), listOf("udemy.com")),
        KnownPlatform(PlatformInfo("hotmart", "Hotmart", 0xFFF04E23), listOf("hotmart.com")),
    )

    private val directExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "zst", "exe", "msi", "dmg",
        "pkg", "deb", "rpm", "appimage", "apk", "iso", "pdf", "epub", "mobi", "doc", "docx",
        "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "txt", "csv", "jpg", "jpeg",
        "png", "gif", "webp", "bmp", "tiff", "svg", "heic", "avif", "bin", "dat", "img", "jar",
        "whl", "cab", "tbz", "lz4", "mp4", "mkv", "webm", "mov", "mp3", "m4a", "flac", "wav",
        "ogg", "opus",
    )

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim().trim('"', '\'', '<', '>')
        if (trimmed.isBlank()) return ""
        if (trimmed.contains("://")) return trimmed
        if (trimmed.startsWith("www.") || trimmed.contains('.') && !trimmed.contains(' ')) {
            return "https://$trimmed"
        }
        return trimmed
    }

    fun extractFirstUrl(input: String): String? {
        val match = Regex(
            """https?://[^\s<>"'，。；：！？、（）【】《》「」『』]+""",
            RegexOption.IGNORE_CASE,
        ).find(input)
        return match?.value
            ?.trimEnd('.', ',', ';', ':', ')', ']', '}', '!', '?')
            ?.let(::normalizeUrl)
    }

    fun isValidUrl(input: String): Boolean {
        val url = normalizeUrl(input)
        return runCatching {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    fun detect(input: String): PlatformInfo {
        val url = normalizeUrl(input)
        val host = runCatching { URI(url).host?.lowercase().orEmpty() }.getOrDefault("")
        val known = platforms.firstOrNull { platform ->
            platform.domains.any { domain -> host == domain || host.endsWith(".$domain") }
        }
        if (known != null) return known.info

        val extension = runCatching {
            URI(url).path.substringAfterLast('.', "").lowercase()
        }.getOrDefault("")
        if (extension in directExtensions) {
            return PlatformInfo("direct", "Direct file", 0xFF30D158)
        }
        return PlatformInfo("generic", "Web", 0xFF0A84FF)
    }

    fun infoForId(id: String): PlatformInfo {
        if (id == "direct") return PlatformInfo("direct", "Direct file", 0xFF30D158)
        if (id == "generic") return PlatformInfo("generic", "Web", 0xFF0A84FF)
        return platforms.firstOrNull { it.info.id == id }?.info
            ?: PlatformInfo(id, id.replaceFirstChar(Char::uppercase), 0xFF0A84FF)
    }
}
