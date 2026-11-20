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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esri.arcgismaps.kotlin.sampleviewer.model.DefaultSampleInfoRepository
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import com.esri.arcgismaps.sample.sampleslib.download.DownloadUiState
import com.esri.arcgismaps.sample.sampleslib.download.SampleDownloadManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class SampleDownloadEntry(
    val sample: Sample,
    val status: DownloadUiState
)

internal class CenterDownloadingViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = SampleDownloadManager.getInstance(application)

    val entries: StateFlow<List<SampleDownloadEntry>> = combine(
        DefaultSampleInfoRepository.getAllSamples(),
        downloadManager.statuses
    ) { samples, statuses ->
        samples.asSequence()
            .filter { it.metadata.offlineDataIDs.isNotEmpty() }
            .map { sample ->
                SampleDownloadEntry(
                    sample = sample,
                    status = statuses[sample.mainActivity] ?: DownloadUiState.NotDownloaded
                )
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun download(sample: Sample) {
        viewModelScope.launch {
            try {
                downloadManager.requestDownload(
                    sampleId = sample.mainActivity,
                    sampleName = sample.name,
                    itemIds = sample.metadata.offlineDataIDs
                ).join()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // The manager publishes the failure state for the row.
            }
        }
    }

    fun cancel(sample: Sample) {
        viewModelScope.launch {
            downloadManager.cancelDownload(sample.mainActivity)
        }
    }
}