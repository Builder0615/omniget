package com.openselena.omniget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openselena.omniget.OmniGetApplication
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadStatus
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.download.DownloadService
import com.openselena.omniget.download.FileDelivery
import com.openselena.omniget.ui.components.AppIcon
import com.openselena.omniget.ui.components.BrandMark
import com.openselena.omniget.ui.components.OmniBackground
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.PaperPressable
import com.openselena.omniget.ui.components.paperWell
import com.openselena.omniget.ui.components.paperRaised
import com.openselena.omniget.ui.components.pressMotion
import com.openselena.omniget.ui.components.SvgIcon
import com.openselena.omniget.ui.screens.DownloadsScreen
import com.openselena.omniget.ui.screens.HomeScreen
import com.openselena.omniget.ui.screens.SettingsScreen
import com.openselena.omniget.ui.theme.OmniColors
import com.openselena.omniget.ui.vm.HomeViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private enum class Destination(
    val icon: AppIcon,
    val labelRes: Int,
) {
    HOME(AppIcon.HOME, R.string.nav_home),
    DOWNLOADS(AppIcon.DOWNLOAD, R.string.nav_downloads),
    SETTINGS(AppIcon.SETTINGS, R.string.nav_settings),
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
    val isLandscape =
        LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
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

    val screenContent: @Composable (Destination, Modifier) -> Unit = { target, contentModifier ->
        when (target) {
            Destination.HOME -> HomeScreen(
                viewModel = homeViewModel,
                snackbarHostState = snackbarHostState,
                modifier = contentModifier,
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
                modifier = contentModifier,
            )

            Destination.SETTINGS -> SettingsScreen(
                settingsRepository = app.settings,
                downloads = app.downloads,
                snackbarHostState = snackbarHostState,
                modifier = contentModifier,
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        OmniBackground()
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                LandscapeNavigationRail(
                    destination = destination,
                    activeCount = activeCount,
                    onDestination = { destination = it },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 14.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 24.dp),
                ) {
                    DestinationContent(destination, screenContent)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                DestinationContent(destination, screenContent)
            }
            BottomNavigation(
                destination = destination,
                activeCount = activeCount,
                onDestination = { destination = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = if (isLandscape) 140.dp else 24.dp,
                    end = 24.dp,
                    bottom = if (isLandscape) 16.dp else 104.dp,
                ),
        )
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    content: @Composable (Destination, Modifier) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = destination,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 40 }) togetherWith fadeOut(tween(120))
        },
        label = "navigation",
    ) { target ->
        stateHolder.SaveableStateProvider(target.name) {
            content(target, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LandscapeNavigationRail(
    destination: Destination,
    activeCount: Int,
    onDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    OmniCard(
        modifier = modifier.widthIn(max = 88.dp),
        contentPadding = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(size = 36.dp)
            Spacer(Modifier.height(18.dp))
            val railOffset by animateDpAsState(
                targetValue = 66.dp * destination.ordinal,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                label = "rail-slider",
            )
            Box(Modifier.fillMaxWidth().height(66.dp * Destination.entries.size)) {
                Box(Modifier.offset(y = railOffset).fillMaxWidth().height(60.dp)
                    .paperRaised(RoundedCornerShape(17.dp), elevation = 3.dp))
                Column {
            Destination.entries.forEach { item ->
                val selected = destination == item
                val foreground = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                PaperPressable(
                    onClick = { onDestination(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { this.selected = selected },
                    shape = RoundedCornerShape(17.dp),
                    role = Role.Tab,
                ) {
                Column(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        SvgIcon(
                            item.icon,
                            contentDescription = null,
                            tint = foreground,
                            modifier = Modifier.size(20.dp),
                        )
                        if (item == Destination.DOWNLOADS && activeCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground,
                        maxLines = 1,
                    )
                }
                }
                Spacer(Modifier.height(6.dp))
            }
                }
            }
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
    OmniCard(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        contentPadding = 5.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val slotWidth = maxWidth / Destination.entries.size
            val sliderOffset by animateDpAsState(
                targetValue = slotWidth * destination.ordinal,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                label = "tab-slider",
            )
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset(x = sliderOffset).width(slotWidth).fillMaxHeight()
                    .paperRaised(RoundedCornerShape(17.dp), elevation = 3.dp))
            }
        Row {
            Destination.entries.forEach { item ->
                val selected = destination == item
                val foreground by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(220), label = "tab-foreground",
                )
                val iconScale by animateFloatAsState(if (selected) 1.04f else 1f, tween(220), label = "tab-icon")
                PaperPressable(
                    onClick = { onDestination(item) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { this.selected = selected },
                    shape = RoundedCornerShape(17.dp),
                    role = Role.Tab,
                ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        SvgIcon(item.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(21.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                        if (item == Destination.DOWNLOADS && activeCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary),
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
