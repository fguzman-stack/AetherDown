package com.aetherdown.app.presentation.queue

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.aetherdown.app.R
import com.aetherdown.app.data.local.entity.DownloadEntity
import com.aetherdown.app.data.local.entity.DownloadStatus
import com.aetherdown.app.ui.theme.DownloadBlue
import com.aetherdown.app.ui.theme.DownloadGreen
import com.aetherdown.app.ui.theme.DownloadOrange
import com.aetherdown.app.ui.theme.DownloadPaused
import com.aetherdown.app.ui.theme.DownloadRed
import com.aetherdown.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.nav_queue), fontWeight = FontWeight.Bold)
                        if (state.activeCount > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${state.activeCount} ${stringResource(R.string.pending).lowercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_downloads),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.no_downloads_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = state.downloads,
                    key = { it.id }
                ) { download ->
                    DownloadItem(
                        download = download,
                        onPause = { viewModel.pause(download.id) },
                        onResume = { viewModel.resume(download.id) },
                        onCancel = { viewModel.cancel(download.id) },
                        onDelete = { viewModel.delete(download.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (download.status) {
        DownloadStatus.DOWNLOADING -> DownloadBlue
        DownloadStatus.COMPLETED -> DownloadGreen
        DownloadStatus.FAILED -> DownloadRed
        DownloadStatus.PAUSED -> DownloadPaused
        DownloadStatus.PENDING -> DownloadOrange
        DownloadStatus.QUEUED -> DownloadOrange
        DownloadStatus.VERIFYING -> DownloadBlue
    }

    val statusIcon = when (download.status) {
        DownloadStatus.DOWNLOADING -> Icons.Filled.PauseCircle
        DownloadStatus.COMPLETED -> Icons.Filled.CheckCircle
        DownloadStatus.FAILED -> Icons.Filled.Cancel
        DownloadStatus.PAUSED -> Icons.Filled.PlayCircle
        DownloadStatus.PENDING -> Icons.Filled.HourglassEmpty
        DownloadStatus.QUEUED -> Icons.Filled.HourglassEmpty
        DownloadStatus.VERIFYING -> Icons.Filled.Verified
    }

    val statusText = when (download.status) {
        DownloadStatus.DOWNLOADING -> stringResource(R.string.downloading)
        DownloadStatus.COMPLETED -> stringResource(R.string.completed)
        DownloadStatus.FAILED -> stringResource(R.string.failed)
        DownloadStatus.PAUSED -> stringResource(R.string.paused)
        DownloadStatus.PENDING -> stringResource(R.string.pending)
        DownloadStatus.QUEUED -> stringResource(R.string.queued)
        DownloadStatus.VERIFYING -> stringResource(R.string.verifying)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title.ifEmpty { download.fileName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$statusText · ${FormatUtils.formatFileSize(download.fileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                if (download.status == DownloadStatus.DOWNLOADING) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = FormatUtils.formatSpeed(download.speed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${download.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (download.status == DownloadStatus.DOWNLOADING && download.eta > 0) {
                        Text(
                            "ETA: ${FormatUtils.formatEta(download.eta)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (download.status == DownloadStatus.FAILED && download.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DownloadRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        download.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = DownloadRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        SmallActionButton(Icons.Filled.Pause, stringResource(R.string.pause), DownloadOrange, onPause)
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallActionButton(Icons.Filled.Close, stringResource(R.string.cancel), DownloadRed, onCancel)
                    }
                    DownloadStatus.PAUSED -> {
                        SmallActionButton(Icons.Filled.PlayArrow, stringResource(R.string.resume), DownloadGreen, onResume)
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallActionButton(Icons.Filled.Close, stringResource(R.string.cancel), DownloadRed, onCancel)
                    }
                    DownloadStatus.COMPLETED -> {
                        SmallActionButton(Icons.Filled.Delete, stringResource(R.string.delete), MaterialTheme.colorScheme.onSurfaceVariant, onDelete)
                    }
                    DownloadStatus.FAILED -> {
                        SmallActionButton(Icons.Filled.Refresh, stringResource(R.string.retry), DownloadBlue, onResume)
                        Spacer(modifier = Modifier.width(8.dp))
                        SmallActionButton(Icons.Filled.Delete, stringResource(R.string.delete), DownloadRed, onDelete)
                    }
                    else -> {
                        SmallActionButton(Icons.Filled.Close, stringResource(R.string.cancel), DownloadRed, onCancel)
                    }
                }
                if (download.platform.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            download.platform,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, color: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = color
        )
    }
}
