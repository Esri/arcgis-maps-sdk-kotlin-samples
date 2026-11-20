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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
            Text(
                text = "$downloadedCount of ${entries.size} samples downloaded",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
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

@Composable
private fun DownloadEntryRow(
    entry: SampleDownloadEntry,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.sample.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            DownloadStatusContent(entry.status)
        }

        when (entry.status) {
            DownloadUiState.NotDownloaded,
            is DownloadUiState.Failed -> Button(onClick = onDownload) {
                Text(if (entry.status is DownloadUiState.Failed) "Retry" else "Download")
            }
            DownloadUiState.Queued,
            is DownloadUiState.Downloading -> TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            DownloadUiState.Downloaded,
            DownloadUiState.WaitingForConfiguration -> Unit
        }
    }
}

@Composable
private fun DownloadStatusContent(status: DownloadUiState) {
    when (status) {
        DownloadUiState.NotDownloaded -> Text("Not downloaded", style = MaterialTheme.typography.bodySmall)
        DownloadUiState.Queued -> Text("Queued", style = MaterialTheme.typography.bodySmall)
        DownloadUiState.WaitingForConfiguration -> Text("Checking", style = MaterialTheme.typography.bodySmall)
        DownloadUiState.Downloaded -> Text("Downloaded", style = MaterialTheme.typography.bodySmall)
        is DownloadUiState.Failed -> Text(
            text = status.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        is DownloadUiState.Downloading -> {
            val progress = status.progress
            val label = progress?.let { "$it% · " } ?: ""
            Text(
                text = "${label}${status.itemIndex + 1} of ${status.itemCount}",
                style = MaterialTheme.typography.bodySmall
            )
            if (progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}