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

package com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice.components

import android.app.Application
import androidx.compose.material3.SnackbarHostState
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
import com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MapViewModel(private val application: Application) : AndroidViewModel(application) {

    // Create a red boundary line showing the filtered map extents for the features
    private val boundarySymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.red,
        width = 5f
    )

    // Create graphic overlay to add graphics
    val graphicsOverlay = GraphicsOverlay()

    // Create a ViewModel to handle dialog interactions.
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // Create a streets basemap layer
    val map = ArcGISMap(BasemapStyle.ArcGISStreets)

    // Create a MapViewProxy to handle MapView operations
    val mapViewProxy = MapViewProxy()

    // Keep track of action button text through a string state
    var actionButtonText by mutableStateOf(application.getString(R.string.generate_button_text))
        private set

    // Create a button to keep track of action button enablement status
    var isActionButtonEnabled by mutableStateOf(false)
        private set

    // Determinate GenerateGeodatabase job progress loading dialog visibility state
    var showGenerateGeodatabaseJobProgressDialog by mutableStateOf(false)
        private set

    // Determine GenerateGeodatabase job progress percentage
    var generateGeodatabaseJobProgress by mutableIntStateOf(0)
        private set

    // Determinate SyncGeodatabase job progress loading dialog visibility state
    var showSyncGeodatabaseJobProgressDialog by mutableStateOf(false)
        private set

    // Determine SyncGeodatabase job progress percentage
    var syncGeodatabaseJobProgress by mutableIntStateOf(0)
        private set

    // Create a local file path to the geodatabase
    private val geodatabaseFilePath by lazy {
        application.getExternalFilesDir(null)?.path + File.separator + application.getString(R.string.geodatabase_file)
    }

    // Create a geodatabase sync task with the feature service url
    // This feature service illustrates a collection schema for wildfire information in the san fransisco area
    private val geodatabaseSyncTask by lazy { GeodatabaseSyncTask(application.getString(R.string.feature_server_url)) }

    // Current edit state to track when feature edits can be performed on the geodatabase
    private var geodatabaseEditState = GeodatabaseEditState.NOT_READY

    // List of selected features to edit
    private var selectedFeatures = mutableListOf<Feature>()

    // Geodatabase instance that is loaded
    private var geodatabase: Geodatabase? = null

    // Job used to generate geodatabase
    private var generateGeodatabaseJob: GenerateGeodatabaseJob? = null

    // Job used to sync geodatabase
    private var syncGeodatabaseJob: SyncGeodatabaseJob? = null

    // Initialize polygon to null as starting value and emit its state changes
    var polygon: Polygon? = null

    // Create a SnackbarHostState to handle successful operation messages
    val snackbarHostState = SnackbarHostState()

    init {

        // Set the max map extents to that of the feature service representing san fransisco area
        map.maxExtent = Envelope(
            xMin = -1.3641320770825155E7,
            yMin = 4524676.716562641,
            xMax = -1.3617221998199359E7,
            yMax = 4567228.901189857,
            spatialReference = SpatialReference.webMercator()
        )

        viewModelScope.launch(Dispatchers.Main) {
            // If map load failed, show the error and return
            map.load().onFailure {
                return@launch messageDialogVM.showMessageDialog(
                    title = "Unable to load map",
                    description = it.message.toString()
                )
            }
            // If the metadata load fails, show the error and return
            geodatabaseSyncTask.load().onFailure {
                return@launch messageDialogVM.showMessageDialog(
                    title = "Failed to fetch geodatabase metadata",
                    description = it.message.toString()
                )
            }
            // Enable the sync button since the task is now loaded
            isActionButtonEnabled = true
        }
    }

    /**
     * Collect and handle map touch events
     */
    fun onSingleTapConfirmed(event: SingleTapConfirmedEvent) {
        // Receive the point in map coordinates where the user tapped,
        event.mapPoint?.let { point ->
            // Perform an action based on the current edit state
            when (geodatabaseEditState) {
                GeodatabaseEditState.NOT_READY -> {
                    // If not ready, show info message
                    messageDialogVM.showMessageDialog("Can't edit yet. The geodatabase hasn't been generated!")
                }

                GeodatabaseEditState.EDITING -> {
                    // If edits have been performed, move the selected features
                    moveSelectedFeatures(point)
                }

                GeodatabaseEditState.READY -> {
                    // If no edits are performed but geodatabase is ready, select the tapped features
                    selectFeatures(screenCoordinate = event.screenCoordinate)
                }
            }
        }
    }

    /**
     * Handle button click events
     */
    fun onClickActionButton() {
        when (geodatabaseEditState) {
            // If geodatabase hasn't been generated
            GeodatabaseEditState.NOT_READY -> {
                polygon?.extent?.let { polygon -> generateGeodatabase(extents = polygon) }
            }

            // Cannot sync in the middle of edit
            GeodatabaseEditState.EDITING ->
                showSuccessMessage("Features are currently being edited!")

            // If the edits have been completed
            GeodatabaseEditState.READY -> syncGeodatabase()
        }
    }

    /**
     * Starts a [GeodatabaseSyncTask] with the given [ArcGISMap] and [Envelope.extent],
     * runs a GenerateGeodatabaseJob and saves the geodatabase file into local storage
     */
    private fun generateGeodatabase(extents: Envelope) {
        // Create a boundary representing the extents selected
        val boundary = Graphic(
            geometry = extents,
            symbol = boundarySymbol
        )

        // Add this boundary to the graphics overlay
        graphicsOverlay.graphics.add(boundary)

        with(viewModelScope) {
            launch(Dispatchers.IO) {
                // Create generateGeodatabase parameters for the selected extents
                val defaultParameters =
                    geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents)
                        .getOrElse {
                            // Show the error and return if the sync task fails
                            return@launch messageDialogVM.showMessageDialog(title = "Error creating geodatabase parameters")
                        }.apply {
                            // Set return attachments option to false
                            // Indicates if any attachments are added to the geodatabase from the feature service
                            returnAttachments = false
                        }

                // Create a generateGeodatabase job
                generateGeodatabaseJob = geodatabaseSyncTask.createGenerateGeodatabaseJob(
                    parameters = defaultParameters,
                    pathToGeodatabaseFile = geodatabaseFilePath
                )

                generateGeodatabaseJob?.let { generateGeodatabaseJob ->

                    // Show the dialog of generateGeodatabase Job
                    showGenerateGeodatabaseJobProgressDialog = true

                    // Create a flow-collection for the job's progress
                    launch(Dispatchers.Main) {
                        generateGeodatabaseJob.progress.collect { progress ->
                            // Display the current job's progress value
                            generateGeodatabaseJobProgress = progress
                        }
                    }

                    // Start the job
                    generateGeodatabaseJob.start()

                    // If the job completed successfully, get the geodatabase from the result
                    geodatabase = generateGeodatabaseJob.result().getOrElse {
                        // Show an error and return if job failed
                        messageDialogVM.showMessageDialog("Error fetching geodatabase: ${it.message}")
                        // Dismiss the dialog
                        showGenerateGeodatabaseJobProgressDialog = false
                        // Clear any drawn boundary
                        graphicsOverlay.graphics.clear()

                        return@launch
                    }

                    // Load and display the geodatabase
                    loadGeodatabase()

                    // Dismiss the dialog
                    showGenerateGeodatabaseJobProgressDialog = false
                }
            }
        }
    }

    /**
     * Loads the [Geodatabase] and renders the feature layers on to the [ArcGISMap]
     */
    private suspend fun loadGeodatabase() {
        // Load the geodatabase
        geodatabase?.let { geodatabase ->
            geodatabase.load().onFailure {
                // If the load failed, show the error and return
                return messageDialogVM.showMessageDialog("Error loading geodatabase")
            }

            // Add all of the geodatabase feature tables to the map as feature layers
            map.operationalLayers += geodatabase.featureTables.map { featureTable ->
                FeatureLayer.createWithFeatureTable(featureTable)
            }
        }

        // Update the sync button text to show a sync action
        actionButtonText = getApplication<Application>().getString(R.string.sync_button_text)

        // Update the geodatabase edit state to indicate its ready for edits and syncs
        geodatabaseEditState = GeodatabaseEditState.READY

    }

    /**
     * Syncs changes made on either the local [Geodatabase] or web service geodatabase with each other
     */
    private fun syncGeodatabase() {
        // Create parameters for the geodatabase sync task
        val syncGeodatabaseParameters = SyncGeodatabaseParameters().apply {
            geodatabaseSyncDirection = SyncDirection.Bidirectional
            shouldRollbackOnFailure = false
        }

        geodatabase?.let { geodatabase ->
            // Set synchronization option for each layer in the geodatabase we want to synchronize
            syncGeodatabaseParameters.layerOptions +=
                geodatabase.featureTables.map { featureTable ->
                    // Create a new sync layer option with the layer id of the feature table
                    SyncLayerOption(featureTable.serviceLayerId)
                }

            with(viewModelScope) {
                // Create the SyncGeodatabaseJob using the parameters and the geodatabase
                launch(Dispatchers.IO) {
                    syncGeodatabaseJob = geodatabaseSyncTask.createSyncGeodatabaseJob(
                        parameters = syncGeodatabaseParameters, geodatabase = geodatabase
                    )

                    syncGeodatabaseJob?.let { syncGeodatabaseJob ->

                        // Show the dialog
                        showSyncGeodatabaseJobProgressDialog = true

                        // Create a flow-collection for the job's progress
                        launch(Dispatchers.Main) {
                            syncGeodatabaseJob.progress.collect { progress ->
                                // Display the current job's progress value
                                syncGeodatabaseJobProgress = progress
                            }
                        }
                        // Start the job and wait for Job result
                        syncGeodatabaseJob.start()
                        syncGeodatabaseJob.result().onSuccess {
                            showSuccessMessage("Sync Complete")
                        }
                            .onFailure { messageDialogVM.showMessageDialog("Database did not sync correctly") }
                    }

                    // Dismiss the dialog
                    showSyncGeodatabaseJobProgressDialog = false

                    // Set the edit state to indicate geodatabase is ready for edits
                    geodatabaseEditState = GeodatabaseEditState.READY
                }
            }
        }
    }

    /**
     * Queries and selects features on FeatureLayers at the tapped [ScreenCoordinate] on the [ArcGISMap]
     */
    private fun selectFeatures(screenCoordinate: ScreenCoordinate) {
        // Set the current edit state to editing
        geodatabaseEditState = GeodatabaseEditState.EDITING

        // Create a new coroutine to handle the selection
        viewModelScope.launch(Dispatchers.Main) {
            // Flag to indicate if any features were selected
            var featuresSelected = false

            // For each feature layer in the map
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->

                // Identify the layer at the tapped screenCoordinate
                val identifyLayerResult = mapViewProxy.identify(
                    layer = featureLayer,
                    screenCoordinate = screenCoordinate,
                    tolerance = 12.dp,
                    returnPopupsOnly = false
                ).getOrElse {
                    // Show an error and return if the identifyLayer operation failed
                    return@launch messageDialogVM.showMessageDialog(
                        title = "Unable to identify selected layer",
                        description = it.message.toString()
                    )
                }

                // Get the identified features in the feature layer
                val identifiedFeatures = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                if (identifiedFeatures.isNotEmpty()) {
                    // Select the features on the map
                    featureLayer.selectFeatures(identifiedFeatures)
                    // Add the identified features to the selectedFeatures list
                    selectedFeatures.addAll(identifiedFeatures)
                    // Set the flag to true
                    featuresSelected = true
                }
            }

            // If no features were selected
            if (!featuresSelected) {
                // Show a message
                showSuccessMessage("No features found at the tapped location!")

                // Reset the current edit state to ready
                geodatabaseEditState = GeodatabaseEditState.READY
            }
        }
    }

    /**
     * Moves the selected features to a new [Point] on the [ArcGISMap]
     */
    private fun moveSelectedFeatures(point: Point) {
        // Create a new coroutine to move the features
        selectedFeatures.forEach { feature ->
            // Update each selected features geometry
            feature.geometry = point

            // Update the feature
            viewModelScope.launch(Dispatchers.IO) {
                feature.featureTable?.updateFeature(feature)
            }
        }

        // Clear the list of selected features once all have been updated
        selectedFeatures.clear()

        // Clear any selected features on the map
        map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
            featureLayer.clearSelection()
        }

        // Set the current edit state to ready
        geodatabaseEditState = GeodatabaseEditState.READY
    }

    fun cancelGenerateGeodatabaseJob() {
        viewModelScope.launch(Dispatchers.IO) {
            generateGeodatabaseJob?.cancel()
        }
    }

    fun cancelSyncGeodatabaseJob() {
        viewModelScope.launch(Dispatchers.IO) {
            syncGeodatabaseJob?.cancel()
        }
    }

    private fun showSuccessMessage(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            snackbarHostState.showSnackbar(message)
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
