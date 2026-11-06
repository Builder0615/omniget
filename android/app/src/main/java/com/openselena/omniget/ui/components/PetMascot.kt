package com.openselena.omniget.ui.components

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

enum class PetState(val assetName: String, val imageName: String) {
    IDLE("idle", "sleeping"),
    ANALYZING("analyzing", "analyzing"),
    COMPLETE("complete", "complete"),
    ERROR("error", "idle"),
}

@Composable
fun PetMascot(
    state: PetState,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val resolver = LocalContext.current.contentResolver
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animationsEnabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    var celebrationFinished by remember(state) { mutableStateOf(false) }
    val displayedState = if (state == PetState.COMPLETE && celebrationFinished) {
        PetState.IDLE
    } else {
        state
    }
    Crossfade(
        targetState = displayedState,
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        animationSpec = tween(if (animationsEnabled) 200 else 0),
        label = "pet-state",
    ) { displayedState ->
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("pet/${displayedState.assetName}.json"),
            imageAssetsFolder = "pet/images/",
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (composition == null || !animationsEnabled) {
                AsyncImage(
                    model = "file:///android_asset/pet/images/${displayedState.imageName}.png",
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxHeight(0.8f).aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
            } else {
                val animationState = animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = true,
                    restartOnPlay = true,
                    iterations = if (displayedState == PetState.COMPLETE) {
                        1
                    } else {
                        LottieConstants.IterateForever
                    },
                )
                LaunchedEffect(displayedState, animationState.isAtEnd) {
                    if (displayedState == PetState.COMPLETE && animationState.isAtEnd) {
                        celebrationFinished = true
                    }
                }
                LottieAnimation(
                    composition = composition,
                    progress = { animationState.progress },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
