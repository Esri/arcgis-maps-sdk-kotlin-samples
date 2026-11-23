/* Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgismaps.kotlin.sampleviewer.ui.screens.centerDownloading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esri.arcgismaps.kotlin.sampleviewer.ui.components.SampleViewerTopAppBar
import com.esri.arcgismaps.sample.sampleslib.download.DownloadUiState

@Composable
internal fun CenterDownloadingScreen(
    onBackPressed: () -> Unit,
    viewModel: CenterDownloadingViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val downloadedCount = entries.count { it.status == DownloadUiState.Downloaded }
    val hasActiveDownloads = entries.any {
        it.status == DownloadUiState.Queued || it.status is DownloadUiState.Downloading
    }
    val progress = if (entries.isEmpty()) 0f else downloadedCount.toFloat() / entries.size
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete downloaded data?") },
            text = {
                Text("This will remove offline data for $downloadedCount samples. You can download it again later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAllDownloadedData()
                    }
                ) {
                    Text("Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Keep data")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SampleViewerTopAppBar(
                title = "Downloads",
                onBackPressed = onBackPressed
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$downloadedCount of ${entries.size} samples downloaded",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (entries.isNotEmpty() && downloadedCount < entries.size) {
                        FilledTonalButton(
                            onClick = {
                                if (hasActiveDownloads) viewModel.cancelAll() else viewModel.downloadAll()
                            }
                        ) {
                            Icon(
                                imageVector = if (hasActiveDownloads) {
                                    Icons.Outlined.Close
                                } else {
                                    Icons.Outlined.CloudDownload
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (hasActiveDownloads) "Cancel all" else "Download all")
                        }
                    }
                }
                if (downloadedCount > 0) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = !hasActiveDownloads,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Delete downloaded data")
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outline
                )
            }
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No sample downloads", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.sample.mainActivity }) { entry ->
                        DownloadEntryRow(
                            entry = entry,
                            onDownload = { viewModel.download(entry.sample) },
                            onCancel = { viewModel.cancel(entry.sample) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadEntryRow(
    entry: SampleDownloadEntry,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(entry.sample.name, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            DownloadStatusContent(entry.status)
        },
        trailingContent = {
            when (entry.status) {
                DownloadUiState.NotDownloaded -> DownloadAction(
                    icon = Icons.Outlined.CloudDownload,
                    description = "Download ${entry.sample.name}",
                    onClick = onDownload
                )
                is DownloadUiState.Failed -> DownloadAction(
                    icon = Icons.Outlined.Refresh,
                    description = "Retry download for ${entry.sample.name}",
                    onClick = onDownload
                )
                DownloadUiState.Queued,
                is DownloadUiState.Downloading -> DownloadAction(
                    progress = (entry.status as? DownloadUiState.Downloading)?.progress,
                    description = "Cancel download for ${entry.sample.name}",
                    onClick = onCancel
                )
                DownloadUiState.Downloaded,
                DownloadUiState.WaitingForConfiguration -> Unit
            }
        }
    )
}

@Composable
private fun DownloadAction(
    icon: ImageVector? = null,
    progress: Int? = null,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else if (icon == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}

@Composable
private fun DownloadStatusContent(status: DownloadUiState) {
    when (status) {
        DownloadUiState.NotDownloaded -> Text("Not downloaded", style = MaterialTheme.typography.bodyMedium)
        DownloadUiState.Queued -> Text("Queued", style = MaterialTheme.typography.bodyMedium)
        DownloadUiState.WaitingForConfiguration -> Text("Checking", style = MaterialTheme.typography.bodyMedium)
        DownloadUiState.Downloaded -> Text("Downloaded", style = MaterialTheme.typography.bodyMedium)
        is DownloadUiState.Failed -> Text(
            text = "Download failed: ${status.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        is DownloadUiState.Downloading -> {
            val progress = status.progress
            val label = progress?.let { "$it% · " } ?: ""
            Text(
                text = "${label}${status.itemIndex + 1} of ${status.itemCount}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
