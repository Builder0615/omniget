package com.openselena.omniget.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.openselena.omniget.FullscreenPlayerActivity
import com.openselena.omniget.R
import com.openselena.omniget.data.PlaybackStream
import com.openselena.omniget.data.PlaybackVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

data class PlayerExitSnapshot(
    val variantUrl: String,
    val positionMs: Long,
    val playbackSpeed: Float,
    val wasPlaying: Boolean,
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun EmbeddedPlayer(
    variants: List<PlaybackVariant>,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    initialVariantUrl: String? = null,
    initialPositionMs: Long = 0L,
    initialSpeed: Float = 1f,
    autoPlay: Boolean = true,
    isDedicatedFullscreen: Boolean = false,
    onDedicatedExit: (PlayerExitSnapshot) -> Unit = {},
) {
    if (variants.isEmpty()) return

    val context = LocalContext.current
    var selectedUrl by remember(variants, initialVariantUrl) {
        mutableStateOf(
            initialVariantUrl?.takeIf { url -> variants.any { it.video.url == url } }
                ?: variants.first().video.url,
        )
    }
    val selected = variants.firstOrNull { it.video.url == selectedUrl } ?: variants.first()
    var resumePosition by remember { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var playWhenReady by remember { mutableStateOf(autoPlay) }
    var playbackSpeed by remember { mutableFloatStateOf(initialSpeed) }
    var retryToken by remember { mutableIntStateOf(0) }
    val player = remember(selected.video.url, selected.audio?.url, retryToken) {
        createPlayer(context, selected)
    }
    var playbackError by remember(selected.video.url, retryToken) {
        mutableStateOf<PlaybackException?>(null)
    }
    var hasRenderedFrame by remember(player) { mutableStateOf(false) }
    var positionMs by remember(player) { mutableLongStateOf(0L) }
    var durationMs by remember(player) { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember(player) { mutableLongStateOf(0L) }
    var isPlaying by remember(player) { mutableStateOf(true) }
    var isBuffering by remember(player) { mutableStateOf(true) }
    var controlsVisible by remember(player) { mutableStateOf(true) }
    var controlsHeld by remember(player) { mutableStateOf(false) }
    var controlsInteraction by remember(player) { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun revealControls() {
        controlsVisible = true
        controlsInteraction += 1
    }

    val fullscreenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val exitState = result.data?.let(FullscreenPlayerActivity::readResult)
            if (exitState != null) {
                playbackSpeed = exitState.playbackSpeed
                resumePosition = exitState.positionMs
                playWhenReady = exitState.wasPlaying
                if (exitState.variantUrl != selected.video.url) {
                    selectedUrl = exitState.variantUrl
                } else {
                    player.setPlaybackSpeed(exitState.playbackSpeed)
                    player.seekTo(exitState.positionMs)
                    if (exitState.wasPlaying) player.play() else player.pause()
                    revealControls()
                }
            }
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
                isBuffering = false
                controlsVisible = true
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFrame = true
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        if (resumePosition > 0L) player.seekTo(resumePosition)
        player.setPlaybackSpeed(playbackSpeed)
        player.prepare()
        player.playWhenReady = playWhenReady
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
            isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            isBuffering = player.playbackState == Player.STATE_BUFFERING
            delay(250L)
        }
    }

    LaunchedEffect(player, isPlaying, isBuffering) {
        if (!isPlaying || isBuffering) controlsVisible = true
    }

    LaunchedEffect(
        player,
        controlsVisible,
        controlsHeld,
        controlsInteraction,
        isPlaying,
        isBuffering,
    ) {
        if (controlsVisible && isPlaying && !isBuffering && !controlsHeld) {
            delay(3_200L)
            controlsVisible = false
        }
    }

    fun switchVariant(variant: PlaybackVariant) {
        if (variant.video.url == selected.video.url) return
        resumePosition = player.currentPosition.coerceAtLeast(0L)
        playWhenReady = player.playWhenReady
        selectedUrl = variant.video.url
        revealControls()
    }

    fun retryPlayback() {
        resumePosition = player.currentPosition.coerceAtLeast(0L)
        playWhenReady = true
        retryToken += 1
        revealControls()
    }

    fun exitSnapshot(): PlayerExitSnapshot = PlayerExitSnapshot(
        variantUrl = selected.video.url,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        playbackSpeed = playbackSpeed,
        wasPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
    )

    BackHandler(enabled = isDedicatedFullscreen) {
        onDedicatedExit(exitSnapshot())
    }

    val controls: @Composable BoxScope.(Boolean) -> Unit = { isFullscreen ->
        PlayerControls(
            selected = selected,
            variants = variants,
            isPlaying = isPlaying,
            isVisible = controlsVisible,
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            playbackSpeed = playbackSpeed,
            isFullscreen = isFullscreen,
            onInteraction = ::revealControls,
            onHoldChanged = { held -> controlsHeld = held },
            onTogglePlay = {
                revealControls()
                val ended = player.playbackState == Player.STATE_ENDED
                if (ended) {
                    player.seekTo(0L)
                }
                if (ended || !player.playWhenReady) player.play() else player.pause()
            },
            onSeek = { position ->
                player.seekTo(position)
                revealControls()
            },
            onSpeedChange = { speed ->
                playbackSpeed = speed
                player.setPlaybackSpeed(speed)
                revealControls()
            },
            onSelectVariant = ::switchVariant,
            onToggleFullscreen = {
                revealControls()
                if (isDedicatedFullscreen) {
                    onDedicatedExit(exitSnapshot())
                } else {
                    fullscreenLauncher.launch(
                        FullscreenPlayerActivity.createIntent(
                            context = context,
                            variants = variants,
                            coverUrl = coverUrl,
                            variantUrl = selected.video.url,
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            playbackSpeed = playbackSpeed,
                            autoPlay = player.playWhenReady &&
                                player.playbackState != Player.STATE_ENDED,
                        ),
                    )
                }
            },
        )
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            coverUrl = coverUrl,
            showCover = !hasRenderedFrame || !isPlaying,
            modifier = Modifier.fillMaxSize(),
        )
        PlayerTapLayer(
            onTap = {
                if (controlsVisible && isPlaying && !controlsHeld) {
                    controlsVisible = false
                } else {
                    revealControls()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (isBuffering && playbackError == null) {
            BufferingIndicator(Modifier.align(Alignment.Center))
        }
        if (playbackError == null) controls(isDedicatedFullscreen)
        playbackError?.let {
            PlaybackErrorOverlay(
                error = it,
                onRetry = ::retryPlayback,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerSurface(
    player: ExoPlayer,
    coverUrl: String?,
    showCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.player = player
                view.keepScreenOn = player.playWhenReady
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (showCover) {
            CoverLayer(coverUrl, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CoverLayer(coverUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF28282B), Color(0xFF0D0D0F)),
            ),
        ),
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
private fun BoxScope.PlayerControls(
    selected: PlaybackVariant,
    variants: List<PlaybackVariant>,
    isPlaying: Boolean,
    isVisible: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    onInteraction: () -> Unit,
    onHoldChanged: (Boolean) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSelectVariant: (PlaybackVariant) -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    var openMenu by remember(selected.video.url) { mutableStateOf<PlayerMenu?>(null) }
    var scrubbing by remember(selected.video.url) { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (!isVisible) openMenu = null
    }
    LaunchedEffect(openMenu, scrubbing) {
        onHoldChanged(openMenu != null || scrubbing)
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(animationSpec = tween(120)),
        exit = fadeOut(animationSpec = tween(180)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!isPlaying) {
                PlayerControlButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(58.dp),
                    shape = CircleShape,
                ) {
                    SvgIcon(
                        icon = AppIcon.PLAY,
                        contentDescription = stringResource(R.string.play),
                        tint = Color.White,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.34f to Color.Black.copy(alpha = 0.12f),
                            0.68f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    )
                    .padding(
                        start = if (isFullscreen) 18.dp else 8.dp,
                        end = if (isFullscreen) 18.dp else 6.dp,
                        top = if (isFullscreen) 42.dp else 30.dp,
                        bottom = if (isFullscreen) 8.dp else 3.dp,
                    ),
            ) {
                PlayerSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    onSeek = onSeek,
                    onInteraction = onInteraction,
                    onScrubbingChanged = { scrubbing = it },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerIconButton(
                        icon = if (isPlaying) AppIcon.PAUSE else AppIcon.PLAY,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.pause else R.string.play,
                        ),
                        onClick = onTogglePlay,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = playerTime(positionMs),
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = " / ${playerTime(durationMs)}",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    QualityPicker(
                        selected = selected,
                        variants = variants,
                        opened = openMenu == PlayerMenu.QUALITY,
                        onOpen = {
                            onInteraction()
                            openMenu = PlayerMenu.QUALITY
                        },
                        onDismiss = { openMenu = null },
                        onSelect = {
                            openMenu = null
                            onSelectVariant(it)
                        },
                    )
                    SpeedPicker(
                        selectedSpeed = playbackSpeed,
                        opened = openMenu == PlayerMenu.SPEED,
                        onOpen = {
                            onInteraction()
                            openMenu = PlayerMenu.SPEED
                        },
                        onDismiss = { openMenu = null },
                        onSelect = {
                            openMenu = null
                            onSpeedChange(it)
                        },
                    )
                    PlayerIconButton(
                        icon = if (isFullscreen) AppIcon.CONTRACT else AppIcon.EXPAND,
                        contentDescription = stringResource(
                            if (isFullscreen) R.string.exit_fullscreen else R.string.enter_fullscreen,
                        ),
                        onClick = onToggleFullscreen,
                    )
                }
            }
        }
    }
}

private enum class PlayerMenu { QUALITY, SPEED }

private val playbackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val playerAccent = Color(0xFF00AEEC)
private val playerMenuBackground = Color(0xF218181B)

@Composable
private fun PlayerTapLayer(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnTap by rememberUpdatedState(onTap)
    Box(
        modifier.pointerInput(Unit) {
            detectTapGestures { latestOnTap() }
        },
    )
}

@Composable
private fun BufferingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.44f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(25.dp),
            color = Color.White.copy(alpha = 0.92f),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    onInteraction: () -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnInteraction by rememberUpdatedState(onInteraction)
    val latestOnScrubbingChanged by rememberUpdatedState(onScrubbingChanged)
    val density = LocalDensity.current
    val edgeInsetPx = with(density) { 3.dp.toPx() }
    val trackFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bufferedFraction = if (durationMs > 0L) {
        (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val visibleFraction = if (scrubbing) scrubFraction else trackFraction

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        val bubbleWidth = 44.dp
        val bubbleX = (maxWidth * visibleFraction - bubbleWidth / 2)
            .coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp))

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        fun fractionAt(x: Float): Float =
                            ((x - edgeInsetPx) / (size.width - edgeInsetPx * 2f))
                                .coerceIn(0f, 1f)

                        var pointerFraction = fractionAt(down.position.x)
                        scrubFraction = pointerFraction
                        scrubbing = true
                        latestOnScrubbingChanged(true)
                        latestOnInteraction()
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                pointerFraction = fractionAt(change.position.x)
                                scrubFraction = pointerFraction
                                change.consume()
                                if (!change.pressed) break
                            }
                            latestOnSeek((pointerFraction * durationMs).toLong())
                        } finally {
                            scrubbing = false
                            latestOnScrubbingChanged(false)
                            latestOnInteraction()
                        }
                    }
                },
        ) {
            val left = edgeInsetPx
            val right = size.width - edgeInsetPx
            val width = (right - left).coerceAtLeast(1f)
            val centerY = size.height - 7.dp.toPx()
            val bufferX = left + width * bufferedFraction
            val progressX = left + width * visibleFraction

            drawLine(
                color = Color.White.copy(alpha = 0.24f),
                start = Offset(left, centerY),
                end = Offset(right, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            if (bufferedFraction > 0f) {
                drawLine(
                    color = Color.White.copy(alpha = 0.54f),
                    start = Offset(left, centerY),
                    end = Offset(bufferX, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            if (visibleFraction > 0f) {
                drawLine(
                    color = playerAccent,
                    start = Offset(left, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = (if (scrubbing) 6.dp else 4.5.dp).toPx(),
                center = Offset(progressX, centerY),
            )
            drawCircle(
                color = playerAccent,
                radius = (if (scrubbing) 4.5.dp else 3.dp).toPx(),
                center = Offset(progressX, centerY),
            )
        }

        if (scrubbing) {
            Box(
                modifier = Modifier
                    .offset(x = bubbleX)
                    .width(bubbleWidth)
                    .height(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xE619191C)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = playerTime((durationMs * scrubFraction).toLong()),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun QualityPicker(
    selected: PlaybackVariant,
    variants: List<PlaybackVariant>,
    opened: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (PlaybackVariant) -> Unit,
) {
    val autoQuality = stringResource(R.string.playback_auto_quality)
    Box {
        PlayerTextButton(
            label = qualityLabel(selected, autoQuality),
            opened = opened,
            showChevron = variants.size > 1,
            enabled = variants.size > 1,
            onClick = {
                if (opened) onDismiss() else onOpen()
            },
        )
        if (opened) {
            PlayerPopupMenu(
                title = stringResource(R.string.quality),
                options = variants.map {
                    PlayerMenuOption(
                        label = qualityLabel(it, autoQuality),
                        selected = it.video.url == selected.video.url,
                    )
                },
                onDismiss = onDismiss,
                onSelect = { index -> onSelect(variants[index]) },
            )
        }
    }
}

@Composable
private fun SpeedPicker(
    selectedSpeed: Float,
    opened: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
) {
    Box {
        PlayerTextButton(
            label = speedLabel(selectedSpeed),
            opened = opened,
            showChevron = true,
            onClick = {
                if (opened) onDismiss() else onOpen()
            },
        )
        if (opened) {
            PlayerPopupMenu(
                title = stringResource(R.string.playback_speed),
                options = playbackSpeeds.map {
                    PlayerMenuOption(label = speedLabel(it), selected = it == selectedSpeed)
                },
                menuWidth = 118.dp,
                onDismiss = onDismiss,
                onSelect = { index -> onSelect(playbackSpeeds[index]) },
            )
        }
    }
}

private data class PlayerMenuOption(
    val label: String,
    val selected: Boolean,
)

@Composable
private fun PlayerPopupMenu(
    title: String,
    options: List<PlayerMenuOption>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    menuWidth: Dp = 138.dp,
) {
    val shape = RoundedCornerShape(8.dp)
    val offsetY = with(LocalDensity.current) { -(6.dp.roundToPx()) }
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(menuWidth)
                .shadow(18.dp, shape, clip = false)
                .clip(shape)
                .background(playerMenuBackground)
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), shape)
                .heightIn(max = 320.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 13.dp, top = 10.dp, end = 13.dp, bottom = 7.dp),
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, option ->
                    PlayerControlButton(
                        onClick = { onSelect(index) },
                        selected = option.selected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .height(34.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                        Text(
                            text = option.label,
                            color = if (option.selected) playerAccent else Color.White.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Spacer(Modifier.weight(1f))
                        if (option.selected) {
                            SvgIcon(
                                icon = AppIcon.CHECK_CIRCLE,
                                contentDescription = null,
                                tint = playerAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTextButton(
    label: String,
    opened: Boolean,
    showChevron: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PlayerControlButton(
        onClick = onClick,
        enabled = enabled,
        selected = opened,
        modifier = Modifier.height(34.dp),
    ) {
        Row(Modifier.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.46f)
                opened -> playerAccent
                else -> Color.White.copy(alpha = 0.9f)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        if (showChevron) {
            Spacer(Modifier.width(2.dp))
            SvgIcon(
                icon = AppIcon.CHEVRON_DOWN,
                contentDescription = null,
                tint = if (opened) playerAccent else Color.White.copy(alpha = 0.64f),
                modifier = Modifier.size(11.dp),
            )
        }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
) {
    PlayerControlButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        SvgIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PlayerControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(5.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val recessed = pressed || selected
    val surface by animateColorAsState(
        if (recessed) Color(0xEB101014) else Color(0xCC2C2C32),
        tween(120),
        label = "player-button-surface",
    )
    val elevation by animateDpAsState(
        if (recessed) 0.dp else 2.dp,
        tween(120),
        label = "player-button-relief",
    )
    val upperEdge by animateColorAsState(
        if (recessed) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.25f),
        tween(120),
        label = "player-button-upper-edge",
    )
    val lowerEdge by animateColorAsState(
        if (recessed) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.8f),
        tween(120),
        label = "player-button-lower-edge",
    )
    Box(
        modifier = modifier
            .pressMotion(interaction)
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .background(surface, shape)
            .border(0.7.dp, Brush.verticalGradient(listOf(upperEdge, lowerEdge)), shape)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun qualityLabel(variant: PlaybackVariant, autoLabel: String): String =
    if (variant.height > 0) "${variant.height}P" else autoLabel

private fun speedLabel(speed: Float): String = when (speed) {
    0.5f -> "0.5X"
    0.75f -> "0.75X"
    1f -> "1.0X"
    1.25f -> "1.25X"
    1.5f -> "1.5X"
    2f -> "2.0X"
    else -> "${speed}X"
}

private fun playerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun PlaybackErrorOverlay(
    error: PlaybackException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SvgIcon(
            icon = AppIcon.ERROR,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = stringResource(R.string.playback_failed),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = error.errorCodeName,
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(13.dp))
        PlayerControlButton(onClick = onRetry, shape = RoundedCornerShape(999.dp)) {
            Text(
                text = stringResource(R.string.retry_playback),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun createPlayer(
    context: android.content.Context,
    variant: PlaybackVariant,
): ExoPlayer {
    val attributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()
    return ExoPlayer.Builder(context)
        .setAudioAttributes(attributes, true)
        .build()
        .apply {
            val videoSource = createMediaSource(context, variant.video)
            val source = variant.audio?.let {
                MergingMediaSource(videoSource, createMediaSource(context, it))
            } ?: videoSource
            setMediaSource(source)
        }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun createMediaSource(
    context: android.content.Context,
    stream: PlaybackStream,
): MediaSource {
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)))
        .setAllowCrossProtocolRedirects(true)
        .apply {
            if (stream.headers.isNotEmpty()) {
                setDefaultRequestProperties(stream.headers)
            }
        }
    val item = MediaItem.Builder()
        .setUri(stream.url)
        .apply {
            stream.mimeType?.let { setMimeType(it) }
        }
        .build()
    val mimeType = stream.mimeType.orEmpty()
    val isHls = mimeType.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) ||
        stream.url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
    val isDash = mimeType.equals(MimeTypes.APPLICATION_MPD, ignoreCase = true) ||
        stream.url.substringBefore('?').endsWith(".mpd", ignoreCase = true)

    return when {
        isHls -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        isDash -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
    }
}
