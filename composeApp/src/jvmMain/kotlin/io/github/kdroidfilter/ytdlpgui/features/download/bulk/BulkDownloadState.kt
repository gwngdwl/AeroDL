package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo

sealed class BulkDownloadNavigationState {
    data object None : BulkDownloadNavigationState()
    data object NavigateToDownloader : BulkDownloadNavigationState()
}

data class BulkDownloadState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val playlistInfo: PlaylistInfo? = null,
    val selectedVideoIds: Set<String> = emptySet(),
    val availablePresets: List<YtDlpWrapper.Preset> = emptyList(),
    val selectedPreset: YtDlpWrapper.Preset? = null,
    val availableAudioQualityPresets: List<YtDlpWrapper.AudioQualityPreset> = emptyList(),
    val selectedAudioQualityPreset: YtDlpWrapper.AudioQualityPreset? = null,
    val isAudioOnly: Boolean = false,
    val navigationState: BulkDownloadNavigationState = BulkDownloadNavigationState.None,
) {
    val selectedCount: Int get() = selectedVideoIds.size
    val totalCount: Int get() = playlistInfo?.entries?.size ?: 0
    val allSelected: Boolean get() = selectedVideoIds.size == totalCount && totalCount > 0

    companion object {
        val loadingState = BulkDownloadState(isLoading = true)
    }
}
