package com.personal.lira.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.lira.data.DownloadItem
import com.personal.lira.data.DownloadRepository
import com.personal.lira.data.MediaDetails
import com.personal.lira.data.MediaExtractor
import com.personal.lira.data.MediaOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class AppUiState(
    val url: String = "",
    val media: MediaDetails? = null,
    val selectedOption: MediaOption? = null,
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val homeRequest: Int = 0,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloadRepository(application)
    private val extractor = MediaExtractor()
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    val downloads = repository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setUrl(value: String) {
        _uiState.update { it.copy(url = value, errorMessage = null) }
    }

    fun acceptSharedText(sharedText: String?) {
        val sharedUrl = sharedText
            ?.let { Regex("https?://[^\\s]+").find(it)?.value }
            ?.trimEnd('.', ',', ';', ')', ']', '>')
            ?: return
        _uiState.update {
            it.copy(
                url = sharedUrl,
                media = null,
                selectedOption = null,
                errorMessage = null,
                homeRequest = it.homeRequest + 1,
            )
        }
    }

    fun analyze() {
        val normalizedUrl = validatedUrl() ?: return
        _uiState.update {
            it.copy(
                isAnalyzing = true,
                errorMessage = null,
                media = null,
                selectedOption = null,
            )
        }
        viewModelScope.launch {
            try {
                val result = withTimeout(30_000L) {
                    extractor.analyze(normalizedUrl)
                }
                _uiState.update {
                    it.copy(
                        media = result,
                        selectedOption = result.options.firstOrNull { option -> option.id == "video_720" }
                            ?: result.options.firstOrNull(),
                    )
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No se pudieron consultar las calidades en 30 s. La descarga directa sigue disponible.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = error.message?.takeIf(String::isNotBlank)
                            ?: "No se pudo analizar este enlace.",
                    )
                }
            } finally {
                _uiState.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    fun startDirectVideoDownload(onQueued: () -> Unit) {
        val normalizedUrl = validatedUrl() ?: return
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = normalizedUrl,
            title = DownloadItem.DIRECT_DOWNLOAD_TITLE,
            artist = "yt-dlp",
            thumbnailUrl = null,
            kind = DownloadItem.KIND_VIDEO,
            quality = "Mejor calidad disponible",
            formatSelector = "bestvideo+bestaudio/best",
            extension = "auto",
        )
        enqueueDownload(item, onQueued)
    }

    fun selectOption(option: MediaOption) {
        _uiState.update { it.copy(selectedOption = option) }
    }

    fun startDownload(onQueued: () -> Unit) {
        val state = _uiState.value
        val details = state.media ?: return
        val option = state.selectedOption ?: return
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = state.url.trim(),
            title = details.title,
            artist = details.artist,
            thumbnailUrl = details.thumbnailUrl,
            kind = option.kind,
            quality = option.quality,
            formatSelector = option.formatSelector,
            extension = option.extension,
        )
        enqueueDownload(item, onQueued)
    }

    private fun enqueueDownload(item: DownloadItem, onQueued: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.enqueue(item)
                onQueued()
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = error.message?.takeIf(String::isNotBlank)
                            ?: "No se pudo añadir la descarga a la cola.",
                    )
                }
            }
        }
    }

    private fun validatedUrl(): String? {
        val normalizedUrl = _uiState.value.url.trim()
        val parsed = Uri.parse(normalizedUrl)
        if (parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Pega un enlace válido que empiece con http:// o https://.")
            }
            return null
        }
        return normalizedUrl
    }

    fun cancelDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.cancel(item.id)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
