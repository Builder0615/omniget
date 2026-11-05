package com.openselena.omniget

import android.app.Application
import com.openselena.omniget.data.DownloadRepository
import com.openselena.omniget.data.SettingsRepository

class OmniGetApplication : Application() {
    lateinit var downloads: DownloadRepository
        private set
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        downloads = DownloadRepository(this)
    }
}
