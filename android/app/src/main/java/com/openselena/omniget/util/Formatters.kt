package com.openselena.omniget.util

import com.openselena.omniget.data.ParsedProgress
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
    fun bytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        val pattern = if (value >= 100 || unit == 0) "%.0f %s" else "%.1f %s"
        return String.format(Locale.US, pattern, value, units[unit])
    }

    fun speed(bytesPerSecond: Long): String =
        if (bytesPerSecond <= 0L) "" else "${bytes(bytesPerSecond)}/s"

    fun duration(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return ""
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun eta(seconds: Long): String =
        if (seconds < 0L) "" else duration(seconds).ifBlank { "0:00" }

    fun playbackTime(milliseconds: Long): String =
        duration((milliseconds.coerceAtLeast(0L)) / 1000L).ifBlank { "0:00" }
}

object ProgressParser {
    private val sizePattern = Regex("""([\d.]+)\s*([KMGTP]?i?B)""", RegexOption.IGNORE_CASE)
    private val percentPattern = Regex("""(\d+(?:\.\d+)?)%""")
    private val speedPattern = Regex("""at\s+([\d.]+)\s*([KMGTP]?i?B)/s""", RegexOption.IGNORE_CASE)
    private val etaPattern = Regex("""ETA\s+(\d+):(\d+)(?::(\d+))?""", RegexOption.IGNORE_CASE)

    fun parse(line: String, callbackPercent: Float, callbackEta: Long): ParsedProgress {
        val clean = line.replace("\r", "").trim()
        val upper = clean.uppercase(Locale.US)
        val processing = upper.contains("[MERGER]") ||
            upper.contains("[EXTRACT]") ||
            upper.contains("[FIXUP") ||
            upper.contains("[METADATA]") ||
            upper.contains("[EMBED") ||
            upper.contains("[CONVERT")

        val percent = percentPattern.find(clean)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            ?: callbackPercent.takeIf { it in 0f..100f }
            ?: 0f

        val sizes = sizePattern.findAll(clean).map { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val unit = match.groupValues[2].uppercase(Locale.US)
            (value * multiplier(unit)).roundToLong()
        }.toList()

        val speedMatch = speedPattern.find(clean)
        val speed = speedMatch?.let {
            val value = it.groupValues[1].toDoubleOrNull() ?: 0.0
            (value * multiplier(it.groupValues[2].uppercase(Locale.US))).roundToLong()
        } ?: 0L

        val eta = etaPattern.find(clean)?.let { match ->
            val first = match.groupValues[1].toLongOrNull() ?: 0L
            val second = match.groupValues[2].toLongOrNull() ?: 0L
            val third = match.groupValues[3].toLongOrNull()
            if (third != null) first * 3600 + second * 60 + third else first * 60 + second
        } ?: callbackEta.takeIf { it >= 0L } ?: -1L

        val total = sizes.maxOrNull() ?: 0L
        val downloaded = if (total > 0L && percent > 0f) {
            (total * (percent / 100.0)).roundToLong()
        } else {
            0L
        }

        return ParsedProgress(
            percent = percent.coerceIn(0f, 100f),
            speedBps = speed,
            etaSeconds = eta,
            downloadedBytes = downloaded,
            totalBytes = total,
            detail = clean.take(220),
            processing = processing,
        )
    }

    private fun multiplier(unit: String): Double = when (unit.uppercase(Locale.US)) {
        "KIB", "KB" -> 1024.0
        "MIB", "MB" -> 1024.0 * 1024
        "GIB", "GB" -> 1024.0 * 1024 * 1024
        "TIB", "TB" -> 1024.0 * 1024 * 1024 * 1024
        "PIB", "PB" -> 1024.0 * 1024 * 1024 * 1024 * 1024
        else -> 1.0
    }
}
