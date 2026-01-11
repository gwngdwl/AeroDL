package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.kdroidfilter.logging.errorln
import io.github.kdroidfilter.logging.infoln
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlpgui.core.domain.manager.DownloadManager
import io.github.kdroidfilter.ytdlpgui.core.navigation.Destination
import io.github.kdroidfilter.ytdlpgui.core.ui.MVIViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BulkDownloadViewModel @AssistedInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val ytDlpWrapper: YtDlpWrapper,
    private val downloadManager: DownloadManager,
) : MVIViewModel<BulkDownloadState, BulkDownloadEvents>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): BulkDownloadViewModel
    }

    override fun initialState(): BulkDownloadState = BulkDownloadState.loadingState

    val playlistUrl = savedStateHandle.toRoute<Destination.Download.Bulk>().url

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _playlistInfo = MutableStateFlow<PlaylistInfo?>(null)
    private val _selectedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _availablePresets = MutableStateFlow<List<YtDlpWrapper.Preset>>(emptyList())
    private val _selectedPreset = MutableStateFlow<YtDlpWrapper.Preset?>(null)
    private val _availableAudioQualityPresets = MutableStateFlow<List<YtDlpWrapper.AudioQualityPreset>>(emptyList())
    private val _selectedAudioQualityPreset = MutableStateFlow<YtDlpWrapper.AudioQualityPreset?>(null)
    private val _isAudioOnly = MutableStateFlow(false)
    private val _navigationState = MutableStateFlow<BulkDownloadNavigationState>(BulkDownloadNavigationState.None)

    override val uiState = combine(
        _isLoading,
        _errorMessage,
        _playlistInfo,
        _selectedVideoIds,
        _availablePresets,
        _selectedPreset,
        _availableAudioQualityPresets,
        _selectedAudioQualityPreset,
        _isAudioOnly,
        _navigationState,
    ) { values: Array<Any?> ->
        BulkDownloadState(
            isLoading = values[0] as Boolean,
            errorMessage = values[1] as String?,
            playlistInfo = values[2] as PlaylistInfo?,
            selectedVideoIds = @Suppress("UNCHECKED_CAST") (values[3] as Set<String>),
            availablePresets = @Suppress("UNCHECKED_CAST") (values[4] as List<YtDlpWrapper.Preset>),
            selectedPreset = values[5] as YtDlpWrapper.Preset?,
            availableAudioQualityPresets = @Suppress("UNCHECKED_CAST") (values[6] as List<YtDlpWrapper.AudioQualityPreset>),
            selectedAudioQualityPreset = values[7] as YtDlpWrapper.AudioQualityPreset?,
            isAudioOnly = values[8] as Boolean,
            navigationState = values[9] as BulkDownloadNavigationState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BulkDownloadState.loadingState,
    )

    init {
        loadPlaylistInfo()
    }

    private fun loadPlaylistInfo() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            infoln { "[BulkDownloadViewModel] Getting playlist info for $playlistUrl" }
            ytDlpWrapper.getPlaylistInfo(
                url = playlistUrl,
                extractFlat = false,
            ).onSuccess { info ->
                infoln { "[BulkDownloadViewModel] Got playlist info: ${info.title}, ${info.entries.size} entries" }
                _playlistInfo.value = info

                // Select all videos by default
                _selectedVideoIds.value = info.entries.map { it.id }.toSet()

                // Set available presets
                _availablePresets.value = YtDlpWrapper.Preset.entries.toList()
                _selectedPreset.value = YtDlpWrapper.Preset.P720

                _availableAudioQualityPresets.value = YtDlpWrapper.AudioQualityPreset.entries.toList()
                _selectedAudioQualityPreset.value = YtDlpWrapper.AudioQualityPreset.HIGH

                _isLoading.value = false
            }.onFailure { error ->
                val detail = error.localizedMessage ?: error.message ?: error.toString()
                errorln { "[BulkDownloadViewModel] Error getting playlist info: $detail" }
                _errorMessage.value = detail
                _isLoading.value = false
            }
        }
    }

    override fun handleEvent(event: BulkDownloadEvents) {
        when (event) {
            BulkDownloadEvents.Refresh -> {
                _isLoading.value = true
                _errorMessage.value = null
                loadPlaylistInfo()
            }

            is BulkDownloadEvents.ToggleVideoSelection -> {
                val current = _selectedVideoIds.value
                _selectedVideoIds.value = if (current.contains(event.videoId)) {
                    current - event.videoId
                } else {
                    current + event.videoId
                }
            }

            BulkDownloadEvents.SelectAll -> {
                _playlistInfo.value?.let { info ->
                    _selectedVideoIds.value = info.entries.map { it.id }.toSet()
                }
            }

            BulkDownloadEvents.DeselectAll -> {
                _selectedVideoIds.value = emptySet()
            }

            is BulkDownloadEvents.SelectPreset -> {
                _selectedPreset.value = event.preset
            }

            is BulkDownloadEvents.SelectAudioQualityPreset -> {
                _selectedAudioQualityPreset.value = event.preset
            }

            is BulkDownloadEvents.SetAudioOnly -> {
                _isAudioOnly.value = event.enabled
            }

            BulkDownloadEvents.StartDownload -> {
                startBulkDownload()
            }

            BulkDownloadEvents.ScreenDisposed -> {
                _playlistInfo.value = null
                _selectedVideoIds.value = emptySet()
                _availablePresets.value = emptyList()
                _selectedPreset.value = null
                _errorMessage.value = null
                _isLoading.value = false
            }

            BulkDownloadEvents.OnNavigationConsumed -> {
                _navigationState.value = BulkDownloadNavigationState.None
            }
        }
    }

    private fun startBulkDownload() {
        val playlist = _playlistInfo.value ?: return
        val selectedIds = _selectedVideoIds.value
        val preset = _selectedPreset.value
        val audioPreset = _selectedAudioQualityPreset.value
        val isAudio = _isAudioOnly.value

        val selectedVideos = playlist.entries.filter { it.id in selectedIds }

        infoln { "[BulkDownloadViewModel] Starting bulk download: ${selectedVideos.size} videos, audio=$isAudio" }

        selectedVideos.forEach { video ->
            if (isAudio) {
                downloadManager.startAudio(video.url, video, audioPreset)
            } else {
                downloadManager.start(video.url, video, preset)
            }
        }

        _navigationState.value = BulkDownloadNavigationState.NavigateToDownloader
    }
}
