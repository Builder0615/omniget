package com.openselena.omniget.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openselena.omniget.OmniGetApplication
import com.openselena.omniget.data.DownloadMode
import com.openselena.omniget.data.DownloadTask
import com.openselena.omniget.data.MediaPreview
import com.openselena.omniget.data.QualityPreset
import com.openselena.omniget.download.DownloadEngine
import com.openselena.omniget.util.PlatformDetector
import com.openselena.omniget.util.PlatformInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val platform: PlatformInfo = PlatformInfo("generic", "Web", 0xFF0A84FF),
    val mode: DownloadMode = DownloadMode.VIDEO,
    val quality: QualityPreset = QualityPreset.BEST,
    val preview: MediaPreview? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val isValidUrl: Boolean = false,
)

sealed interface HomeEvent {
    data object AddedToQueue : HomeEvent
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OmniGetApplication
    private val _state = MutableStateFlow(
        HomeUiState(
            mode = app.settings.settings.value.defaultMode,
            quality = app.settings.settings.value.defaultQuality,
        ),
    )
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    private var inspectJob: Job? = null
    private var lastCanceledUrl: String? = null

    val state = _state.asStateFlow()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            app.settings.settings
                .map { it.defaultMode to it.defaultQuality }
                .distinctUntilChanged()
                .collect { (mode, quality) ->
                    _state.update { it.copy(mode = mode, quality = quality) }
                }
        }
    }

    fun onUrlChanged(raw: String) {
        val extractedUrl = PlatformDetector.extractFirstUrl(raw)
        val normalized = PlatformDetector.normalizeUrl(extractedUrl ?: raw)
        val valid = PlatformDetector.isValidUrl(normalized)
        inspectJob?.cancel()
        DownloadEngine.cancelPreview()
        _state.update {
            it.copy(
                url = extractedUrl ?: raw,
                platform = if (valid) PlatformDetector.detect(normalized) else it.platform,
                preview = null,
                isAnalyzing = false,
                error = null,
                isValidUrl = valid,
            )
        }
        if (!valid) return

        inspectJob = viewModelScope.launch {
            delay(650)
            analyzeInternal(normalized)
        }
    }

    fun analyze() {
        val normalized = PlatformDetector.normalizeUrl(_state.value.url)
        if (!PlatformDetector.isValidUrl(normalized)) {
            _state.update { it.copy(error = null, isValidUrl = false) }
            return
        }
        inspectJob?.cancel()
        DownloadEngine.cancelPreview()
        inspectJob = viewModelScope.launch { analyzeInternal(normalized) }
    }

    fun acceptSharedText(text: String) {
        val url = PlatformDetector.extractFirstUrl(text)
            ?: PlatformDetector.normalizeUrl(text).takeIf(PlatformDetector::isValidUrl)
            ?: return
        _state.update {
            it.copy(
                url = url,
                platform = PlatformDetector.detect(url),
                preview = null,
                error = null,
                isValidUrl = true,
                isAnalyzing = true,
            )
        }
        inspectJob?.cancel()
        DownloadEngine.cancelPreview()
        inspectJob = viewModelScope.launch { analyzeInternal(url) }
    }

    fun selectMode(mode: DownloadMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun selectQuality(quality: QualityPreset) {
        _state.update { it.copy(quality = quality) }
    }

    fun download(mode: DownloadMode, quality: QualityPreset) {
        val state = _state.value
        val preview = state.preview ?: return

        val platform = PlatformDetector.detect(preview.url)
        app.downloads.enqueue(
            DownloadTask(
                url = preview.url,
                title = preview.title,
                platform = platform.id,
                uploader = preview.uploader,
                thumbnailUrl = preview.thumbnailUrl,
                durationSeconds = preview.durationSeconds,
                mode = mode,
                quality = quality,
            ),
        )
        _state.update {
            it.copy(
                url = "",
                preview = null,
                error = null,
                isAnalyzing = false,
                isValidUrl = false,
                mode = mode,
                quality = quality,
            )
        }
        _events.trySend(HomeEvent.AddedToQueue)
    }

    fun clear() {
        inspectJob?.cancel()
        DownloadEngine.cancelPreview()
        _state.value = HomeUiState(
            mode = app.settings.settings.value.defaultMode,
            quality = app.settings.settings.value.defaultQuality,
        )
    }

    override fun onCleared() {
        DownloadEngine.cancelPreview()
        super.onCleared()
    }

    private suspend fun analyzeInternal(url: String) {
        _state.update { it.copy(isAnalyzing = true, error = null) }
        try {
            val preview = DownloadEngine.inspect(getApplication(), url, app.settings.settings.value)
            if (PlatformDetector.normalizeUrl(_state.value.url) != url) return
            _state.update {
                it.copy(
                    preview = preview,
                    isAnalyzing = false,
                    error = null,
                    platform = PlatformDetector.detect(url),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (PlatformDetector.normalizeUrl(_state.value.url) != url) return
            _state.update {
                it.copy(
                    preview = null,
                    isAnalyzing = false,
                    error = friendlyPreviewError(error),
                )
            }
        }
    }

    private fun friendlyPreviewError(error: Throwable): String {
        val detail = error.message.orEmpty()
            .lineSequence()
            .map(String::trim)
            .lastOrNull { it.isNotBlank() }
            ?.removePrefix("ERROR:")
            ?.trim()
            .orEmpty()
        return detail.take(420).ifBlank { error.javaClass.simpleName }
    }
}
