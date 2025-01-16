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

package com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.components

import android.app.Application
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val FEATURE_SERVICE_URL =
    "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Mobile_Data_Collection_WFL1/FeatureServer"

class GenerateGeodatabaseReplicaFromFeatureServiceViewModel(
    private val application: Application
) : AndroidViewModel(application) {
    // graphics overlay to display the download area
    val graphicsOverlay = GraphicsOverlay()

    // symbol used to show a box around the extent we want to download
    private val downloadArea: Graphic = Graphic(
        symbol = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = com.arcgismaps.Color.red,
            width = 2F
        )
    )

    // a Trees FeatureLayer, using the first layer of the ServiceFeatureTable
    private val featureLayer: FeatureLayer by lazy {
        FeatureLayer.createWithFeatureTable(
            featureTable = ServiceFeatureTable(
                uri = "$FEATURE_SERVICE_URL/0"
            )
        )
    }

    // create a MapViewProxy, used to convert screen points to map points
    val mapViewProxy = MapViewProxy()

    // the dimensions of the MapView
    private var mapViewSize = IntSize(0, 0)

    // create a map with a Topographic basemap style
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        // set the max extent to that of the feature service representing an area of Portland
        maxExtent = Envelope(
            -13687689.2185849,
            5687273.88331375,
            -13622795.3756647,
            5727520.22085841,
            spatialReference = SpatialReference.webMercator()
        )

        // add the FeatureLayer to the map
        operationalLayers.add(featureLayer)
    }

    // a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // state flow to expose the current UI state
    private val _uiStateFlow = MutableStateFlow(UiState(appStatus = AppStatus.STARTING))
    val uiStateFlow = _uiStateFlow.asStateFlow()

    // create a GeodatabaseSyncTask with the URL of the feature service
    private var geodatabaseSyncTask = GeodatabaseSyncTask(FEATURE_SERVICE_URL)

    // job used to generate the geodatabase replica
    private var generateGeodatabaseJob: GenerateGeodatabaseJob? = null

    // the geodatabase replica
    private var geodatabase: Geodatabase? = null

    init {
        // add the download graphic to the graphics overlay
        graphicsOverlay.graphics.add(downloadArea)

        viewModelScope.launch {
            // load the map
            arcGISMap.load().onSuccess {
                // load the GeodatabaseSyncTask
                geodatabaseSyncTask.load().onSuccess {
                    _uiStateFlow.value = UiState(appStatus = AppStatus.READY_TO_GENERATE)
                }.onFailure { error ->
                    messageDialogVM.showMessageDialog(
                        title = "Failed to load GeodatabaseSyncTask",
                        description = error.message.toString()
                    )
                }
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }
        }
    }

    /**
     * Function called when the map view size is known.
     */
    fun updateMapViewSize(size: IntSize) {
        mapViewSize = size
    }

    /**
     * Use map view's size to determine dimensions of the area to download.
     */
    fun calculateDownloadArea() {
        // upper left corner of the area to take offline
        val minScreenPoint = ScreenCoordinate(200.0, 200.0)

        // lower right corner of the downloaded area
        val maxScreenPoint = ScreenCoordinate(
            x = mapViewSize.width - 200.0,
            y = mapViewSize.height - 200.0
        )

        // convert screen points to map points
        val minPoint = mapViewProxy.screenToLocationOrNull(minScreenPoint)
        val maxPoint = mapViewProxy.screenToLocationOrNull(maxScreenPoint)

        // set the download area's geometry using the calculated bounds
        if (minPoint != null && maxPoint != null) {
            val envelope = Envelope(minPoint, maxPoint)
            downloadArea.geometry = envelope
        }
    }

    /**
     * Reset the map to its original state.
     */
    fun resetMap() {
        // clear any layers and symbols already on the map
        arcGISMap.operationalLayers.clear()
        graphicsOverlay.graphics.clear()
        // add the download area boundary
        graphicsOverlay.graphics.add(downloadArea)
        // add back the feature layer
        arcGISMap.operationalLayers.add(featureLayer)
        // close the current geodatabase, if a replica was already generated
        geodatabase?.close()
        _uiStateFlow.value = UiState(appStatus = AppStatus.READY_TO_GENERATE)
    }

    /**
     * Generate the geodatabase replica.
     */
    fun generateGeodatabaseReplica() {
        _uiStateFlow.value = UiState(appStatus = AppStatus.GENERATING, jobProgress = 0)

        val offlineGeodatabasePath =
            application.getExternalFilesDir(null)?.path + "/portland_trees_gdb.geodatabase"

        // delete any offline geodatabase already in the cache
        File(offlineGeodatabasePath).deleteRecursively()

        // get the geometry of the download area
        val geometry = downloadArea.geometry ?: return messageDialogVM.showMessageDialog(
            title = "Could not get geometry of the downloadArea"
        )

        viewModelScope.launch(Dispatchers.Main) {
            // create GenerateGeodatabaseParameters for the selected extent
            val parameters =
                geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(geometry).getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error creating geodatabase parameters",
                        description = it.message.toString()
                    )
                    return@launch
                }.apply {
                    // modify the parameters to only include the Trees (0) layer
                    layerOptions.removeIf { layerOptions ->
                        layerOptions.layerId != 0L
                    }
                }

            // we don't need attachments
            parameters.returnAttachments = false

            // create a GenerateGeodatabaseJob
            val job = geodatabaseSyncTask.createGenerateGeodatabaseJob(
                parameters = parameters,
                pathToGeodatabaseFile = offlineGeodatabasePath
            )

            // stash the job so the cancel function can use it
            generateGeodatabaseJob = job

            // run the job
            runGenerateGeodatabaseJob(job)
        }
    }

    /**
     * Run the [job], showing the progress dialog and displaying the resultant data on the map.
     */
    private suspend fun runGenerateGeodatabaseJob(job: GenerateGeodatabaseJob) {
        // create a flow-collection for the job's progress
        viewModelScope.launch(Dispatchers.Main) {
            job.progress.collect { progress ->
                _uiStateFlow.value = UiState(appStatus = AppStatus.GENERATING, jobProgress = progress)
            }
        }

        // start the job and wait for Job result
        job.start()
        job.result().onSuccess { geodatabase ->
            // display the data
            loadGeodatabaseAndAddToMap(geodatabase)

            // unregister the geodatabase since we will not sync changes to the service
            geodatabaseSyncTask.unregisterGeodatabase(geodatabase).getOrElse {
                messageDialogVM.showMessageDialog(
                    title = "Failed to unregister the geodatabase",
                    description = it.message.toString()
                )
            }
        }.onFailure { error ->
            _uiStateFlow.value = UiState(appStatus = AppStatus.READY_TO_GENERATE)
            messageDialogVM.showMessageDialog(
                title = "Error generating geodatabase",
                description = error.message.toString()
            )
        }
    }

    /**
     * Loads the [replicaGeodatabase] and renders the feature layers on to the map.
     */
    private suspend fun loadGeodatabaseAndAddToMap(replicaGeodatabase: Geodatabase) {
        // clear any layers and symbols already on the map
        arcGISMap.operationalLayers.clear()
        graphicsOverlay.graphics.clear()

        // load the geodatabase
        replicaGeodatabase.load().onSuccess {
            // add all the geodatabase feature tables to the map as feature layers
            arcGISMap.operationalLayers += replicaGeodatabase.featureTables.map { featureTable ->
                FeatureLayer.createWithFeatureTable(featureTable)
            }
            // keep track of the geodatabase to close it before generating a new replica
            geodatabase = replicaGeodatabase
            _uiStateFlow.value = UiState(appStatus = AppStatus.REPLICA_DISPLAYED)
        }.onFailure { error ->
            _uiStateFlow.value = UiState(appStatus = AppStatus.READY_TO_GENERATE)
            messageDialogVM.showMessageDialog(
                title = "Error loading geodatabase",
                description = error.message.toString()
            )
        }
    }

    /**
     * Cancel the current [generateGeodatabaseJob].
     */
    fun cancelOfflineGeodatabaseJob() {
        viewModelScope.launch(Dispatchers.IO) {
            generateGeodatabaseJob?.cancel()
        }
        _uiStateFlow.value = UiState(appStatus = AppStatus.READY_TO_GENERATE)
    }

    override fun onCleared() {
        super.onCleared()
        // close the current geodatabase, if any, to release internal resources and file locks
        geodatabase?.close()
    }
}

/**
 * Data class representing the UI state.
 */
data class UiState(
    val appStatus: AppStatus,
    val jobProgress: Int = 0
)

enum class AppStatus {
    STARTING,
    READY_TO_GENERATE,
    GENERATING,
    REPLICA_DISPLAYED
}
