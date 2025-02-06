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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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

    private val offlineMapDirectory by lazy {
        File(application.externalCacheDir?.path + application.getString(R.string.download_preplanned_map_area_app_name))
    }

    // create a portal to ArcGIS Online
    val portal = Portal("https://www.arcgis.com")

    // create a portal item using the portal and the item id of a map service
    val portalItem = PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674")

    val onlineMap = ArcGISMap(portalItem)

    var currentMap by mutableStateOf(onlineMap)

    val offlineMapTask = OfflineMapTask(portalItem)

    private var _preplannedMapAreaInfoList = mutableStateListOf<PreplannedMapAreaInfo>()
    val preplannedMapAreaInfoList: List<PreplannedMapAreaInfo> = _preplannedMapAreaInfoList

    var preplannedMapAreas = mutableListOf<PreplannedMapArea>()

    private var _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            offlineMapTask.getPreplannedMapAreas().onSuccess {
                // Keep a list of all preplanned map areas
                preplannedMapAreas.addAll(it)

                // Add all of the preplanned map areas name and download status to a list
                it.forEach { preplannedMapArea ->
                    _preplannedMapAreaInfoList.add(
                        PreplannedMapAreaInfo(
                            name = preplannedMapArea.portalItem.title
                        )
                    )
                }
            }
        }





        viewModelScope.launch {
            onlineMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map", error.message.toString()
                )
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
    fun downloadOrShowMap(preplannedMapAreaListItem: PreplannedMapAreaInfo) {
        if (preplannedMapAreaListItem.isDownloaded) {
            showOfflineMap(preplannedMapAreaListItem)
        } else {
            downloadOfflineMap(preplannedMapAreaListItem)
        }
    }

    /**
     * Show the offline map of the given preplanned map area name.
     */
    fun showOfflineMap(preplannedMapAreaInfo: PreplannedMapAreaInfo) {

    }

    private fun downloadOfflineMap(preplannedMapAreaInfo: PreplannedMapAreaInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the area of interest for the preplanned map area
            preplannedMapAreas.find { it.portalItem.title == preplannedMapAreaInfo.name }?.let { preplannedMapArea ->
                // Create default download parameters from the offline map task
                offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(preplannedMapArea).onSuccess {
                    // Set the update mode to receive no updates
                    it.updateMode = PreplannedUpdateMode.NoUpdates

                    val downloadDirectoryPath = offlineMapDirectory.path + File.separator + preplannedMapAreaInfo.name
                    File(downloadDirectoryPath).mkdirs()

                    // create a job to download the preplanned offline map to a temporary directory
                    val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
                        parameters = it, downloadDirectoryPath = downloadDirectoryPath
                    )

                    runOfflineMapJob(downloadPreplannedOfflineMapJob, preplannedMapAreaInfo)
                }
            }
        }

    }


    /**
     * Starts the [GenerateOfflineMapJob], shows the progress dialog and
     * displays the result offline map to the MapView
     */
    private fun runOfflineMapJob(
        downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob, preplannedMapAreaInfo: PreplannedMapAreaInfo
    ) {

        with(viewModelScope) {

            // Create a flow-collection for the job's progress
            launch(Dispatchers.Main) {
                downloadPreplannedOfflineMapJob.progress.collect { progress ->
                    val index = _preplannedMapAreaInfoList.indexOf(preplannedMapAreaInfo)
                    // Display the current job's progress value
                    _preplannedMapAreaInfoList.remove(preplannedMapAreaInfo)
                    _preplannedMapAreaInfoList.add(index, preplannedMapAreaInfo.apply {
                        this.progress = (progress / 100).toFloat()
                    })
                }
            }

            launch(Dispatchers.IO) {
                // Start the job and wait for Job result
                downloadPreplannedOfflineMapJob.start()
                downloadPreplannedOfflineMapJob.result().onSuccess {
                    // Set the offline map result as the displayed
                    currentMap = it.offlineMap

                    // Show user where map was locally saved
                    //snackbarHostState.showSnackbar(message = "Map saved at: " + offlineMapJob.downloadDirectoryPath)

                }.onFailure { throwable ->
                    messageDialogVM.showMessageDialog(
                        title = throwable.message.toString(), description = throwable.cause.toString()
                    )
                }
            }
        }

    }
}

class PreplannedMapAreaInfo(val name: String) {
    var progress by mutableFloatStateOf(0.0f)
    var isDownloaded by mutableStateOf(false)
}
