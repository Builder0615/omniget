package com.openselena.omniget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openselena.omniget.OmniGetApplication
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.download.DownloadService
import com.openselena.omniget.download.FileDelivery
import com.openselena.omniget.ui.components.BrandMark
import com.openselena.omniget.ui.components.OmniBackground
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.screens.DownloadsScreen
import com.openselena.omniget.ui.screens.HomeScreen
import com.openselena.omniget.ui.screens.SettingsScreen
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.ui.vm.HomeViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private enum class Destination(
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(Icons.Rounded.Home, R.string.nav_home),
    DOWNLOADS(Icons.Rounded.Download, R.string.nav_downloads),
    SETTINGS(Icons.Rounded.Settings, R.string.nav_settings),
}

@Composable
fun OmniGetApp(
    app: OmniGetApplication,
    sharedUrl: String?,
    openDownloads: Boolean,
    onSharedUrlConsumed: () -> Unit,
    onOpenDownloadsConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val tasks by app.downloads.tasks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    val activeCount = tasks.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            destination = Destination.HOME
            homeViewModel.acceptSharedText(sharedUrl)
            onSharedUrlConsumed()
        }
    }
    LaunchedEffect(openDownloads) {
        if (openDownloads) {
            destination = Destination.DOWNLOADS
            onOpenDownloadsConsumed()
        }
    }
    LaunchedEffect(Unit) {
        if (app.downloads.nextQueued() != null) {
            DownloadService.start(context)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 800.dp
        OmniBackground()
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                AppRail(
                    destination = destination,
                    activeCount = activeCount,
                    onDestination = { destination = it },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .padding(bottom = if (wide) 0.dp else 88.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (destination) {
                    Destination.HOME -> HomeScreen(
                        viewModel = homeViewModel,
                        tasks = tasks,
                        snackbarHostState = snackbarHostState,
                        onOpenDownloads = { destination = Destination.DOWNLOADS },
                        showBranding = !wide,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Destination.DOWNLOADS -> DownloadsScreen(
                        tasks = tasks,
                        onPause = { app.downloads.pause(it.id) },
                        onResume = { app.downloads.resume(it.id) },
                        onCancel = { app.downloads.cancel(it.id) },
                        onRetry = { app.downloads.retry(it.id) },
                        onOpen = { task -> openTask(context, task) },
                        onShare = { task -> shareTask(context, task) },
                        onRemove = { app.downloads.remove(it.id, deleteFile = false) },
                        onDeleteFile = { app.downloads.remove(it.id, deleteFile = true) },
                        onClearFinished = { app.downloads.clearFinished() },
                        onGoHome = { destination = Destination.HOME },
                        modifier = Modifier.fillMaxSize(),
                    )

                    Destination.SETTINGS -> SettingsScreen(
                        settingsRepository = app.settings,
                        downloads = app.downloads,
                        snackbarHostState = snackbarHostState,
                        showBranding = !wide,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (!wide) {
            BottomNavigation(
                destination = destination,
                activeCount = activeCount,
                onDestination = { destination = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (wide) 18.dp else 92.dp, start = 18.dp, end = 18.dp),
        )
    }
}

@Composable
private fun AppRail(
    destination: Destination,
    activeCount: Int,
    onDestination: (Destination) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(224.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.brand_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Destination.entries.forEach { item ->
                RailItem(
                    destination = item,
                    selected = destination == item,
                    badge = if (item == Destination.DOWNLOADS) activeCount else 0,
                    onClick = { onDestination(item) },
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.weight(1f))
            OmniCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LOCAL-FIRST",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "yt-dlp + FFmpeg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailItem(
    destination: Destination,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else Color.Transparent
    val foreground = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(destination.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(destination.labelRes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
        if (badge > 0) {
            Text(
                text = badge.toString(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun BottomNavigation(
    destination: Destination,
    activeCount: Int,
    onDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(5.dp)) {
            Destination.entries.forEach { item ->
                val selected = destination == item
                val foreground = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                            else Color.Transparent,
                        )
                        .clickable { onDestination(item) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(item.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(21.dp))
                        if (item == Destination.DOWNLOADS && activeCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(OmniColors.Orange),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = foreground,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun openTask(context: Context, task: DownloadTask) {
    val uri = FileDelivery.uriForTask(context, task) ?: return
    runCatching {
        context.startActivity(FileDelivery.openIntent(context, uri))
    }
}

private fun shareTask(context: Context, task: DownloadTask) {
    val uri = FileDelivery.uriForTask(context, task) ?: return
    runCatching {
        context.startActivity(
            Intent.createChooser(
                FileDelivery.shareIntent(context, uri, task.title),
                context.getString(R.string.share),
            ),
        )
    }
}
