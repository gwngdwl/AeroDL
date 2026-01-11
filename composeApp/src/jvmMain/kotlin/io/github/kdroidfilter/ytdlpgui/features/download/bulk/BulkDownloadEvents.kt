package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import io.github.kdroidfilter.ytdlp.YtDlpWrapper

sealed class BulkDownloadEvents {
    data object Refresh : BulkDownloadEvents()
    data class ToggleVideoSelection(val videoId: String) : BulkDownloadEvents()
    data object SelectAll : BulkDownloadEvents()
    data object DeselectAll : BulkDownloadEvents()
    data class SelectPreset(val preset: YtDlpWrapper.Preset) : BulkDownloadEvents()
    data class SelectAudioQualityPreset(val preset: YtDlpWrapper.AudioQualityPreset) : BulkDownloadEvents()
    data class SetAudioOnly(val enabled: Boolean) : BulkDownloadEvents()
    data object StartDownload : BulkDownloadEvents()
    data object ScreenDisposed : BulkDownloadEvents()
    data object OnNavigationConsumed : BulkDownloadEvents()
}
