package com.openselena.omniget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.download.MediaThumbnails
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.util.Formatters
import com.openselena.omniget.util.PlatformDetector
import kotlinx.coroutines.delay

@Composable
fun OmniBackground(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val dark = background.luminance() < 0.5f
    val fiber = if (dark) Color.White.copy(alpha = 0.025f) else Color(0xFF6E5537).copy(alpha = 0.035f)
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .drawWithCache {
                val fibers = Path()
                val spacing = 13.dp.toPx()
                val columns = (size.width / spacing).toInt() + 1
                val rows = (size.height / spacing).toInt() + 1
                repeat(rows) { rowIndex ->
                    repeat(columns) { columnIndex ->
                        val seed = rowIndex * 37 + columnIndex * 71
                        val left = columnIndex * spacing + (seed % 11) / 11f * spacing
                        val top = rowIndex * spacing + (seed % 7) / 7f * spacing
                        fibers.moveTo(left, top)
                        fibers.lineTo(left + 0.8.dp.toPx(), top + 0.25.dp.toPx())
                    }
                }
                onDrawBehind {
                    drawPath(fibers, fiber, style = androidx.compose.ui.graphics.drawscope.Stroke(0.6.dp.toPx()))
                }
            },
    )
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(Color(0xFFF9E9DA)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.xiaohei_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.96f),
        )
    }
}

@Composable
fun OmniCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.paperRaised(shape = RoundedCornerShape(18.dp), color = color),
        shape = RoundedCornerShape(18.dp),
        color = color,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onAction != null) {
            PaperButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun PlatformBadge(platformId: String, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val platform = PlatformDetector.infoForId(platformId)
    val color = Color(platform.color)
    val icon = when (platformId) {
        "youtube" -> AppIcon.YOUTUBE
        "bilibili" -> AppIcon.BILIBILI
        "tiktok" -> AppIcon.TIKTOK
        "instagram" -> AppIcon.INSTAGRAM
        "twitter" -> AppIcon.X
        "reddit" -> AppIcon.REDDIT
        "vimeo" -> AppIcon.VIMEO
        else -> AppIcon.LINK
    }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(
            icon = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

@Composable
fun ModeSegmented(
    mode: DownloadMode,
    onModeChange: (DownloadMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.height(48.dp).paperWell(RoundedCornerShape(14.dp))) {
        val slotWidth = (maxWidth - 8.dp) / 2
        val sliderOffset by animateDpAsState(
            targetValue = if (mode == DownloadMode.VIDEO) 0.dp else slotWidth,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 450f),
            label = "mode-slider",
        )
        Box(
            Modifier.offset(x = sliderOffset + 4.dp, y = 4.dp)
                .width(slotWidth).height(40.dp)
                .paperRaised(RoundedCornerShape(10.dp), elevation = 2.dp),
        )
        Row(Modifier.fillMaxSize().padding(4.dp).selectableGroup()) {
            Segment(
                label = stringResource(R.string.video),
                icon = AppIcon.MOVIE,
                active = mode == DownloadMode.VIDEO,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onModeChange(DownloadMode.VIDEO) },
            )
            Segment(
                label = stringResource(R.string.audio),
                icon = AppIcon.MUSIC,
                active = mode == DownloadMode.AUDIO,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onModeChange(DownloadMode.AUDIO) },
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    icon: AppIcon,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val foreground by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "mode-ink-color",
    )
    PaperPressable(
        onClick = onClick,
        modifier = modifier.semantics { selected = active },
        shape = RoundedCornerShape(9.dp),
        role = Role.Tab,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SvgIcon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = foreground, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun StatusPill(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        DownloadStatus.QUEUED -> stringResource(R.string.status_queued) to OmniColors.Yellow
        DownloadStatus.DOWNLOADING -> stringResource(R.string.status_downloading) to OmniColors.Blue
        DownloadStatus.PAUSED -> stringResource(R.string.status_paused) to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.COMPLETED -> stringResource(R.string.status_completed) to OmniColors.Green
        DownloadStatus.FAILED -> stringResource(R.string.status_failed) to OmniColors.Red
    }
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    modifier: Modifier = Modifier,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onOpen: () -> Unit = {},
    onShare: () -> Unit = {},
    onRemove: () -> Unit = {},
    onDeleteFile: () -> Unit = {},
) {
    var confirmDelete by remember(task.id) { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(2600)
            confirmDelete = false
        }
    }
    val platform = PlatformDetector.infoForId(task.platform)
    val accent = Color(platform.color)

    OmniCard(modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                TaskPoster(task, accent)
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = task.title.ifBlank { task.url },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusPill(task.status)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PlatformBadge(task.platform, size = 18.dp)
                        Text(
                            text = platform.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (task.mode == DownloadMode.AUDIO) {
                                stringResource(R.string.audio)
                            } else {
                                qualityLabel(task.quality)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (task.totalBytes > 0L) {
                            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.bytes(task.totalBytes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (task.status == DownloadStatus.DOWNLOADING && task.statusLine.isNotBlank()) {
                        Text(
                            text = task.statusLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when (task.status) {
                DownloadStatus.DOWNLOADING -> {
                    val preparing = task.progress <= 0f && task.statusLine.isBlank()
                    ProgressTrack(task.progress, OmniColors.Blue, indeterminate = preparing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (preparing) {
                                stringResource(R.string.download_progress_preparing)
                            } else {
                                buildString {
                                    append("${task.progress.toInt()}%")
                                    if (task.totalBytes > 0L) {
                                        append("  ·  ${Formatters.bytes(task.downloadedBytes)} / ${Formatters.bytes(task.totalBytes)}")
                                    }
                                    if (task.speedBps > 0) append("  ·  ${Formatters.speed(task.speedBps)}")
                                    if (task.etaSeconds >= 0) append("  ·  ${Formatters.eta(task.etaSeconds)}")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        TaskAction(AppIcon.PAUSE, stringResource(R.string.pause), onPause)
                        TaskAction(AppIcon.CLOSE, stringResource(R.string.cancel), onCancel)
                    }
                }

                DownloadStatus.PAUSED -> {
                    ProgressTrack(task.progress, MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${task.progress.toInt()}%" +
                                if (task.totalBytes > 0L) {
                                    "  ·  ${Formatters.bytes(task.downloadedBytes)} / ${Formatters.bytes(task.totalBytes)}"
                                } else {
                                    ""
                                },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TaskAction(AppIcon.PLAY, stringResource(R.string.resume), onResume, accent = true)
                        TaskAction(AppIcon.CLOSE, stringResource(R.string.cancel), onCancel)
                    }
                }

                DownloadStatus.QUEUED -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TaskAction(AppIcon.CLOSE, stringResource(R.string.cancel), onCancel)
                    }
                }

                DownloadStatus.FAILED -> {
                    if (!task.error.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SvgIcon(
                                AppIcon.ERROR,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = task.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TaskAction(AppIcon.REFRESH, stringResource(R.string.retry), onRetry, accent = true)
                        TaskAction(AppIcon.CLOSE, stringResource(R.string.remove), onRemove)
                    }
                }

                DownloadStatus.COMPLETED -> {
                    ProgressTrack(100f, OmniColors.Green)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "100%" + if (task.totalBytes > 0L) {
                                "  ·  ${Formatters.bytes(task.totalBytes)}"
                            } else {
                                ""
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = OmniColors.Green,
                        )
                        TaskAction(AppIcon.OPEN_EXTERNAL, stringResource(R.string.open), onOpen)
                        TaskAction(AppIcon.SHARE, stringResource(R.string.share), onShare)
                        TaskAction(
                            AppIcon.DELETE,
                            stringResource(if (confirmDelete) R.string.confirm_delete else R.string.delete_file),
                            onClick = {
                                if (confirmDelete) {
                                    confirmDelete = false
                                    onDeleteFile()
                                } else {
                                    confirmDelete = true
                                }
                            },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTaskRow(task: DownloadTask, modifier: Modifier = Modifier) {
    val platform = PlatformDetector.infoForId(task.platform)
    val accent = Color(platform.color)
    OmniCard(modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaskPoster(task, accent, width = 54.dp, height = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    task.title.ifBlank { task.url },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    platform.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (task.status == DownloadStatus.DOWNLOADING) {
                CircularProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.size(24.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.outline,
                    strokeWidth = 2.5.dp,
                )
            } else {
                StatusPill(task.status)
            }
        }
    }
}

@Composable
private fun TaskPoster(
    task: DownloadTask,
    accent: Color,
    width: Dp = 88.dp,
    height: Dp = 64.dp,
) {
    val context = LocalContext.current
    val cover by produceState(
        initialValue = task.thumbnailUrl,
        task.id,
        task.thumbnailUrl,
        task.fileUri,
        task.filePath,
    ) {
        if (task.thumbnailUrl.isNullOrBlank() && task.mode == DownloadMode.VIDEO) {
            value = MediaThumbnails.fromTask(context, task)
        }
    }
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.08f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!cover.isNullOrBlank()) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SvgIcon(
                icon = if (task.mode == DownloadMode.AUDIO) AppIcon.MUSIC else AppIcon.MOVIE,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                    ),
                ),
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float, color: Color, indeterminate: Boolean = false) {
    if (indeterminate) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline,
        )
        return
    }
    val animated by animateFloatAsState(
        targetValue = (progress / 100f).coerceIn(0f, 1f),
        label = "download-progress",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun TaskAction(
    icon: AppIcon,
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    tint: Color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
) {
    PaperIconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
    ) {
        SvgIcon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun qualityLabel(quality: QualityPreset): String =
    if (quality == QualityPreset.BEST) {
        stringResource(R.string.quality_best)
    } else {
        "${quality.id}p"
    }

@Composable
fun TinyArrow(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(modifier.size(14.dp)) {
        SvgIcon(
            AppIcon.OPEN_EXTERNAL,
            contentDescription = null,
            tint = color,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
