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

package com.esri.arcgismaps.sample.generateofflinemapwithcustomparameters.components

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseParameters
import com.arcgismaps.tasks.geodatabase.GenerateLayerQueryOption
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameterOverrides
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameters
import com.arcgismaps.tasks.offlinemaptask.OfflineMapParametersKey
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.tilecache.ExportTileCacheParameters
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class GenerateOfflineMapWithCustomParametersViewModel(private val application: Application) :
    AndroidViewModel(application) {

    val mapViewProxy = MapViewProxy()

    // view model to handle popup dialogs
    val messageDialogVM = MessageDialogViewModel()

    // create a portal item with the itemId of the web map
    var portal: Portal = Portal("https://www.arcgis.com")
    var portalItem: PortalItem = PortalItem(portal, "acc027394bc84c2fb04d1ed317aac674")

    // create a map with the portal item
    var arcGISMap: ArcGISMap = ArcGISMap(portalItem)

    // define the download area graphic
    private val downloadAreaGraphic = Graphic().apply {
        symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2f)
    }

    // create a graphics overlay for the map view
    val graphicsOverlay = GraphicsOverlay().apply {
        graphics.add(downloadAreaGraphic)
    }

    // Defined to send messages related to offlineMapJob
    val snackbarHostState = SnackbarHostState()

    // Determinate job progress loading dialog visibility state
    var showJobProgressDialog by mutableStateOf(false)
        private set

    // Determinate job progress percentage
    var offlineMapJobProgress by mutableIntStateOf(0)
        private set

    private var generateOfflineMapJob: GenerateOfflineMapJob? = null

    // Create an IntSize to retrieve dimensions of the map
    private var mapViewSize by mutableStateOf(IntSize(0, 0))

    fun updateMapViewSize(size: IntSize) {
        mapViewSize = size
    }

    /**
     * Use map view's size to determine dimensions of the map to get the download offline area
     * and use [MapViewProxy] to assist in converting screen points to map points
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
        // Create an envelope to set the download area's geometry using the defined bounds
        if (minPoint != null && maxPoint != null) {
            val envelope = Envelope(minPoint, maxPoint)
            downloadAreaGraphic.geometry = envelope
        }
    }

    fun defineParameters(
        minScale: Int,
        maxScale: Int,
        bufferDistance: Int,
        includeSystemValves: Boolean,
        includeServiceConnections: Boolean,
        flowRate: Int,
        cropWaterPipes: Boolean
    ) {
        // Create an offline map offlineMapTask with the map
        val offlineMapTask = OfflineMapTask(arcGISMap)
        downloadAreaGraphic.geometry?.let { downloadArea ->
            viewModelScope.launch {
                // Create default generate offline map parameters from the offline map task
                offlineMapTask.createDefaultGenerateOfflineMapParameters(areaOfInterest = downloadArea)
                    .onSuccess { generateOfflineMapParameters ->
                        // dDon't let generate offline map parameters continue on errors (including canceling during authentication)
                        generateOfflineMapParameters.continueOnErrors = false
                        // create parameter overrides for greater control
                        offlineMapTask.createGenerateOfflineMapParameterOverrides(generateOfflineMapParameters)
                            .onSuccess { parameterOverrides ->
                                // set basemap scale and area of interest
                                setBasemapScaleAndAreaOfInterest(parameterOverrides, minScale, maxScale, bufferDistance)
                                // exclude system valve layer
                                if (!includeSystemValves) {
                                    excludeLayerFromDownload(parameterOverrides, "System Valve")
                                }
                                // exclude service connection layer
                                if (!includeServiceConnections) {
                                    excludeLayerFromDownload(parameterOverrides, "Service Connection")
                                }
                                // crop pipes layer
                                if (cropWaterPipes) {
                                    getGenerateGeodatabaseParameters(parameterOverrides, "Main")?.layerOptions?.forEach {
                                        it.useGeometry = true
                                    }
                                }
                                // Get a reference to the hydrant layer
                                (arcGISMap.operationalLayers.find { it.name == "Hydrant" } as? FeatureLayer)?.let { hydrantLayer ->
                                    // Get it's service layer id
                                    val serviceLayerId =
                                        (hydrantLayer.featureTable as? ServiceFeatureTable)?.layerInfo?.serviceLayerId
                                    getGenerateGeodatabaseParameters(
                                        parameterOverrides,
                                        hydrantLayer.name
                                    )?.layerOptions?.filter { it.layerId == serviceLayerId }?.forEach {
                                        it.whereClause = "FLOW >= $flowRate"
                                        it.queryOption = GenerateLayerQueryOption.UseFilter
                                    }
                                }
                                // start a an offline map job from the task and parameters
                                createOfflineMapJob(offlineMapTask, generateOfflineMapParameters, parameterOverrides)
                            }
                    }
            }
        }
    }

    /**
     * Generate an offline map job with the given [OfflineMapTask], [GenerateOfflineMapParameters] and
     * [GenerateOfflineMapParameterOverrides].
     */
    private fun createOfflineMapJob(
        offlineMapTask: OfflineMapTask,
        generateOfflineMapParameters: GenerateOfflineMapParameters,
        parameterOverrides: GenerateOfflineMapParameterOverrides
    ) {
        // Store the offline map in the app's scoped storage directory
        val offlineMapPath =
            application.getExternalFilesDir(null)?.path + File.separator + "offlineMap"

        // Delete any offline map already present
        File(offlineMapPath).deleteRecursively()

        // Report any errors that occur during the offline map job
        viewModelScope.launch(Dispatchers.Main) {
            offlineMapTask.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = error.message.toString(),
                    description = error.cause.toString()
                )
            }
        }

        // Create an offline map job with the download directory path and parameters and start the job
        generateOfflineMapJob = offlineMapTask.createGenerateOfflineMapJob(
            parameters = generateOfflineMapParameters,
            downloadDirectoryPath = offlineMapPath,
            overrides = parameterOverrides
        )

        runOfflineMapJob()

    }

    /**
     * Starts the [GenerateOfflineMapJob], shows the progress dialog and displays the result offline map to the MapView.
     */
    private fun runOfflineMapJob() {
        generateOfflineMapJob?.let { offlineMapJob ->
            // Show the Job Progress Dialog
            showJobProgressDialog = true
            with(viewModelScope) {
                // Create a flow-collection for the job's progress
                launch(Dispatchers.Main) {
                    offlineMapJob.progress.collect { progress ->
                        // Display the current job's progress value
                        offlineMapJobProgress = progress
                    }
                }
                launch(Dispatchers.IO) {
                    // Start the job and wait for Job result
                    offlineMapJob.start()
                    offlineMapJob.result().onSuccess {
                        // Set the offline map result as the displayed map and clear the red bounding box graphic
                        arcGISMap = it.offlineMap
                        graphicsOverlay.graphics.clear()
                        // Dismiss the progress dialog
                        showJobProgressDialog = false
                        // Show user where map was locally saved
                        snackbarHostState.showSnackbar(message = "Map saved at: " + offlineMapJob.downloadDirectoryPath)
                    }.onFailure { throwable ->
                        messageDialogVM.showMessageDialog(
                            title = throwable.message.toString(),
                            description = throwable.cause.toString()
                        )
                        showJobProgressDialog = false
                    }
                }
            }
        }
    }

    /**
     * Cancel the offline map job.
     */
    fun cancelOfflineMapJob() {
        with(viewModelScope) {
            launch(Dispatchers.IO) {
                generateOfflineMapJob?.cancel()
            }
            launch(Dispatchers.Main) {
                snackbarHostState.showSnackbar(message = "User canceled.")
            }
        }
    }

    /**
     * Set basemap scale and area of interest using the given values
     */
    private fun setBasemapScaleAndAreaOfInterest(
        parameterOverrides: GenerateOfflineMapParameterOverrides, minScale: Int, maxScale: Int, bufferDistance: Int
    ) {
        (arcGISMap.basemap.value?.baseLayers?.first())?.let { basemapLayer ->
            // get the export tile cache parameters
            getExportTileCacheParameters(parameterOverrides, basemapLayer as? FeatureLayer)?.let { exportTileCacheParameters ->
                // create a new sublist of LODs in the range requested by the user
                exportTileCacheParameters.levelIds.clear()
                (minScale until maxScale).forEach { i ->
                    exportTileCacheParameters.levelIds.add(i)
                }
                downloadAreaGraphic.geometry?.let { downloadArea ->
                    // set the area of interest to the original download area plus a buffer
                    exportTileCacheParameters.areaOfInterest =
                        GeometryEngine.bufferOrNull(downloadArea, bufferDistance.toDouble())
                }
            }
        }
    }

    /**
     * Remove the layer named from the generate layer options list in the generate geodatabase parameters.
     */
    private fun excludeLayerFromDownload(parameterOverrides: GenerateOfflineMapParameterOverrides, layerName: String) {
        // get the named layer as a feature layer
        (arcGISMap.operationalLayers.find { it.name == layerName } as? FeatureLayer)?.let { targetFeatureLayer ->
            // get the layer's id
            val targetLayerId = getServiceLayerId(targetFeatureLayer)
            // get the layer's layer options
            getGenerateGeodatabaseParameters(parameterOverrides, layerName)?.layerOptions?.let { layerOptions ->
                // remove the target layer
                layerOptions.remove(layerOptions.find { it.layerId == targetLayerId })
            }
        }
    }

    /**
     * Helper function to get the service layer id for the given feature layer.
     */
    private fun getServiceLayerId(featureLayer: FeatureLayer): Long? {
        return (featureLayer.featureTable as? ServiceFeatureTable)?.layerInfo?.serviceLayerId
    }

    /**
     * Helper function to get export tile cache parameters for the given layer.
     */
    private fun getExportTileCacheParameters(parameterOverrides: GenerateOfflineMapParameterOverrides, targetFeatureLayer: FeatureLayer?): ExportTileCacheParameters? {
        targetFeatureLayer?.let {
            val key = OfflineMapParametersKey(it)
            return parameterOverrides.exportTileCacheParameters[key]
        }
        return null
    }

    /**
     * Helper function to get the generate geodatabase parameters for the given layer.
     *
     */
    private fun getGenerateGeodatabaseParameters(
        parameterOverrides: GenerateOfflineMapParameterOverrides,
        layerName: String
    ): GenerateGeodatabaseParameters? {
        // get the named feature layer
        (arcGISMap.operationalLayers.find { it.name == layerName } as? FeatureLayer)?.let { targetFeatureLayer ->
            val key = OfflineMapParametersKey(targetFeatureLayer)
            // return the layer's geodatabase parameters options
            return parameterOverrides.generateGeodatabaseParameters[key]
        }
        return null
    }
}
