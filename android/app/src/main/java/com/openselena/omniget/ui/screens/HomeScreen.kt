package com.openselena.omniget.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.MediaPreview
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.ui.components.AppIcon
import com.openselena.omniget.ui.components.EmbeddedPlayer
import com.openselena.omniget.ui.components.ModeSegmented
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.PetMascot
import com.openselena.omniget.ui.components.PetState
import com.openselena.omniget.ui.components.PaperButton
import com.openselena.omniget.ui.components.PaperIconButton
import com.openselena.omniget.ui.components.PlatformBadge
import com.openselena.omniget.ui.components.SvgIcon
import com.openselena.omniget.ui.components.qualityLabel
import com.openselena.omniget.ui.components.paperWell
import com.openselena.omniget.ui.vm.HomeEvent
import com.openselena.omniget.ui.vm.HomeUiState
import com.openselena.omniget.ui.vm.HomeViewModel
import com.openselena.omniget.util.Formatters
import com.openselena.omniget.util.PlatformDetector
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current
    val addedText = stringResource(R.string.download_added)
    val preview = state.preview
    var showDownloadOptions by rememberSaveable(preview?.url) { mutableStateOf(false) }
    val petState = when {
        state.isAnalyzing -> PetState.ANALYZING
        preview != null -> PetState.COMPLETE
        state.error != null -> PetState.ERROR
        else -> PetState.IDLE
    }
    val analyze = {
        focusManager.clearFocus()
        viewModel.analyze()
    }

    LaunchedEffect(viewModel, addedText) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.AddedToQueue -> snackbarHostState.showSnackbar(addedText)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = preview,
            contentKey = { it?.url },
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(300)) { it / 32 }) togetherWith fadeOut(tween(140))
            },
            contentAlignment = Alignment.Center,
            label = "parsed-result",
        ) { shownPreview ->
        if (shownPreview != null && landscape) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(0.9f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HomePet(petState, Modifier.fillMaxWidth().height(140.dp))
                    Omnibox(state, viewModel::onUrlChanged, analyze)
                }
                Column(
                    modifier = Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PreviewCard(shownPreview, onDownload = { showDownloadOptions = true })
                }
            }
        } else if (shownPreview != null) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item { HomePet(petState, Modifier.fillMaxWidth().height(116.dp)) }
                    item { Omnibox(state, viewModel::onUrlChanged, analyze) }
                    item { PreviewCard(shownPreview, onDownload = { showDownloadOptions = true }) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (landscape) 12.dp else 28.dp),
            ) {
                HomePet(petState, Modifier.fillMaxWidth().height(if (landscape) 144.dp else 224.dp))
                Omnibox(state, viewModel::onUrlChanged, analyze)
                state.error?.let { error -> AnalysisErrorCard(error, analyze) }
            }
        }
        }
    }

    if (showDownloadOptions && preview != null) {
        DownloadOptionsSheet(
            preview = preview,
            initialMode = state.mode,
            initialQuality = state.quality,
            onDismiss = { showDownloadOptions = false },
            onConfirm = { mode, quality ->
                showDownloadOptions = false
                viewModel.download(mode, quality)
            },
        )
    }
}

@Composable
private fun HomePet(state: PetState, modifier: Modifier) {
    val description = stringResource(
        when (state) {
            PetState.IDLE -> R.string.pet_idle
            PetState.ANALYZING -> R.string.pet_analyzing
            PetState.COMPLETE -> R.string.pet_complete
            PetState.ERROR -> R.string.pet_error
        },
    )
    PetMascot(state, modifier, contentDescription = description)
}

@Composable
private fun Omnibox(
    state: HomeUiState,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().paperWell(CircleShape).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (state.isValidUrl) {
                    PlatformBadge(state.platform.id, size = 24.dp)
                } else {
                    SvgIcon(AppIcon.LINK, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            BasicTextField(
                value = state.url,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onAnalyze() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (state.url.isBlank()) {
                            Text(
                                stringResource(R.string.url_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (state.url.isNotEmpty()) {
                PaperIconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                    SvgIcon(
                        AppIcon.CLOSE,
                        stringResource(R.string.clear_input),
                        Modifier.size(18.dp),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PaperIconButton(
                onClick = onAnalyze,
                accent = true,
                enabled = state.isValidUrl && !state.isAnalyzing,
                modifier = Modifier
                    .size(44.dp)
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                    )
                } else {
                    SvgIcon(
                        AppIcon.ARROW_FORWARD,
                        stringResource(R.string.analyze),
                        Modifier.size(21.dp),
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisErrorCard(error: String, onRetry: () -> Unit) {
    OmniCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.preview_error), style = MaterialTheme.typography.titleMedium)
                Text(
                    error,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PaperIconButton(onClick = onRetry) {
                SvgIcon(AppIcon.REFRESH, stringResource(R.string.retry_analysis), Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PreviewCard(preview: MediaPreview, onDownload: () -> Unit) {
    val playbackKey = preview.playbackVariants.firstOrNull()?.video?.url
    var playing by remember(preview.url, playbackKey) { mutableStateOf(false) }
    val downloadInteraction = remember { MutableInteractionSource() }
    OmniCard(Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(13.dp)).background(Color.Black),
            ) {
                if (playing && preview.playbackVariants.isNotEmpty()) {
                    EmbeddedPlayer(preview.playbackVariants, preview.thumbnailUrl, Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = preview.thumbnailUrl,
                        contentDescription = preview.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))),
                        ),
                    )
                    if (preview.playbackVariants.isNotEmpty()) {
                        PaperIconButton(
                            onClick = { playing = true },
                            modifier = Modifier.align(Alignment.Center).size(56.dp),
                        ) {
                            SvgIcon(AppIcon.PLAY, stringResource(R.string.play_preview), Modifier.size(26.dp), MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (preview.durationSeconds > 0) PreviewChip(Formatters.duration(preview.durationSeconds.toLong()))
                        PreviewChip(PlatformDetector.infoForId(preview.platform).label)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                Text(
                    preview.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.uploader.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview.uploader,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PaperButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    accent = true,
                ) {
                    SvgIcon(AppIcon.DOWNLOAD, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.download))
                }
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String) {
    Text(
        label,
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.54f)).padding(horizontal = 7.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DownloadOptionsSheet(
    preview: MediaPreview,
    initialMode: DownloadMode,
    initialQuality: QualityPreset,
    onDismiss: () -> Unit,
    onConfirm: (DownloadMode, QualityPreset) -> Unit,
) {
    val qualities = remember(preview.availableHeights) {
        listOf(QualityPreset.BEST) + QualityPreset.entries.filter {
            it.maxHeight != null && it.maxHeight in preview.availableHeights
        }
    }
    var mode by rememberSaveable(preview.url) { mutableStateOf(initialMode) }
    var quality by rememberSaveable(preview.url) {
        mutableStateOf(initialQuality.takeIf { it in qualities } ?: QualityPreset.BEST)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }
    val confirmInteraction = remember { MutableInteractionSource() }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.84f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0xFF6E5537).copy(alpha = 0.16f),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight).animateContentSize(tween(260)).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(stringResource(R.string.download_options), style = MaterialTheme.typography.headlineSmall)
                Text(
                    preview.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ModeSegmented(mode, onModeChange = { mode = it }, modifier = Modifier.fillMaxWidth())
                AnimatedVisibility(
                    visible = mode == DownloadMode.VIDEO,
                    enter = fadeIn(tween(180)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(220)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.quality), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            qualities.forEach { preset ->
                                PaperButton(
                                    selected = quality == preset,
                                    onClick = { quality = preset },
                                    modifier = Modifier.padding(vertical = 5.dp),
                                ) { Text(qualityLabel(preset)) }
                            }
                        }
                    }
                }
            }
            PaperButton(
                onClick = {
                    confirming = true
                    scope.launch {
                        sheetState.hide()
                        onConfirm(mode, quality)
                    }
                },
                enabled = !confirming,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                accent = true,
            ) {
                SvgIcon(AppIcon.DOWNLOAD, null, Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_download))
                if (mode == DownloadMode.AUDIO) Text(" · M4A")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
