package com.openselena.omniget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.util.Formatters
import com.openselena.omniget.util.PlatformDetector
import kotlinx.coroutines.delay

@Composable
fun OmniBackground(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f)
    Box(
        modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.width * 0.8f,
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.18f, 0f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow, Color.Transparent),
                        center = Offset(size.width, size.height * 0.25f),
                        radius = size.width * 0.65f,
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width, size.height * 0.25f),
                )
            },
    )
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(
                Brush.linearGradient(
                    listOf(OmniColors.OrangeBright, OmniColors.Orange),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = null,
            tint = OmniColors.OnOrange,
            modifier = Modifier.size(size * 0.62f),
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
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
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
            Text(
                text = actionLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun PlatformBadge(platformId: String, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val platform = PlatformDetector.infoForId(platformId)
    val color = Color(platform.color)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.label.take(1).uppercase(),
            color = color,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ModeSegmented(
    mode: DownloadMode,
    onModeChange: (DownloadMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(3.dp)) {
            Segment(
                label = stringResource(R.string.video),
                icon = Icons.Rounded.Movie,
                active = mode == DownloadMode.VIDEO,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(DownloadMode.VIDEO) },
            )
            Segment(
                label = stringResource(R.string.audio),
                icon = Icons.Rounded.MusicNote,
                active = mode == DownloadMode.AUDIO,
                modifier = Modifier.weight(1f),
                onClick = { onModeChange(DownloadMode.AUDIO) },
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    val foreground = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .background(background)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = foreground, style = MaterialTheme.typography.labelLarge)
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
                    ProgressTrack(task.progress, OmniColors.Blue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = buildString {
                                append("${task.progress.toInt()}%")
                                if (task.speedBps > 0) append("  ·  ${Formatters.speed(task.speedBps)}")
                                if (task.etaSeconds >= 0) append("  ·  ${Formatters.eta(task.etaSeconds)}")
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        TaskAction(Icons.Rounded.Pause, stringResource(R.string.pause), onPause)
                        TaskAction(Icons.Rounded.Close, stringResource(R.string.cancel), onCancel)
                    }
                }

                DownloadStatus.PAUSED -> {
                    ProgressTrack(task.progress, MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TaskAction(Icons.Rounded.PlayArrow, stringResource(R.string.resume), onResume, accent = true)
                        TaskAction(Icons.Rounded.Close, stringResource(R.string.cancel), onCancel)
                    }
                }

                DownloadStatus.QUEUED -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TaskAction(Icons.Rounded.Close, stringResource(R.string.cancel), onCancel)
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
                            Icon(
                                Icons.Rounded.ErrorOutline,
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
                        TaskAction(Icons.Rounded.Refresh, stringResource(R.string.retry), onRetry, accent = true)
                        TaskAction(Icons.Rounded.Close, stringResource(R.string.remove), onRemove)
                    }
                }

                DownloadStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TaskAction(Icons.Rounded.OpenInNew, stringResource(R.string.open), onOpen)
                        TaskAction(Icons.Rounded.Share, stringResource(R.string.share), onShare)
                        TaskAction(
                            Icons.Rounded.Delete,
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
        if (!task.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = task.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = if (task.mode == DownloadMode.AUDIO) Icons.Rounded.MusicNote else Icons.Rounded.Movie,
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
private fun ProgressTrack(progress: Float, color: Color) {
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
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    tint: Color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(
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
        Icon(
            Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint = color,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
