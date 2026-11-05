package com.openselena.omniget.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openselena.omniget.BuildConfig
import com.openselena.omniget.R
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadRepository
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.data.SettingsRepository
import com.openselena.omniget.data.ThemeMode
import com.openselena.omniget.download.DownloadEngine
import com.openselena.omniget.ui.components.BrandMark
import com.openselena.omniget.ui.components.OmniCard
import com.openselena.omniget.ui.components.qualityLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    downloads: DownloadRepository,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showBranding: Boolean = true,
) {
    val context = LocalContext.current
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var engineVersion by remember { mutableStateOf(DownloadEngine.version(context)) }
    var updatingEngine by remember { mutableStateOf(false) }
    var importingCookies by remember { mutableStateOf(false) }

    val cookieImported = stringResource(R.string.cookie_imported)
    val cookieInvalid = stringResource(R.string.cookie_invalid)
    val engineUpdated = stringResource(R.string.engine_updated)
    val engineBusy = stringResource(R.string.engine_busy)
    val engineFailurePrefix = stringResource(R.string.engine_update_failed, "")

    val cookiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importingCookies = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importCookieFile(context, context.contentResolver, uri)
            }
            importingCookies = false
            if (result != null) {
                settingsRepository.setCookiePath(result.absolutePath)
                snackbarHostState.showSnackbar(cookieImported)
            } else {
                snackbarHostState.showSnackbar(cookieInvalid)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 880.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBranding) {
                    BrandMark(size = 38.dp)
                    Spacer(Modifier.width(11.dp))
                }
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.app_name) + " · Android",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_appearance),
            ) {
                SegmentedChoices(
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ),
                    selected = settings.theme,
                    onSelect = { theme -> settingsRepository.update { it.copy(theme = theme) } },
                )
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.Download,
                title = stringResource(R.string.settings_downloads),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingLabel(stringResource(R.string.default_mode))
                        SegmentedChoices(
                            options = listOf(
                                DownloadMode.VIDEO to stringResource(R.string.video),
                                DownloadMode.AUDIO to stringResource(R.string.audio),
                            ),
                            selected = settings.defaultMode,
                            onSelect = { mode ->
                                settingsRepository.update { it.copy(defaultMode = mode) }
                            },
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingLabel(stringResource(R.string.default_quality))
                        QualityChoices(
                            selected = settings.defaultQuality,
                            onSelect = { quality ->
                                settingsRepository.update { it.copy(defaultQuality = quality) }
                            },
                        )
                    }
                    SettingSwitch(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.write_subtitles),
                        description = stringResource(R.string.write_subtitles_desc),
                        checked = settings.writeSubtitles,
                        onCheckedChange = { enabled ->
                            settingsRepository.update { it.copy(writeSubtitles = enabled) }
                        },
                    )
                    SettingSwitch(
                        icon = Icons.Rounded.Sync,
                        title = stringResource(R.string.allow_playlist),
                        description = stringResource(R.string.allow_playlist_desc),
                        checked = settings.allowPlaylist,
                        onCheckedChange = { enabled ->
                            settingsRepository.update { it.copy(allowPlaylist = enabled) }
                        },
                    )
                }
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.settings_storage),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SettingIconLine(
                        icon = Icons.Rounded.Folder,
                        title = "Downloads/OmniGet",
                        description = stringResource(R.string.storage_path),
                    )
                    Text(
                        text = stringResource(R.string.storage_internal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.Cookie,
                title = stringResource(R.string.settings_cookies),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingIconLine(
                        icon = Icons.Rounded.Cookie,
                        title = if (settings.cookiePath != null) {
                            stringResource(R.string.cookie_active)
                        } else {
                            stringResource(R.string.cookie_none)
                        },
                        description = stringResource(R.string.cookie_help),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { cookiePicker.launch(arrayOf("text/plain", "application/octet-stream")) },
                            enabled = !importingCookies,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            if (importingCookies) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(7.dp))
                            } else {
                                Icon(Icons.Rounded.Cookie, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(stringResource(R.string.cookie_import))
                        }
                        if (settings.cookiePath != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { settingsRepository.setCookiePath(null) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.cookie_clear),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.SettingsSuggest,
                title = stringResource(R.string.settings_engine),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.engine_version, engineVersion),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "yt-dlp · FFmpeg · QuickJS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            if (downloads.hasActiveDownloads()) {
                                scope.launch { snackbarHostState.showSnackbar(engineBusy) }
                                return@Button
                            }
                            updatingEngine = true
                            scope.launch {
                                val result = runCatching { DownloadEngine.update(context) }
                                engineVersion = DownloadEngine.version(context)
                                updatingEngine = false
                                result.onSuccess {
                                    snackbarHostState.showSnackbar(engineUpdated)
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar(
                                        engineFailurePrefix + error.message.orEmpty().take(180),
                                    )
                                }
                            }
                        },
                        enabled = !updatingEngine,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        if (updatingEngine) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(stringResource(R.string.update_engine))
                    }
                }
            }
        }

        item {
            SettingsSection(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_about),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.about_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                        InfoPill(stringResource(R.string.open_source))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
        OmniCard(Modifier.fillMaxWidth(), contentPadding = 15.dp) {
            content()
        }
    }
}

@Composable
private fun <T> SegmentedChoices(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QualityChoices(
    selected: QualityPreset,
    onSelect: (QualityPreset) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(QualityPreset.entries, key = { it.id }) { quality ->
            val active = quality == selected
            Text(
                text = qualityLabel(quality),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    )
                    .border(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(quality) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SettingLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingIconLine(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoPill(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun importCookieFile(
    context: android.content.Context,
    resolver: android.content.ContentResolver,
    uri: Uri,
): File? {
    return runCatching {
        val cookies = File(context.filesDir, "cookies.txt")
        val temporary = File(context.filesDir, "cookies.txt.tmp")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            temporary.outputStream().use { output -> input.copyTo(output) }
        }
        val text = temporary.takeIf { it.length() in 1..10_000_000 }?.readText().orEmpty()
        val valid = text.contains("Netscape HTTP Cookie File", ignoreCase = true) ||
            text.lineSequence().any { line ->
                !line.isBlank() &&
                    !line.startsWith("#") &&
                    line.split('\t').size >= 7
            }
        if (!valid) {
            temporary.delete()
            return null
        }
        if (temporary.renameTo(cookies)) {
            cookies
        } else {
            temporary.copyTo(cookies, overwrite = true)
            temporary.delete()
            cookies
        }
    }.getOrNull()
}
