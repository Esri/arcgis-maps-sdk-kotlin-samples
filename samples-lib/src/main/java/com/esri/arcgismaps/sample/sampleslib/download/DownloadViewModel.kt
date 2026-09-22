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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

internal sealed interface DownloadUiState {
    data object WaitingForConfiguration : DownloadUiState
    data class Downloading(val progress: Int?) : DownloadUiState
    data class Failed(val message: String) : DownloadUiState
}

internal class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = SampleDownloadManager()
    private val _uiState = MutableStateFlow<DownloadUiState>(
        DownloadUiState.WaitingForConfiguration
    )
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val launchEvents = Channel<Unit>(Channel.BUFFERED)
    val launchSample = launchEvents.receiveAsFlow()

    private var itemIds: List<String> = emptyList()
    private var destinationFolder: File? = null
    private var downloadJob: Job? = null

    fun configure(sampleName: String, provisionUrls: List<String>) {
        if (destinationFolder != null) return

        this.itemIds = provisionUrls
        destinationFolder = File(
            getApplication<Application>().getExternalFilesDir(null),
            sampleName
        )
        startDownload()
    }

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        val destination = destinationFolder ?: return

        downloadJob = viewModelScope.launch {
            _uiState.value = DownloadUiState.Downloading(progress = null)
            try {
                engine.download(itemIds, destination) { progress ->
                    _uiState.value = DownloadUiState.Downloading(progress)
                }
                launchEvents.send(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = DownloadUiState.Failed(
                    exception.message ?: "Unable to download sample data."
                )
            }
        }
    }

    suspend fun cancelDownload() {
        engine.cancelDownLoad()
        downloadJob?.cancel()
        downloadJob?.join()

        destinationFolder?.let { destination ->
            engine.delete(destination)
        }
    }
}