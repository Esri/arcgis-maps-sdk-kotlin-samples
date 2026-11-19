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

package com.esri.arcgismaps.sample.sampleslib.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.R


/**
 * Download screen for samples that require offline data
 */
@Composable
internal fun DownloaderScreen(
    uiState: DownloadUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onExit: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    DownloadUiState.WaitingForConfiguration -> CircularProgressIndicator()

                    is DownloadUiState.Downloading -> DownloadingContent(
                        progress = uiState.progress,
                        itemIndex = uiState.itemIndex,
                        itemCount = uiState.itemCount,
                        onCancel = onCancel
                    )

                    is DownloadUiState.Failed -> FailedContent(
                        message = uiState.message,
                        onRetry = onDownload,
                        onExit = onExit
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    progress: Int?,
    itemIndex: Int,
    itemCount: Int,
    onCancel: () -> Unit
) {
    ScreenTitle(text = stringResource(R.string.downloading_data))
    Text(text = "${itemIndex + 1} / $itemCount")
    if (progress == null) {
        CircularProgressIndicator()
    } else {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("$progress%")
    }
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = onCancel) {
        Text(stringResource(R.string.cancel))
    }
}

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit, onExit: () -> Unit) {
    ScreenTitle(text = "Download failed")
    Text(text = message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Retry")
    }
    TextButton(onClick = onExit) {
        Text("Exit")
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(12.dp))
}