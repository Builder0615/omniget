package com.openselena.omniget.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.ui.components.BrandMark
import com.openselena.omniget.ui.components.ModeSegmented
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.PlatformBadge
import com.openselena.omniget.ui.components.RecentTaskRow
import com.openselena.omniget.ui.components.SectionHeader
import com.openselena.omniget.ui.components.qualityLabel
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.ui.vm.HomeEvent
import com.openselena.omniget.ui.vm.HomeUiState
import com.openselena.omniget.ui.vm.HomeViewModel
import com.openselena.omniget.util.Formatters
import com.openselena.omniget.util.PlatformDetector

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    tasks: List<DownloadTask>,
    snackbarHostState: SnackbarHostState,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    showBranding: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preview = state.preview
    val error = state.error
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardEmptyText = stringResource(R.string.paste_empty)
    val addedText = stringResource(R.string.download_added)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                HomeEvent.AddedToQueue -> addedText
                HomeEvent.ClipboardEmpty -> clipboardEmptyText
                is HomeEvent.Message -> event.text
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val mascot = when {
        tasks.any { it.status == DownloadStatus.DOWNLOADING } -> R.drawable.mascot_downloading
        tasks.any { it.status == DownloadStatus.FAILED } -> R.drawable.mascot_error
        tasks.any { it.status == DownloadStatus.COMPLETED } -> R.drawable.mascot_complete
        else -> R.drawable.mascot_idle
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (showBranding) item {
                HomeHeader(
                    activeCount = tasks.count {
                        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                    },
                    onOpenDownloads = onOpenDownloads,
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Crossfade(targetState = mascot, label = "mascot") { image ->
                        androidx.compose.foundation.Image(
                            painter = painterResource(image),
                            contentDescription = null,
                            modifier = Modifier
                                .size(132.dp)
                                .padding(4.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_eyebrow),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        modifier = Modifier.widthIn(max = 580.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Omnibox(
                    state = state,
                    onValueChange = viewModel::onUrlChanged,
                    onAnalyze = {
                        focusManager.clearFocus()
                        viewModel.analyze()
                    },
                    onPaste = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val pasted = clipboard?.primaryClip
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                        if (pasted.isBlank()) {
                            viewModel.clipboardEmpty()
                        } else {
                            viewModel.onUrlChanged(PlatformDetector.extractFirstUrl(pasted) ?: pasted)
                            focusManager.clearFocus()
                        }
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ModeSegmented(
                        mode = state.mode,
                        onModeChange = viewModel::selectMode,
                        modifier = Modifier.weight(1f),
                    )
                    if (preview != null) {
                        Text(
                            text = qualityLabel(state.quality),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            if (preview != null) {
                item {
                    QualityRow(
                        heights = preview.availableHeights,
                        selected = state.quality,
                        onSelect = viewModel::selectQuality,
                    )
                }
            }

            if (state.isAnalyzing) {
                item {
                    OmniCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.analyzing),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = state.platform.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            preview?.let { preview ->
                item {
                    PreviewCard(
                        title = preview.title,
                        uploader = preview.uploader,
                        thumbnail = preview.thumbnailUrl,
                        duration = preview.durationSeconds.toLong(),
                        fileSize = preview.fileSizeApprox,
                        heights = preview.availableHeights,
                        platform = preview.platform,
                        onDownload = viewModel::download,
                    )
                }
            }

            if (error != null) {
                item {
                    OmniCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.preview_error),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(onClick = viewModel::analyze)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    stringResource(R.string.retry_analysis),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SupportedSources()
            }

            if (tasks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.recent_downloads),
                        actionLabel = stringResource(R.string.view_all),
                        onAction = onOpenDownloads,
                    )
                }
                items(tasks.take(3), key = { it.id }) { task ->
                    RecentTaskRow(task)
                }
            }

            item {
                Text(
                    text = stringResource(R.string.privacy_note),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(activeCount: Int, onOpenDownloads: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenDownloads)
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (activeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    activeCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Omnibox(
    state: HomeUiState,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPaste: () -> Unit,
) {
    OmniCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp),
                )
                BasicTextField(
                    value = state.url,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onAnalyze() }),
                    decorationBox = { innerTextField ->
                        Box {
                            if (state.url.isBlank()) {
                                Text(
                                    text = stringResource(R.string.url_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.paste),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onPaste)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onAnalyze),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = stringResource(R.string.analyze),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (state.isValidUrl) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    PlatformBadge(state.platform.id, size = 20.dp)
                    Text(
                        text = state.platform.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = stringResource(R.string.detected),
                        style = MaterialTheme.typography.labelMedium,
                        color = OmniColors.Green,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = OmniColors.Green,
                        modifier = Modifier.size(15.dp),
                    )
                }
            } else if (state.url.isNotBlank()) {
                Text(
                    text = stringResource(R.string.invalid_url),
                    modifier = Modifier.padding(start = 10.dp, bottom = 9.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QualityRow(
    heights: List<Int>,
    selected: QualityPreset,
    onSelect: (QualityPreset) -> Unit,
) {
    val presets = remember(heights) {
        buildList {
            add(QualityPreset.BEST)
            heights.filter { it in 360..2160 }
                .distinct()
                .sortedDescending()
                .forEach { height ->
                    QualityPreset.entries.firstOrNull { it.maxHeight == height }?.let(::add)
                }
            if (size == 1) {
                addAll(listOf(QualityPreset.P1080, QualityPreset.P720, QualityPreset.P480, QualityPreset.P360))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.quality).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets, key = { it.id }) { preset ->
                val active = preset == selected
                Text(
                    text = qualityLabel(preset),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                        )
                        .border(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { onSelect(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    uploader: String,
    thumbnail: String?,
    duration: Long,
    fileSize: Long,
    heights: List<Int>,
    platform: String,
    onDownload: () -> Unit,
) {
    OmniCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            ) {
                if (!thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    if (duration > 0) {
                        PreviewChip(Formatters.duration(duration))
                    }
                    PreviewChip(PlatformDetector.infoForId(platform).label)
                    if (heights.isNotEmpty()) {
                        PreviewChip("${heights.max()}p")
                    }
                }
            }

            Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uploader.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = uploader,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.download),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (fileSize > 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "· ${Formatters.bytes(fileSize)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.68f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.56f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun SupportedSources() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(stringResource(R.string.supported_title))
        OmniCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.supported_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val platforms = listOf("youtube", "bilibili", "tiktok", "instagram", "twitter", "reddit", "vimeo")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(platforms, key = { it }) { id ->
                        val info = PlatformDetector.infoForId(id)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlatformBadge(id, size = 17.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = info.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.supported_more),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
