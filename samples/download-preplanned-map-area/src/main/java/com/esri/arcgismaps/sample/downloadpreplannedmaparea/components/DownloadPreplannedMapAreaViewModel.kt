/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.downloadpreplannedmaparea.components

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedMapArea
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.esri.arcgismaps.sample.downloadpreplannedmaparea.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadPreplannedMapAreaViewModel(application: Application) : AndroidViewModel(application) {

    // The directory where the offline map will be saved
    private val offlineMapPath by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.download_preplanned_map_area_app_name
        )
    }

    // Create a portal to ArcGIS Online
    val portal = Portal("https://www.arcgis.com")

    // create a portal item using the portal and the item id of a map service
    val portalItem = PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674")

    private val offlineMapTask = OfflineMapTask(portalItem)

    // A list of preplanned map areas populated by the offline map task
    private var preplannedMapAreas = mutableListOf<PreplannedMapArea>()

    // Keep a hash map of downloaded maps
    private var downloadedMapAreas: HashMap<String, ArcGISMap> = hashMapOf()

    // An online map created from the portal item
    private val onlineMap = ArcGISMap(portalItem)

    // The current map shown in the map view
    var currentMap by mutableStateOf(onlineMap)

    // A flow of preplanned map areas and their download status
    private var _preplannedMapAreaInfoFlow = MutableStateFlow<List<PreplannedMapAreaInfo>>(listOf())
    var preplannedMapAreaInfoFlow = _preplannedMapAreaInfoFlow.asStateFlow()

    // Defined to send messages related to offlineMapJob
    val snackbarHostState = SnackbarHostState()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        with(viewModelScope) {
            launch(Dispatchers.IO) {
                offlineMapTask.getPreplannedMapAreas().onSuccess {
                    // Keep a list of all preplanned map areas
                    preplannedMapAreas.addAll(it)

                    // Add all of the preplanned map areas name and download status to a list
                    it.forEach { preplannedMapArea ->
                        _preplannedMapAreaInfoFlow.value += PreplannedMapAreaInfo(
                            name = preplannedMapArea.portalItem.title, progress = 0f, isDownloaded = false
                        )
                    }
                }
            }

            launch(Dispatchers.Main) {
                onlineMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }
        }
    }

    /**
     * Show the original map from the portal item.
     */
    fun showOnlineMap() {
        currentMap = onlineMap
    }

    /**
     * Download or show the already downloaded preplanned map area.
     */
    fun downloadOrShowOfflineMap(preplannedMapAreaInfo: PreplannedMapAreaInfo) {
        if (preplannedMapAreaInfo.isDownloaded) {
            showOfflineMap(preplannedMapAreaInfo)
        } else {
            downloadOfflineMap(preplannedMapAreaInfo)
        }
    }

    /**
     * Show the offline map of the given preplanned map area name.
     */
    private fun showOfflineMap(preplannedMapAreaInfo: PreplannedMapAreaInfo) {
        downloadedMapAreas[preplannedMapAreaInfo.name]?.let { selectedArcGISMap ->
            currentMap = selectedArcGISMap
        }
    }

    /**
     * Use the [OfflineMapTask] to create [DownloadPreplannedOfflineMapParameters] for the given [PreplannedMapArea].
     * Then use the task to create a [DownloadPreplannedOfflineMapJob] to download the preplanned offline map.
     */
    private fun downloadOfflineMap(preplannedMapAreaInfo: PreplannedMapAreaInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the area of interest for the preplanned map area
            preplannedMapAreas.find { it.portalItem.title == preplannedMapAreaInfo.name }?.let { preplannedMapArea ->
                // Create default download parameters from the offline map task
                offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(preplannedMapArea).onSuccess {
                    // Set the update mode to receive no updates
                    it.updateMode = PreplannedUpdateMode.NoUpdates
                    // Define the path where the map will be saved
                    val downloadDirectoryPath = offlineMapPath + File.separator + preplannedMapAreaInfo.name
                    File(downloadDirectoryPath).mkdirs()
                    // Create a job to download the preplanned offline map
                    val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
                        parameters = it, downloadDirectoryPath = downloadDirectoryPath
                    )
                    runOfflineMapJob(downloadPreplannedOfflineMapJob, preplannedMapAreaInfo)
                }
            }
        }
    }

    /**
     * Starts the [GenerateOfflineMapJob], shows the progress dialog and displays the result offline map to the MapView.
     */
    private fun runOfflineMapJob(
        downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob,
        preplannedMapAreaInfo: PreplannedMapAreaInfo
    ) {
        with(viewModelScope) {
            // Create a flow-collection for the job's progress
            launch(Dispatchers.Main) {
                downloadPreplannedOfflineMapJob.progress.collect { progress ->
                    // Update the UI to show the map as downloaded by replacing entry in the flow's list
                    _preplannedMapAreaInfoFlow.value = _preplannedMapAreaInfoFlow.value.map { mapAreaInfoInFlow ->
                        when (mapAreaInfoInFlow.name) {
                            preplannedMapAreaInfo.name -> mapAreaInfoInFlow.copy(progress = progress.toFloat() / 100)
                            else -> mapAreaInfoInFlow
                        }
                    }
                }
            }
            // Start the job and handle the result
            launch(Dispatchers.IO) {
                // Start the job and wait for Job result
                downloadPreplannedOfflineMapJob.start()
                downloadPreplannedOfflineMapJob.result().onSuccess { downloadedMap ->
                    // Set the offline map result as the displayed
                    currentMap = downloadedMap.offlineMap
                    // Update the UI to show the map as downloaded by replacing entry in the flow's list
                    _preplannedMapAreaInfoFlow.value = _preplannedMapAreaInfoFlow.value.map {
                        when (it.name) {
                            preplannedMapAreaInfo.name -> it.copy(isDownloaded = true)
                            else -> it
                        }
                    }
                    // Add the downloaded map to the list of downloaded maps
                    downloadedMapAreas[preplannedMapAreaInfo.name] = downloadedMap.offlineMap
                    // Show user where map was locally saved
                    snackbarHostState.showSnackbar(message = "Map saved at: " + downloadPreplannedOfflineMapJob.downloadDirectoryPath)
                }.onFailure { messageDialogVM.showMessageDialog(it) }
            }
        }
    }
}

data class PreplannedMapAreaInfo(val name: String, val progress: Float, val isDownloaded: Boolean)
