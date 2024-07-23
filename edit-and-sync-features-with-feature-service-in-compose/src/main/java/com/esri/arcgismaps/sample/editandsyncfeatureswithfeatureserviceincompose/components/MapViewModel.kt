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

package com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureserviceincompose.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.arcgismaps.tasks.geodatabase.SyncDirection
import com.arcgismaps.tasks.geodatabase.SyncGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.SyncGeodatabaseParameters
import com.arcgismaps.tasks.geodatabase.SyncLayerOption
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureserviceincompose.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(private val application: Application) : AndroidViewModel(application) {
    var polygon: Polygon? = null
    val map = ArcGISMap(BasemapStyle.ArcGISStreets)
    var actionButtonText by mutableStateOf(application.getString(R.string.generate_button_text))
    val graphicsOverlay = GraphicsOverlay()
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()
    val showGenerateGeodatabaseJobProgressDialog = mutableStateOf(false)
    val generateGeodatabaseJobProgress = mutableIntStateOf(0)
    val showSyncGeodatabaseJobProgressDialog = mutableStateOf(false)
    val syncGeodatabaseJobProgress = mutableIntStateOf(0)
    val mapViewProxy = MapViewProxy()
    val geodatabaseSyncTask by lazy { GeodatabaseSyncTask(application.getString(R.string.feature_server_url)) }
    val isActionButtonEnabled = mutableStateOf(false)
    private val geodatabaseFilePath by lazy {
        application.getExternalFilesDir(null)?.path + File.separator + application.getString(R.string.geodatabase_file)
    }

    // a red boundary line showing the filtered map extents for the features
    private val boundarySymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.red,
        width = 5f
    )

    // current edit state to track when feature edits can be performed on the geodatabase
    private var geodatabaseEditState = GeodatabaseEditState.NOT_READY

    // list of selected features to edit
    private var selectedFeatures = mutableListOf<Feature>()

    // geodatabase instance that is loaded
    private var geodatabase: Geodatabase? = null

    private var generateGeodatabaseJob: GenerateGeodatabaseJob? = null
    private var syncGeodatabaseJob: SyncGeodatabaseJob? = null

    init {
        map.maxExtent = Envelope(
            -1.3641320770825155E7,
            4524676.716562641,
            -1.3617221998199359E7,
            4567228.901189857,
            spatialReference = SpatialReference.webMercator()
        )

        viewModelScope.launch {
            map.load().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Unable to load map",
                    description = it.message.toString()
                )
                return@launch
            }
            geodatabaseSyncTask.load().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to fetch geodatabase metadata",
                    description = it.message.toString()
                )
                return@launch
            }
            isActionButtonEnabled.value = true

        }
    }

    fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {
        event.mapPoint?.let { point ->
            when (geodatabaseEditState) {
                GeodatabaseEditState.NOT_READY -> {
                    // if not ready, show info message
                    messageDialogVM.showMessageDialog(
                        title = "Can't edit yet. The geodatabase hasn't been generated!"
                    )
                }

                GeodatabaseEditState.EDITING -> {
                    // if edits have been performed, move the selected features
                    moveSelectedFeatures(point, map)
                }

                GeodatabaseEditState.READY -> {
                    // if no edits are performed but geodatabase is ready, select the
                    // tapped features
                    selectFeatures(event.screenCoordinate, map)
                }
            }
        }
    }

    fun onClickActionButton() {
        when (geodatabaseEditState) {
            // if geodatabase hasn't been generated
            GeodatabaseEditState.NOT_READY -> {
                polygon?.extent?.let { generateGeodatabase(geodatabaseSyncTask, map, it) }
            }
            // cannot sync in the middle of edit
            GeodatabaseEditState.EDITING -> messageDialogVM.showMessageDialog("Features are currently being edited!")
            // if the edits have been completed
            GeodatabaseEditState.READY -> geodatabase?.let { syncGeodatabase(it) }
        }
    }

    private fun generateGeodatabase(
        geodatabaseSyncTask: GeodatabaseSyncTask,
        map: ArcGISMap,
        extents: Envelope
    ) {
        val boundary = Graphic(extents, boundarySymbol)
        graphicsOverlay.graphics.add(boundary)

        viewModelScope.launch {
            val defaultParameters =
                geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents).getOrElse {
                    messageDialogVM.showMessageDialog("Error creating geodatabase parameters")
                    return@launch
                }.apply {
                    // set the parameters to only create a replica of the Trees (0) layer
                    layerOptions.removeIf { layerOptions ->
                        layerOptions.layerId != 0L
                    }
                    returnAttachments = false
                }

            generateGeodatabaseJob = geodatabaseSyncTask.createGenerateGeodatabaseJob(
                defaultParameters,
                geodatabaseFilePath
            )

            generateGeodatabaseJob?.let { generateGeodatabaseJob ->

                showGenerateGeodatabaseJobProgressDialog.value = true
                // Create a flow-collection for the job's progress
                viewModelScope.launch {
                    generateGeodatabaseJob.progress.collect { progress ->
                        // Display the current job's progress value
                        generateGeodatabaseJobProgress.intValue = progress
                        // Log.i("Progress", "generateGeodatabaseJobProgress: ${generateGeodatabaseProgress.intValue}")
                    }
                }

                generateGeodatabaseJob.start()

                geodatabase = generateGeodatabaseJob.result().getOrElse {
                    // show an error and return if job failed
                    messageDialogVM.showMessageDialog("Error fetching geodatabase: ${it.message}")
                    // dismiss the dialog
                    showGenerateGeodatabaseJobProgressDialog.value = false
                    // clear any drawn boundary
                    graphicsOverlay.graphics.clear()
                    return@launch
                }

                geodatabase?.let {
                    loadGeodatabase(it, map)
                }
                showGenerateGeodatabaseJobProgressDialog.value = false
                actionButtonText =
                    getApplication<Application>().getString(R.string.sync_button_text)
                geodatabaseEditState = GeodatabaseEditState.READY
            }
        }
    }

    private suspend fun loadGeodatabase(geodatabase: Geodatabase, map: ArcGISMap) {
        geodatabase.load().onFailure {
            return messageDialogVM.showMessageDialog("Error loading geodatabase")
        }
        map.operationalLayers += geodatabase.featureTables.map { featureTable ->
            FeatureLayer.createWithFeatureTable(featureTable)
        }
    }

    private fun syncGeodatabase(geodatabase: Geodatabase) {

        val syncGeodatabaseParameters = SyncGeodatabaseParameters().apply {
            geodatabaseSyncDirection = SyncDirection.Bidirectional
            shouldRollbackOnFailure = false
        }

        syncGeodatabaseParameters.layerOptions += geodatabase.featureTables.map { featureTable ->
            val serviceLayerId = featureTable.serviceLayerId
            SyncLayerOption(serviceLayerId)
        }

        viewModelScope.launch {
            syncGeodatabaseJob = geodatabaseSyncTask.createSyncGeodatabaseJob(
                syncGeodatabaseParameters,
                geodatabase
            )

            syncGeodatabaseJob?.let { syncGeodatabaseJob ->

                showSyncGeodatabaseJobProgressDialog.value = true
                // Create a flow-collection for the job's progress
                viewModelScope.launch {
                    syncGeodatabaseJob.progress.collect { progress ->
                        // Display the current job's progress value
                        syncGeodatabaseJobProgress.intValue = progress
                        // Log.i("Progress", "generateGeodatabaseJobProgress: ${generateGeodatabaseProgress.intValue}")
                    }
                }

                syncGeodatabaseJob.start()
                syncGeodatabaseJob.result().onSuccess {
                    messageDialogVM.showMessageDialog("Sync Complete")
                }.onFailure {
                    messageDialogVM.showMessageDialog("Database did not sync correctly")
                }
                showSyncGeodatabaseJobProgressDialog.value = false
            }

        }
    }

    private fun selectFeatures(screenCoordinate: ScreenCoordinate, map: ArcGISMap) {
        geodatabaseEditState = GeodatabaseEditState.EDITING

        viewModelScope.launch {
            var featuresSelected = false
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                val identifyLayerResult = mapViewProxy.identify(
                    featureLayer,
                    screenCoordinate,
                    12.dp,
                    false
                ).getOrElse {
                    return@launch messageDialogVM.showMessageDialog(
                        "Unable to identify selected layer",
                        description = it.message.toString()
                    )
                }

                val identifiedFeatures = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                if (identifiedFeatures.isNotEmpty()) {
                    featureLayer.selectFeatures(identifiedFeatures)
                    selectedFeatures.addAll(identifiedFeatures)
                    featuresSelected = true
                }
            }
            if (!featuresSelected) {
                messageDialogVM.showMessageDialog("No features found at the tapped location!")
                geodatabaseEditState = GeodatabaseEditState.READY
            }
        }
    }

    private fun moveSelectedFeatures(point: Point, map: ArcGISMap) {
        viewModelScope.launch {
            selectedFeatures.forEach { feature ->
                feature.geometry = point
                feature.featureTable?.updateFeature(feature)
            }
            selectedFeatures.clear()
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                featureLayer.clearSelection()
            }
            geodatabaseEditState = GeodatabaseEditState.READY
        }
    }

    fun cancelGenerateGeodatabaseJob() {
        viewModelScope.launch {
            generateGeodatabaseJob?.cancel()
        }
    }

    fun cancelSyncGeodatabaseJob() {
        viewModelScope.launch {
            syncGeodatabaseJob?.cancel()
        }
    }

    /**
     * Enum state class to track editing of features
     */
    enum class GeodatabaseEditState {
        NOT_READY,  // geodatabase has not yet been generated
        EDITING,  // a feature is in the process of being moved
        READY, // the geodatabase is ready for synchronization or further edits
    }

}
