package com.openselena.omniget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.openselena.omniget.data.PlaybackVariant
import com.openselena.omniget.data.ThemeMode
import com.openselena.omniget.ui.components.EmbeddedPlayer
import com.openselena.omniget.ui.components.PlayerExitSnapshot
import com.openselena.omniget.ui.theme.OmniGetTheme

class FullscreenPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
        }
        hideSystemBars()

        val variants = readVariants(intent)
        if (variants.isEmpty()) {
            finish()
            return
        }
        val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
        val variantUrl = intent.getStringExtra(EXTRA_VARIANT_URL)
        val positionMs = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
        val playbackSpeed = intent.getFloatExtra(EXTRA_PLAYBACK_SPEED, 1f)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)

        setContent {
            OmniGetTheme(mode = ThemeMode.DARK) {
                EmbeddedPlayer(
                    variants = variants,
                    coverUrl = coverUrl,
                    modifier = Modifier.fillMaxSize(),
                    initialVariantUrl = variantUrl,
                    initialPositionMs = positionMs,
                    initialSpeed = playbackSpeed,
                    autoPlay = autoPlay,
                    isDedicatedFullscreen = true,
                    onDedicatedExit = ::finishWithSnapshot,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun finishWithSnapshot(snapshot: PlayerExitSnapshot) {
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(RESULT_VARIANT_URL, snapshot.variantUrl)
                putExtra(RESULT_POSITION_MS, snapshot.positionMs)
                putExtra(RESULT_PLAYBACK_SPEED, snapshot.playbackSpeed)
                putExtra(RESULT_WAS_PLAYING, snapshot.wasPlaying)
            },
        )
        finish()
    }

    companion object {
        private const val EXTRA_VARIANTS = "playback_variants"
        private const val EXTRA_COVER_URL = "cover_url"
        private const val EXTRA_VARIANT_URL = "variant_url"
        private const val EXTRA_POSITION_MS = "position_ms"
        private const val EXTRA_PLAYBACK_SPEED = "playback_speed"
        private const val EXTRA_AUTO_PLAY = "auto_play"

        private const val RESULT_VARIANT_URL = "result_variant_url"
        private const val RESULT_POSITION_MS = "result_position_ms"
        private const val RESULT_PLAYBACK_SPEED = "result_playback_speed"
        private const val RESULT_WAS_PLAYING = "result_was_playing"

        fun createIntent(
            context: Context,
            variants: List<PlaybackVariant>,
            coverUrl: String?,
            variantUrl: String,
            positionMs: Long,
            playbackSpeed: Float,
            autoPlay: Boolean,
        ): Intent = Intent(context, FullscreenPlayerActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_VARIANTS, ArrayList(variants))
            putExtra(EXTRA_COVER_URL, coverUrl)
            putExtra(EXTRA_VARIANT_URL, variantUrl)
            putExtra(EXTRA_POSITION_MS, positionMs)
            putExtra(EXTRA_PLAYBACK_SPEED, playbackSpeed)
            putExtra(EXTRA_AUTO_PLAY, autoPlay)
        }

        fun readResult(intent: Intent): PlayerExitSnapshot? {
            val variantUrl = intent.getStringExtra(RESULT_VARIANT_URL) ?: return null
            return PlayerExitSnapshot(
                variantUrl = variantUrl,
                positionMs = intent.getLongExtra(RESULT_POSITION_MS, 0L),
                playbackSpeed = intent.getFloatExtra(RESULT_PLAYBACK_SPEED, 1f),
                wasPlaying = intent.getBooleanExtra(RESULT_WAS_PLAYING, false),
            )
        }

        @Suppress("DEPRECATION")
        private fun readVariants(intent: Intent): List<PlaybackVariant> =
            intent.getParcelableArrayListExtra<PlaybackVariant>(EXTRA_VARIANTS).orEmpty()
    }
}
