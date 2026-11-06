package com.openselena.omniget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openselena.omniget.ui.OmniGetApp
import com.openselena.omniget.ui.theme.OmniGetTheme
import com.openselena.omniget.util.PlatformDetector
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val incomingUrl = MutableStateFlow<String?>(null)
    private val shouldOpenDownloads = MutableStateFlow(false)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val app = application as OmniGetApplication

        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            val sharedUrl by incomingUrl.collectAsStateWithLifecycle()
            val openDownloads by shouldOpenDownloads.collectAsStateWithLifecycle()

            OmniGetTheme(mode = settings.theme) {
                OmniGetApp(
                    app = app,
                    sharedUrl = sharedUrl,
                    openDownloads = openDownloads,
                    onSharedUrlConsumed = { incomingUrl.value = null },
                    onOpenDownloadsConsumed = { shouldOpenDownloads.value = false },
                )
            }
        }
        requestNotificationPermissionIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val text = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        val url = text?.let {
            PlatformDetector.extractFirstUrl(it)
                ?: PlatformDetector.normalizeUrl(it).takeIf(PlatformDetector::isValidUrl)
        }
        if (url != null) incomingUrl.value = url
        if (intent.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) {
            shouldOpenDownloads.value = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_DOWNLOADS = "com.openselena.omniget.OPEN_DOWNLOADS"
    }
}
