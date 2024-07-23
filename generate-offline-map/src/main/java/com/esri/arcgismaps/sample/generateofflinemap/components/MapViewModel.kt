/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.generateofflinemap.components

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameters
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.generateofflinemap.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(private val application: Application) : AndroidViewModel(application) {

    // Create a symbol to show a box around the extent we want to download
    private val downloadArea: Graphic = Graphic(
        symbol = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = com.arcgismaps.Color.red,
            width = 2F
        )
    )

    // Create graphic overlay to add graphics
    val graphicsOverlay = GraphicsOverlay()

    // Create a ViewModel to handle dialog interactions.
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // Define button to take a map offline
    var takeMapOfflineButtonText by mutableStateOf(application.getString(R.string.take_map_offline))

    // Defined to send messages related to offlineMapJob
    val snackbarHostState = SnackbarHostState()

    // Define map that returns an ArcGISMap
    var map by mutableStateOf(ArcGISMap())

    // Determinate job progress loading dialog visibility state
    val showJobProgressDialog = mutableStateOf(false)

    // Determinate job progress percentage
    val offlineMapJobProgress = mutableIntStateOf(0)

    // Job used to run the offlineMap task on a service
    private var offlineMapJob: GenerateOfflineMapJob? = null

    // Create a MapViewProxy to handle MapView operations
    val mapViewProxy = MapViewProxy()

    // Create an IntSize to retrieve dimensions of the map
    var mapViewSize by mutableStateOf(IntSize(0, 0))

    init {
        // Set up the portal item to  take offline
        setUpMap()
    }

    /**
     * Sets up a portal item and displays map area to take offline
     */
    private fun setUpMap() {

        // Create a portal item with the itemId of the web map
        val portal = Portal(application.getString(R.string.portal_url))
        val portalItem = PortalItem(portal, application.getString(R.string.item_id))

        map = ArcGISMap(portalItem)
        viewModelScope.launch {
            map.load()
                .onSuccess {
                    // Add the download graphic to the graphics overlay
                    graphicsOverlay.graphics.add(downloadArea)

                    // Limit the map scale to the largest layer scale
                    map.maxScale = map.operationalLayers[6].maxScale ?: 0.0
                    map.minScale = map.operationalLayers[6].minScale ?: 0.0
                }
                .onFailure {
                    messageDialogVM.showMessageDialog(
                        title = it.message.toString()
                    )
                }
        }
    }

    /**
     * Generate an offline map job with the default [GenerateOfflineMapParameters]
     *
     */
    fun createOfflineMapJob() {
        // Offline map path
        val offlineMapPath =
            application.getExternalFilesDir(null)?.path + File.separator + "offlineMap"

        // Delete any offline map already in the cache
        File(offlineMapPath).deleteRecursively()

        // Specify the extent, min scale, and max scale as parameters
        var minScale: Double = map.minScale ?: 0.0
        val maxScale: Double = map.maxScale ?: 0.0

        // Variable minScale must always be larger than maxScale
        if (minScale <= maxScale) {
            minScale = maxScale + 1
        }

        // Get the geometry of the downloadArea
        val geometry = downloadArea.geometry ?: return messageDialogVM.showMessageDialog(
            title = "Could not get geometry of the downloadArea"
        )

        // Set the offline map parameters
        val generateOfflineMapParameters = GenerateOfflineMapParameters(
            areaOfInterest = geometry,
            minScale = minScale,
            maxScale = maxScale
        ).apply {
            // Set job to cancel on any errors
            continueOnErrors = false
        }

        // Create an offline map task with the map
        val offlineMapTask = OfflineMapTask(onlineMap = map)

        viewModelScope.launch {
            offlineMapTask.load().getOrElse {
                messageDialogVM.showMessageDialog(
                    title = it.message.toString(),
                    description = it.cause.toString()
                )
            }
        }

        // Create an offline map job with the download directory path and parameters and start the job
        offlineMapJob = offlineMapTask.createGenerateOfflineMapJob(
            parameters = generateOfflineMapParameters,
            downloadDirectoryPath = offlineMapPath
        )

        runOfflineMapJob()

    }

    /**
     * Starts the [offlineMapJob], shows the progress dialog and
     * displays the result offline map to the MapView
     */
    private fun runOfflineMapJob() {

        offlineMapJob?.let { offlineMapJob ->

            // Show the Job Progress Dialog
            showJobProgressDialog.value = true

            with(viewModelScope) {

                // Create a flow-collection for the job's progress
                launch {
                    offlineMapJob.progress.collect { progress ->
                        // Display the current job's progress value
                        offlineMapJobProgress.intValue = progress
                    }
                }

                launch {
                    // Start the job and wait for Job result
                    offlineMapJob.start()
                    offlineMapJob.result().onSuccess {
                        // set the offline map result as the displayed map
                        map = it.offlineMap
                        graphicsOverlay.graphics.clear()

                        // Change the button text to Reset Map once job is done successfully
                        takeMapOfflineButtonText = application.getString(R.string.reset_map)

                        // Dismiss the progress dialog
                        showJobProgressDialog.value = false

                        // Show user where map was locally saved
                        snackbarHostState.showSnackbar(message = "Map saved at: " + offlineMapJob.downloadDirectoryPath)

                    }.onFailure { throwable ->
                        messageDialogVM.showMessageDialog(
                            title = throwable.message.toString(),
                            description = throwable.cause.toString()
                        )
                        showJobProgressDialog.value = false
                    }
                }
            }

        }
    }

    fun cancelOfflineMapJob() {
        viewModelScope.launch {
            offlineMapJob?.cancel()
            snackbarHostState.showSnackbar(message = "User canceled.")
        }
    }

    /**
     * Clear the preview map and set up mapView again
     */
    fun resetButtonClick() {

        // Add the download graphic to the graphics overlay
        graphicsOverlay.graphics.clear()

        // Change button text to Take Map Offline on reset
        takeMapOfflineButtonText = application.getString(R.string.take_map_offline)

        // Set up the portal item to take offline
        setUpMap()

    }

    /**
     * Use [mapViewSize] to determine dimensions of the map to get the download offline area
     * and use [mapViewProxy] to assist in converting screen points to map points
     */
    fun calculateDownloadOfflineArea() {
        // Upper left corner of the area to take offline
        val minScreenPoint = ScreenCoordinate(200.0, 200.0)

        // Lower right corner of the downloaded area
        val maxScreenPoint = ScreenCoordinate(
            x = mapViewSize.width - 200.0,
            y = mapViewSize.height - 200.0
        )

        // Convert screen points to map points
        val minPoint = mapViewProxy.screenToLocationOrNull(minScreenPoint)
        val maxPoint = mapViewProxy.screenToLocationOrNull(maxScreenPoint)

        // Use the points to define and return an envelope
        if (minPoint != null && maxPoint != null) {
            val envelope = Envelope(minPoint, maxPoint)
            downloadArea.geometry = envelope
        }
    }
}
