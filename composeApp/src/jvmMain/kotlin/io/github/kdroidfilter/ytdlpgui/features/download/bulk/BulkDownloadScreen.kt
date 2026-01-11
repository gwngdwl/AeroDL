package io.github.kdroidfilter.ytdlpgui.features.download.bulk

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.*
import io.github.kdroidfilter.ytdlp.YtDlpWrapper
import io.github.kdroidfilter.ytdlp.model.PlaylistInfo
import io.github.kdroidfilter.ytdlp.model.VideoInfo
import io.github.kdroidfilter.ytdlpgui.core.design.components.Switcher
import io.github.kdroidfilter.ytdlpgui.di.LocalAppGraph
import org.jetbrains.compose.resources.stringResource
import ytdlpgui.composeapp.generated.resources.*
import java.time.Duration

@Composable
fun BulkDownloadScreen(
    navController: androidx.navigation.NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry
) {
    val appGraph = LocalAppGraph.current
    val viewModel = remember(backStackEntry) {
        appGraph.bulkDownloadViewModelFactory.create(backStackEntry.savedStateHandle)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigationState) {
        when (state.navigationState) {
            is BulkDownloadNavigationState.NavigateToDownloader -> {
                navController.navigate(io.github.kdroidfilter.ytdlpgui.core.navigation.Destination.MainNavigation.Downloader)
                viewModel.handleEvent(BulkDownloadEvents.OnNavigationConsumed)
            }
            BulkDownloadNavigationState.None -> {}
        }
    }

    BulkDownloadView(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
fun BulkDownloadView(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose {
            onEvent(BulkDownloadEvents.ScreenDisposed)
        }
    }

    when {
        state.isLoading -> LoadingView()
        state.errorMessage != null -> ErrorView(state.errorMessage)
        state.playlistInfo != null -> PlaylistContentView(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.playlist_loading))
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Regular.ErrorCircle,
            contentDescription = null,
            modifier = Modifier.size(144.dp),
            tint = FluentTheme.colors.system.critical
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = message,
            color = FluentTheme.colors.system.critical,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaylistContentView(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    val playlist = state.playlistInfo ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        // Playlist header
        PlaylistHeader(playlist = playlist)

        Spacer(Modifier.height(16.dp))

        // Selection controls
        SelectionControls(
            selectedCount = state.selectedCount,
            totalCount = state.totalCount,
            allSelected = state.allSelected,
            onSelectAll = { onEvent(BulkDownloadEvents.SelectAll) },
            onDeselectAll = { onEvent(BulkDownloadEvents.DeselectAll) },
        )

        Spacer(Modifier.height(12.dp))

        // Video list
        Box(modifier = Modifier.weight(1f)) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlist.entries, key = { it.id }) { video ->
                    VideoItem(
                        video = video,
                        isSelected = video.id in state.selectedVideoIds,
                        onToggle = { onEvent(BulkDownloadEvents.ToggleVideoSelection(video.id)) },
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(16.dp))

        // Download options
        DownloadOptions(
            state = state,
            onEvent = onEvent,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlaylistHeader(playlist: PlaylistInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist thumbnail
        AsyncImage(
            model = playlist.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title ?: stringResource(Res.string.playlist_untitled),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            playlist.uploader?.let { uploader ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uploader,
                    style = FluentTheme.typography.caption,
                    color = FluentTheme.colors.text.secondary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.playlist_video_count, playlist.entries.size),
                style = FluentTheme.typography.caption,
                color = FluentTheme.colors.text.secondary
            )
        }
    }
}

@Composable
private fun SelectionControls(
    selectedCount: Int,
    totalCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.playlist_selected_count, selectedCount, totalCount),
            style = FluentTheme.typography.body
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtleButton(onClick = onSelectAll) {
                Text(stringResource(Res.string.playlist_select_all))
            }
            SubtleButton(onClick = onDeselectAll) {
                Text(stringResource(Res.string.playlist_deselect_all))
            }
        }
    }
}

@Composable
private fun VideoItem(
    video: VideoInfo,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) FluentTheme.colors.subtleFill.secondary
                else FluentTheme.colors.subtleFill.transparent
            )
            .clickable(onClick = onToggle)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )

        Spacer(Modifier.width(8.dp))

        // Video thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Duration badge
            video.duration?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(FluentTheme.colors.background.acrylic.base.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(duration),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = FluentTheme.typography.body
            )
            video.uploader?.let { uploader ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uploader,
                    style = FluentTheme.typography.caption,
                    color = FluentTheme.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DownloadOptions(
    state: BulkDownloadState,
    onEvent: (BulkDownloadEvents) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Audio only toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Regular.MusicNote2, contentDescription = null)
                Text(stringResource(Res.string.playlist_audio_only))
            }
            Switcher(
                checked = state.isAudioOnly,
                onCheckStateChange = { onEvent(BulkDownloadEvents.SetAudioOnly(it)) },
            )
        }

        // Quality selector
        if (!state.isAudioOnly && state.availablePresets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.playlist_video_quality))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.availablePresets.forEach { preset ->
                        SegmentedControl {
                            SegmentedButton(
                                checked = preset == state.selectedPreset,
                                onCheckedChanged = { onEvent(BulkDownloadEvents.SelectPreset(preset)) },
                                position = SegmentedItemPosition.Center,
                                text = { Text("${preset.height}p") }
                            )
                        }
                    }
                }
            }
        }

        // Audio quality selector
        if (state.isAudioOnly && state.availableAudioQualityPresets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.playlist_audio_quality))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.availableAudioQualityPresets.forEach { preset ->
                        SegmentedControl {
                            SegmentedButton(
                                checked = preset == state.selectedAudioQualityPreset,
                                onCheckedChanged = { onEvent(BulkDownloadEvents.SelectAudioQualityPreset(preset)) },
                                position = SegmentedItemPosition.Center,
                                text = { Text(preset.bitrate) }
                            )
                        }
                    }
                }
            }
        }

        // Download button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AccentButton(
                onClick = { onEvent(BulkDownloadEvents.StartDownload) },
                enabled = state.selectedCount > 0
            ) {
                Icon(Icons.Default.ArrowDownload, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        Res.string.playlist_download_selected,
                        state.selectedCount
                    )
                )
            }
        }
    }
}

private fun formatDuration(d: Duration): String {
    val totalSec = d.seconds.coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
