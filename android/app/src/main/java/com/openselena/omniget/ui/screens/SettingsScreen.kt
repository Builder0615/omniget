package com.openselena.omniget.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openselena.omniget.BuildConfig
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadRepository
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.data.SettingsRepository
import com.openselena.omniget.data.ThemeMode
import com.openselena.omniget.download.DownloadEngine
import com.openselena.omniget.ui.components.AppIcon
import com.openselena.omniget.ui.components.BrandMark
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.PaperButton
import com.openselena.omniget.ui.components.PaperIconButton
import com.openselena.omniget.ui.components.PaperPressable
import com.openselena.omniget.ui.components.SvgIcon
import com.openselena.omniget.ui.components.paperRaised
import com.openselena.omniget.ui.components.paperWell
import com.openselena.omniget.ui.components.qualityLabel
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPanel { THEME, MODE, QUALITY, STORAGE, COOKIES, ENGINE, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    downloads: DownloadRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var panel by rememberSaveable { mutableStateOf<SettingsPanel?>(null) }
    var engineVersion by remember { mutableStateOf(DownloadEngine.version(context)) }
    var engineMessage by remember { mutableStateOf("") }
    var updatingEngine by remember { mutableStateOf(false) }
    var importingCookies by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cookieImported = stringResource(R.string.cookie_imported)
    val cookieInvalid = stringResource(R.string.cookie_invalid)
    val engineUpdated = stringResource(R.string.engine_updated)
    val engineBusy = stringResource(R.string.engine_busy)
    val engineFailurePrefix = stringResource(R.string.engine_update_failed, "")
    val dismiss: () -> Unit = { scope.launch { sheetState.hide(); panel = null } }

    val cookiePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importingCookies = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    importCookieFile(context, context.contentResolver, uri)
                }
                importingCookies = false
                if (result != null) settingsRepository.setCookiePath(result.absolutePath)
                snackbarHostState.showSnackbar(if (result != null) cookieImported else cookieInvalid)
            }
        }
    }
    val updateEngine: () -> Unit = {
        if (!updatingEngine) {
            if (downloads.hasActiveDownloads()) {
                engineMessage = engineBusy
                scope.launch { snackbarHostState.showSnackbar(engineBusy) }
            } else {
                updatingEngine = true
                engineMessage = ""
                scope.launch {
                    val message = try {
                        DownloadEngine.update(context)
                        engineVersion = DownloadEngine.version(context)
                        engineUpdated
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        engineFailurePrefix + error.message.orEmpty().take(180)
                    } finally {
                        updatingEngine = false
                    }
                    engineMessage = message
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }
    val appearance: @Composable () -> Unit = {
        SettingsGroup(stringResource(R.string.settings_appearance)) {
            SettingsRow(
                AppIcon.PALETTE,
                stringResource(R.string.settings_theme),
                themeLabel(settings.theme),
                onClick = { panel = SettingsPanel.THEME },
            )
        }
    }
    val downloadOptions: @Composable () -> Unit = {
        SettingsGroup(stringResource(R.string.settings_downloads)) {
            SettingsRow(
                AppIcon.MOVIE,
                stringResource(R.string.default_mode),
                stringResource(if (settings.defaultMode == DownloadMode.VIDEO) R.string.video else R.string.audio),
                onClick = { panel = SettingsPanel.MODE },
            )
            SettingsCrease()
            SettingsRow(
                AppIcon.SLIDERS,
                stringResource(R.string.default_quality),
                qualityLabel(settings.defaultQuality),
                onClick = { panel = SettingsPanel.QUALITY },
            )
            SettingsCrease()
            SettingsToggle(
                AppIcon.SPARKLES,
                stringResource(R.string.write_subtitles),
                settings.writeSubtitles,
            ) { value -> settingsRepository.update { it.copy(writeSubtitles = value) } }
            SettingsCrease()
            SettingsToggle(
                AppIcon.SYNC,
                stringResource(R.string.allow_playlist),
                settings.allowPlaylist,
            ) { value -> settingsRepository.update { it.copy(allowPlaylist = value) } }
        }
    }
    val tools: @Composable () -> Unit = {
        SettingsGroup(stringResource(R.string.settings_tools)) {
            SettingsRow(
                AppIcon.FOLDER,
                stringResource(R.string.settings_storage),
                onClick = { panel = SettingsPanel.STORAGE },
            )
            SettingsCrease()
            SettingsRow(
                AppIcon.COOKIE,
                stringResource(R.string.settings_cookie_label),
                stringResource(if (settings.cookiePath == null) R.string.settings_cookie_empty else R.string.settings_cookie_ready),
                onClick = { panel = SettingsPanel.COOKIES },
                busy = importingCookies,
            )
            SettingsCrease()
            SettingsRow(
                AppIcon.REFRESH,
                stringResource(R.string.settings_engine),
                engineVersion,
                onClick = { panel = SettingsPanel.ENGINE },
                busy = updatingEngine,
            )
        }
    }
    val about: @Composable () -> Unit = {
        OmniCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
            SettingsRow(
                null,
                stringResource(R.string.app_name),
                BuildConfig.VERSION_NAME,
                onClick = { panel = SettingsPanel.ABOUT },
                leading = { BrandMark(size = 32.dp) },
            )
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = if (landscape) 1120.dp else 640.dp).fillMaxSize()
                .padding(horizontal = 24.dp, vertical = if (landscape) 14.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            if (landscape) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) { appearance(); downloadOptions() }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) { tools(); about() }
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) { appearance(); downloadOptions(); tools(); about() }
            }
        }
    }

    panel?.let { currentPanel ->
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.8f).dp)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            when (currentPanel) {
                                SettingsPanel.THEME -> R.string.settings_theme
                                SettingsPanel.MODE -> R.string.default_mode
                                SettingsPanel.QUALITY -> R.string.default_quality
                                SettingsPanel.STORAGE -> R.string.settings_storage
                                SettingsPanel.COOKIES -> R.string.settings_cookies
                                SettingsPanel.ENGINE -> R.string.settings_engine
                                SettingsPanel.ABOUT -> R.string.settings_about
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    PaperIconButton(onClick = dismiss) {
                        SvgIcon(AppIcon.CLOSE, stringResource(R.string.cancel), Modifier.size(18.dp))
                    }
                }
                when (currentPanel) {
                    SettingsPanel.THEME -> SettingsChoices(
                        ThemeMode.entries.map { it to themeLabel(it) },
                        settings.theme,
                    ) { value -> settingsRepository.update { it.copy(theme = value) }; dismiss() }
                    SettingsPanel.MODE -> SettingsChoices(
                        listOf(DownloadMode.VIDEO to stringResource(R.string.video), DownloadMode.AUDIO to stringResource(R.string.audio)),
                        settings.defaultMode,
                    ) { value -> settingsRepository.update { it.copy(defaultMode = value) }; dismiss() }
                    SettingsPanel.QUALITY -> SettingsChoices(
                        QualityPreset.entries.map { it to qualityLabel(it) },
                        settings.defaultQuality,
                    ) { value -> settingsRepository.update { it.copy(defaultQuality = value) }; dismiss() }
                    SettingsPanel.STORAGE -> {
                        Text("Downloads/OmniGet", style = MaterialTheme.typography.titleMedium)
                        SettingsDetail(stringResource(R.string.storage_path))
                        SettingsDetail(stringResource(R.string.storage_internal))
                    }
                    SettingsPanel.COOKIES -> {
                        SettingsDetail(stringResource(R.string.cookie_help))
                        PaperButton(
                            onClick = { cookiePicker.launch(arrayOf("text/plain", "application/octet-stream")) },
                            enabled = !importingCookies,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            accent = true,
                        ) {
                            AnimatedContent(importingCookies, label = "cookie-import") { importing ->
                                if (importing) CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                                else Text(stringResource(R.string.cookie_import))
                            }
                        }
                        AnimatedVisibility(settings.cookiePath != null) {
                            PaperButton(
                                onClick = { settingsRepository.setCookiePath(null) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) {
                                SvgIcon(AppIcon.DELETE, null, Modifier.size(18.dp), MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.cookie_clear), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    SettingsPanel.ENGINE -> {
                        Text(stringResource(R.string.engine_version, engineVersion), style = MaterialTheme.typography.titleMedium)
                        SettingsDetail("yt-dlp · FFmpeg · QuickJS")
                        AnimatedVisibility(engineMessage.isNotEmpty()) {
                            SettingsDetail(engineMessage)
                        }
                        PaperButton(
                            onClick = updateEngine,
                            enabled = !updatingEngine,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            accent = true,
                        ) {
                            AnimatedContent(updatingEngine, label = "engine-update") { updating ->
                                if (updating) CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                                else Text(stringResource(R.string.update_engine))
                            }
                        }
                    }
                    SettingsPanel.ABOUT -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            BrandMark(size = 52.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                                SettingsDetail(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                            }
                        }
                        SettingsDetail(stringResource(R.string.about_text))
                        SettingsDetail(stringResource(R.string.open_source))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            title,
            Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OmniCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) { Column { content() } }
    }
}

@Composable
private fun SettingsCrease() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)))
    }
}

@Composable
private fun SettingsRow(
    icon: AppIcon?,
    title: String,
    value: String = "",
    onClick: (() -> Unit)? = null,
    busy: Boolean = false,
    showChevron: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    checked: Boolean? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            else if (icon != null) SvgIcon(icon, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            AnimatedContent(value, label = "setting-value") { label ->
                if (label.isNotEmpty()) Text(
                    label,
                    Modifier.widthIn(max = 150.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) trailing()
            else AnimatedContent(busy, label = "setting-busy") { loading ->
                if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = LocalContentColor.current, strokeWidth = 1.5.dp)
                else if (showChevron) SvgIcon(
                    AppIcon.CHEVRON_DOWN,
                    null,
                    Modifier.size(14.dp).graphicsLayer { rotationZ = -90f },
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (onClick == null) {
        content()
    } else {
        val checkedDescription = checked?.let {
            stringResource(if (it) R.string.settings_switch_on else R.string.settings_switch_off)
        }
        PaperPressable(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().semantics {
                if (checked != null && checkedDescription != null) {
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                    stateDescription = checkedDescription
                }
            },
            shape = RoundedCornerShape(14.dp),
            role = if (checked != null) Role.Switch else Role.Button,
        ) { content() }
    }
}

@Composable
private fun SettingsToggle(icon: AppIcon, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 650f),
        label = "paper-switch-thumb",
    )
    SettingsRow(
        icon,
        title,
        onClick = { onCheckedChange(!checked) },
        checked = checked,
        trailing = {
            Box(Modifier.size(width = 50.dp, height = 30.dp).paperWell(CircleShape)) {
                Box(
                    Modifier.offset(x = thumbOffset, y = 3.dp).size(24.dp).paperRaised(
                        shape = CircleShape,
                        elevation = 2.dp,
                        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    )
}

@Composable
private fun <Choice> SettingsChoices(options: List<Pair<Choice, String>>, selected: Choice, onSelect: (Choice) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (choice, label) ->
            PaperButton(
                onClick = { onSelect(choice) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                selected = choice == selected,
            ) {
                Text(label, Modifier.weight(1f))
                AnimatedVisibility(choice == selected) {
                    SvgIcon(AppIcon.CHECK_CIRCLE, null, Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsDetail(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun themeLabel(theme: ThemeMode): String = stringResource(
    when (theme) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.LIGHT -> R.string.theme_light
    },
)

private fun importCookieFile(
    context: android.content.Context,
    resolver: android.content.ContentResolver,
    uri: Uri,
): File? = runCatching {
    val cookies = File(context.filesDir, "cookies.txt")
    val temporary = File(context.filesDir, "cookies.txt.tmp")
    resolver.openInputStream(uri).use { input ->
        requireNotNull(input)
        temporary.outputStream().use { output -> input.copyTo(output) }
    }
    val text = temporary.takeIf { it.length() in 1..10_000_000 }?.readText().orEmpty()
    val valid = text.contains("Netscape HTTP Cookie File", ignoreCase = true) ||
        text.lineSequence().any { line -> !line.isBlank() && !line.startsWith("#") && line.split('\t').size >= 7 }
    if (!valid) {
        temporary.delete()
        return null
    }
    if (temporary.renameTo(cookies)) cookies
    else {
        temporary.copyTo(cookies, overwrite = true)
        temporary.delete()
        cookies
    }
}.getOrNull()
