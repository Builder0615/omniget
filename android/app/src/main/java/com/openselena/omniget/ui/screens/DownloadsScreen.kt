package com.openselena.omniget.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.download.MediaThumbnails
import com.openselena.omniget.ui.components.AppIcon
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.PaperButton
import com.openselena.omniget.ui.components.PaperIconButton
import com.openselena.omniget.ui.components.PaperPressable
import com.openselena.omniget.ui.components.PetMascot
import com.openselena.omniget.ui.components.PetState
import com.openselena.omniget.ui.components.SvgIcon
import com.openselena.omniget.ui.components.paperWell
import com.openselena.omniget.ui.components.paperRaised
import com.openselena.omniget.ui.components.qualityLabel
import com.openselena.omniget.util.Formatters
import kotlinx.coroutines.launch

private enum class DownloadFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    ACTIVE(R.string.filter_active),
    COMPLETED(R.string.filter_completed),
    FAILED(R.string.filter_failed),
}

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    onPause: (DownloadTask) -> Unit,
    onResume: (DownloadTask) -> Unit,
    onCancel: (DownloadTask) -> Unit,
    onRetry: (DownloadTask) -> Unit,
    onOpen: (DownloadTask) -> Unit,
    onShare: (DownloadTask) -> Unit,
    onRemove: (DownloadTask) -> Unit,
    onDeleteFile: (DownloadTask) -> Unit,
    onClearFinished: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(DownloadFilter.ALL) }
    var confirmClearFinished by rememberSaveable { mutableStateOf(false) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val filtered = remember(tasks, filter) {
        tasks.filter { task ->
            when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.ACTIVE -> task.status in listOf(
                    DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED,
                )
                DownloadFilter.COMPLETED -> task.status == DownloadStatus.COMPLETED
                DownloadFilter.FAILED -> task.status == DownloadStatus.FAILED
            }
        }.sortedByDescending { it.createdAt }
    }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val deleteTask = tasks.firstOrNull { it.id == deleteTaskId }

    Box(
        modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = if (landscape) 1120.dp else 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(vertical = if (landscape) 14.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.downloads_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (tasks.any { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }) {
                        DownloadIconAction(
                            AppIcon.DELETE_SWEEP,
                            stringResource(R.string.clear_finished),
                            onClick = { confirmClearFinished = true },
                        )
                    }
                }
            }
            item(key = "filters") {
                DownloadFilterStrip(selected = filter, onSelect = { filter = it })
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    EmptyDownloads(
                        onGoHome,
                        labelRes = if (tasks.isEmpty()) R.string.downloads_empty else R.string.downloads_no_match,
                        modifier = Modifier.animateItem().fillMaxWidth(),
                    )
                }
            } else if (landscape) {
                items(filtered.chunked(2), key = { row -> row.joinToString(":") { it.id } }) { row ->
                    Row(
                        Modifier.fillMaxWidth().animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEach { task ->
                            DownloadFileRow(
                                task = task,
                                onPrimary = {
                                    when (task.status) {
                                        DownloadStatus.DOWNLOADING -> onPause(task)
                                        DownloadStatus.PAUSED -> onResume(task)
                                        DownloadStatus.FAILED -> onRetry(task)
                                        DownloadStatus.COMPLETED -> onOpen(task)
                                        DownloadStatus.QUEUED -> Unit
                                    }
                                },
                                onActions = { selectedTaskId = task.id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(filtered, key = { it.id }) { task ->
                    DownloadFileRow(
                        task = task,
                        onPrimary = {
                            when (task.status) {
                                DownloadStatus.DOWNLOADING -> onPause(task)
                                DownloadStatus.PAUSED -> onResume(task)
                                DownloadStatus.FAILED -> onRetry(task)
                                DownloadStatus.COMPLETED -> onOpen(task)
                                DownloadStatus.QUEUED -> Unit
                            }
                        },
                        onActions = { selectedTaskId = task.id },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(key = "bottom") { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (confirmClearFinished) {
        AlertDialog(
            onDismissRequest = { confirmClearFinished = false },
            title = { Text(stringResource(R.string.clear_downloads_title)) },
            text = { Text(stringResource(R.string.clear_downloads_message)) },
            confirmButton = {
                PaperButton(onClick = {
                    confirmClearFinished = false
                    onClearFinished()
                }) {
                    Text(stringResource(R.string.clear_downloads_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PaperButton(onClick = { confirmClearFinished = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    selectedTask?.let { task ->
        DownloadActionsSheet(
            task = task,
            onDismiss = { selectedTaskId = null },
            onPause = { onPause(task) },
            onResume = { onResume(task) },
            onCancel = { onCancel(task) },
            onRetry = { onRetry(task) },
            onOpen = { onOpen(task) },
            onShare = { onShare(task) },
            onRemove = { onRemove(task) },
            onDeleteFile = { deleteTaskId = task.id },
        )
    }
    deleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTaskId = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.delete_confirm_message))
                }
            },
            confirmButton = {
                PaperButton(onClick = {
                    deleteTaskId = null
                    onDeleteFile(task)
                }) {
                    Text(stringResource(R.string.delete_file), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                PaperButton(onClick = { deleteTaskId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DownloadFilterStrip(selected: DownloadFilter, onSelect: (DownloadFilter) -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(50.dp).paperWell(RoundedCornerShape(16.dp)),
    ) {
        val inset = 5.dp
        val tabWidth = (maxWidth - inset * 2) / DownloadFilter.entries.size
        val indicatorOffset by animateDpAsState(
            targetValue = inset + tabWidth * selected.ordinal,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
            label = "download-filter-indicator",
        )
        Box(
            Modifier.offset(x = indicatorOffset, y = inset).width(tabWidth).height(maxHeight - inset * 2)
                .paperRaised(shape = RoundedCornerShape(12.dp), elevation = 2.dp),
        )
        Row(Modifier.fillMaxSize().padding(inset).selectableGroup()) {
            DownloadFilter.entries.forEach { filter ->
                DownloadFilterTab(
                    filter,
                    selected = filter == selected,
                    onClick = { onSelect(filter) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DownloadFilterTab(
    filter: DownloadFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foreground by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "download-filter-ink",
    )
    PaperPressable(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp),
        role = Role.Tab,
    ) {
        Text(
            stringResource(filter.labelRes),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DownloadFileRow(
    task: DownloadTask,
    onPrimary: () -> Unit,
    onActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = when (task.status) {
        DownloadStatus.QUEUED -> stringResource(R.string.status_queued)
        DownloadStatus.DOWNLOADING -> stringResource(R.string.status_downloading)
        DownloadStatus.PAUSED -> stringResource(R.string.status_paused)
        DownloadStatus.COMPLETED -> stringResource(R.string.status_completed)
        DownloadStatus.FAILED -> stringResource(R.string.status_failed)
    }
    val quality = if (task.mode == DownloadMode.AUDIO) stringResource(R.string.audio) else qualityLabel(task.quality)
    val active = task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED
    val progress by animateFloatAsState((task.progress / 100f).coerceIn(0f, 1f), label = "download-file-progress")
    val statusColor = if (task.status == DownloadStatus.FAILED) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    OmniCard(modifier.fillMaxWidth().animateContentSize(), contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DownloadPoster(task)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        task.title.ifBlank { task.url },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append(status)
                            if (!active) {
                                append(" · $quality")
                                if (task.totalBytes > 0) append(" · ${Formatters.bytes(task.totalBytes)}")
                            } else {
                                append(" · ${task.progress.toInt()}%")
                                if (task.speedBps > 0) append(" · ${Formatters.speed(task.speedBps)}")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (task.status != DownloadStatus.QUEUED) {
                    val (icon, label) = when (task.status) {
                        DownloadStatus.DOWNLOADING -> AppIcon.PAUSE to stringResource(R.string.pause)
                        DownloadStatus.PAUSED -> AppIcon.PLAY to stringResource(R.string.resume)
                        DownloadStatus.COMPLETED -> AppIcon.PLAY to stringResource(R.string.open)
                        else -> AppIcon.REFRESH to stringResource(R.string.retry)
                    }
                    DownloadIconAction(icon, label, onPrimary, tint = MaterialTheme.colorScheme.primary)
                }
                DownloadIconAction(AppIcon.MORE, stringResource(R.string.task_actions), onActions)
            }
            if (active) {
                if (task.status == DownloadStatus.DOWNLOADING && task.progress <= 0f && task.statusLine.isBlank()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadPoster(task: DownloadTask) {
    val context = LocalContext.current
    var coverFailed by remember(task.id, task.thumbnailUrl) { mutableStateOf(false) }
    val cover by produceState(task.thumbnailUrl, task.id, task.thumbnailUrl, task.fileUri, task.filePath, coverFailed) {
        if ((task.thumbnailUrl.isNullOrBlank() || coverFailed) && task.mode == DownloadMode.VIDEO) {
            value = MediaThumbnails.fromTask(context, task)
        }
    }
    Box(
        Modifier.width(70.dp).height(60.dp).clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!cover.isNullOrBlank()) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                onError = { coverFailed = true },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (task.mode == DownloadMode.AUDIO) {
            SvgIcon(AppIcon.MUSIC, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DownloadIconAction(
    icon: AppIcon,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    PaperIconButton(
        onClick = onClick,
        modifier = Modifier.padding(start = 6.dp),
    ) {
        SvgIcon(icon, label, Modifier.size(19.dp), tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadActionsSheet(
    task: DownloadTask,
    onDismiss: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    onDeleteFile: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    val scope = rememberCoroutineScope()
    var handling by remember(task.id) { mutableStateOf(false) }
    val perform: (() -> Unit) -> Unit = { action ->
        if (!handling) {
            handling = true
            scope.launch {
                sheetState.hide()
                onDismiss()
                action()
            }
        }
    }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        ) {
            Text(
                task.title.ifBlank { task.url },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (task.status == DownloadStatus.FAILED && !task.error.isNullOrBlank()) {
                Text(
                    task.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            when (task.status) {
                DownloadStatus.DOWNLOADING -> {
                    DownloadMenuAction(AppIcon.PAUSE, R.string.pause, !handling) { perform(onPause) }
                    DownloadMenuAction(AppIcon.CLOSE, R.string.cancel, !handling) { perform(onCancel) }
                }
                DownloadStatus.PAUSED -> {
                    DownloadMenuAction(AppIcon.PLAY, R.string.resume, !handling) { perform(onResume) }
                    DownloadMenuAction(AppIcon.CLOSE, R.string.cancel, !handling) { perform(onCancel) }
                }
                DownloadStatus.QUEUED -> DownloadMenuAction(AppIcon.CLOSE, R.string.cancel, !handling) { perform(onCancel) }
                DownloadStatus.FAILED -> {
                    DownloadMenuAction(AppIcon.REFRESH, R.string.retry, !handling) { perform(onRetry) }
                    DownloadMenuAction(AppIcon.DELETE, R.string.remove, !handling) { perform(onRemove) }
                }
                DownloadStatus.COMPLETED -> {
                    DownloadMenuAction(AppIcon.OPEN_EXTERNAL, R.string.open, !handling) { perform(onOpen) }
                    DownloadMenuAction(AppIcon.SHARE, R.string.share, !handling) { perform(onShare) }
                    DownloadMenuAction(AppIcon.CLOSE, R.string.remove, !handling) { perform(onRemove) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    DownloadMenuAction(AppIcon.DELETE, R.string.delete_file, !handling, danger = true) { perform(onDeleteFile) }
                }
            }
        }
    }
}

@Composable
private fun DownloadMenuAction(
    icon: AppIcon,
    labelRes: Int,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    PaperButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
    ) {
        SvgIcon(icon, null, Modifier.size(20.dp), color)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(labelRes), color = color, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun EmptyDownloads(onGoHome: () -> Unit, labelRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PetMascot(PetState.IDLE, Modifier.size(128.dp))
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PaperButton(onClick = onGoHome) { Text(stringResource(R.string.go_home)) }
    }
}
