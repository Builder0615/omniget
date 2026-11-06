package com.openselena.omniget.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class DownloadMode(val id: String) {
    VIDEO("video"),
    AUDIO("audio");

    companion object {
        fun fromId(id: String?): DownloadMode =
            entries.firstOrNull { it.id == id } ?: VIDEO
    }
}

enum class QualityPreset(val id: String, val maxHeight: Int?) {
    BEST("best", null),
    P2160("2160", 2160),
    P1440("1440", 1440),
    P1080("1080", 1080),
    P720("720", 720),
    P480("480", 480),
    P360("360", 360);

    companion object {
        fun fromId(id: String?): QualityPreset =
            entries.firstOrNull { it.id == id } ?: BEST
    }
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val platform: String,
    val uploader: String = "",
    val thumbnailUrl: String? = null,
    val durationSeconds: Int = 0,
    val mode: DownloadMode = DownloadMode.VIDEO,
    val quality: QualityPreset = QualityPreset.BEST,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val speedBps: Long = 0L,
    val etaSeconds: Long = -1L,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val statusLine: String = "",
    val filePath: String? = null,
    val fileUri: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("title", title)
        put("platform", platform)
        put("uploader", uploader)
        put("thumbnailUrl", thumbnailUrl ?: JSONObject.NULL)
        put("durationSeconds", durationSeconds)
        put("mode", mode.id)
        put("quality", quality.id)
        put("status", status.name)
        put("progress", progress.toDouble())
        put("speedBps", speedBps)
        put("etaSeconds", etaSeconds)
        put("downloadedBytes", downloadedBytes)
        put("totalBytes", totalBytes)
        put("statusLine", statusLine)
        put("filePath", filePath ?: JSONObject.NULL)
        put("fileUri", fileUri ?: JSONObject.NULL)
        put("error", error ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DownloadTask = DownloadTask(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            url = json.optString("url"),
            title = json.optString("title"),
            platform = json.optString("platform", "generic"),
            uploader = json.optString("uploader"),
            thumbnailUrl = json.optStringOrNull("thumbnailUrl"),
            durationSeconds = json.optInt("durationSeconds"),
            mode = DownloadMode.fromId(json.optStringOrNull("mode")),
            quality = QualityPreset.fromId(json.optStringOrNull("quality")),
            status = runCatching {
                DownloadStatus.valueOf(json.optString("status", "QUEUED"))
            }.getOrDefault(DownloadStatus.QUEUED),
            progress = json.optDouble("progress", 0.0).toFloat(),
            speedBps = json.optLong("speedBps"),
            etaSeconds = json.optLong("etaSeconds", -1L),
            downloadedBytes = json.optLong("downloadedBytes"),
            totalBytes = json.optLong("totalBytes"),
            statusLine = json.optString("statusLine"),
            filePath = json.optStringOrNull("filePath"),
            fileUri = json.optStringOrNull("fileUri"),
            error = json.optStringOrNull("error"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        )

        fun listToJson(tasks: List<DownloadTask>): String =
            JSONArray().apply {
                tasks.forEach { put(it.toJson()) }
            }.toString()

        fun listFromJson(raw: String): List<DownloadTask> = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { add(fromJson(it)) }
                }
            }
        }.getOrDefault(emptyList())
    }
}

data class MediaPreview(
    val url: String,
    val title: String,
    val uploader: String,
    val platform: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
    val fileSizeApprox: Long,
    val availableHeights: List<Int>,
    val playbackVariants: List<PlaybackVariant> = emptyList(),
)

@Parcelize
data class PlaybackStream(
    val url: String,
    val mimeType: String?,
    val headers: Map<String, String> = emptyMap(),
) : Parcelable

@Parcelize
data class PlaybackVariant(
    val height: Int,
    val video: PlaybackStream,
    val audio: PlaybackStream? = null,
) : Parcelable

data class ParsedProgress(
    val percent: Float,
    val speedBps: Long,
    val etaSeconds: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val detail: String,
    val processing: Boolean,
)

data class EngineDownloadResult(
    val filePaths: List<String>,
    val output: String,
)

private fun JSONObject.optStringOrNull(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}
