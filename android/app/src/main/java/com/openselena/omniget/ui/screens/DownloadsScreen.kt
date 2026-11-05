package com.openselena.omniget.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.ui.components.DownloadTaskCard
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.util.Formatters

private enum class DownloadFilter {
    ALL,
    ACTIVE,
    QUEUED,
    COMPLETED,
    FAILED
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
    val filtered = remember(tasks, filter) {
        when (filter) {
            DownloadFilter.ALL -> tasks
            DownloadFilter.ACTIVE -> tasks.filter {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED
            }
            DownloadFilter.QUEUED -> tasks.filter { it.status == DownloadStatus.QUEUED }
            DownloadFilter.COMPLETED -> tasks.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> tasks.filter { it.status == DownloadStatus.FAILED }
        }
    }
    val counts = remember(tasks) {
        mapOf(
            DownloadFilter.ALL to tasks.size,
            DownloadFilter.ACTIVE to tasks.count {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED
            },
            DownloadFilter.QUEUED to tasks.count { it.status == DownloadStatus.QUEUED },
            DownloadFilter.COMPLETED to tasks.count { it.status == DownloadStatus.COMPLETED },
            DownloadFilter.FAILED to tasks.count { it.status == DownloadStatus.FAILED },
        )
    }
    val completed = tasks.filter { it.status == DownloadStatus.COMPLETED }
    val totalBytes = completed.sumOf { it.totalBytes }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 900.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.downloads_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.downloads_summary,
                            completed.size,
                            completed.size,
                            Formatters.bytes(totalBytes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (completed.isNotEmpty() || tasks.any { it.status == DownloadStatus.FAILED }) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onClearFinished)
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.clear_finished),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(DownloadFilter.entries, key = { it.name }) { entry ->
                    val active = entry == filter
                    val label = when (entry) {
                        DownloadFilter.ALL -> stringResource(R.string.filter_all)
                        DownloadFilter.ACTIVE -> stringResource(R.string.filter_active)
                        DownloadFilter.QUEUED -> stringResource(R.string.filter_queued)
                        DownloadFilter.COMPLETED -> stringResource(R.string.filter_completed)
                        DownloadFilter.FAILED -> stringResource(R.string.filter_failed)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            )
                            .border(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { filter = entry }
                            .padding(start = 12.dp, end = 9.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = counts[entry].toString(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyDownloads(onGoHome)
            }
        } else {
            items(filtered, key = { it.id }) { task ->
                DownloadTaskCard(
                    task = task,
                    onPause = { onPause(task) },
                    onResume = { onResume(task) },
                    onCancel = { onCancel(task) },
                    onRetry = { onRetry(task) },
                    onOpen = { onOpen(task) },
                    onShare = { onShare(task) },
                    onRemove = { onRemove(task) },
                    onDeleteFile = { onDeleteFile(task) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun EmptyDownloads(onGoHome: () -> Unit) {
    OmniCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentPadding = 24.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.mascot_idle),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
            Text(
                text = stringResource(R.string.downloads_empty),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.downloads_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onGoHome,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.go_home))
            }
        }
    }
}
