package com.openselena.omniget.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromId(id: String?): ThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultMode: DownloadMode = DownloadMode.VIDEO,
    val defaultQuality: QualityPreset = QualityPreset.BEST,
    val writeSubtitles: Boolean = false,
    val allowPlaylist: Boolean = false,
    val cookiePath: String? = null,
)

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())

    val settings = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_THEME, next.theme.id)
            .putString(KEY_MODE, next.defaultMode.id)
            .putString(KEY_QUALITY, next.defaultQuality.id)
            .putBoolean(KEY_SUBTITLES, next.writeSubtitles)
            .putBoolean(KEY_PLAYLIST, next.allowPlaylist)
            .putString(KEY_COOKIES, next.cookiePath)
            .apply()
    }

    fun setCookiePath(path: String?) {
        update { it.copy(cookiePath = path) }
    }

    private fun load(): AppSettings = AppSettings(
        theme = ThemeMode.fromId(prefs.getString(KEY_THEME, null)),
        defaultMode = DownloadMode.fromId(prefs.getString(KEY_MODE, null)),
        defaultQuality = QualityPreset.fromId(prefs.getString(KEY_QUALITY, null)),
        writeSubtitles = prefs.getBoolean(KEY_SUBTITLES, false),
        allowPlaylist = prefs.getBoolean(KEY_PLAYLIST, false),
        cookiePath = prefs.getString(KEY_COOKIES, null),
    )

    private companion object {
        const val PREFS_NAME = "omniget.settings"
        const val KEY_THEME = "theme"
        const val KEY_MODE = "default_mode"
        const val KEY_QUALITY = "default_quality"
        const val KEY_SUBTITLES = "write_subtitles"
        const val KEY_PLAYLIST = "allow_playlist"
        const val KEY_COOKIES = "cookie_path"
    }
}
