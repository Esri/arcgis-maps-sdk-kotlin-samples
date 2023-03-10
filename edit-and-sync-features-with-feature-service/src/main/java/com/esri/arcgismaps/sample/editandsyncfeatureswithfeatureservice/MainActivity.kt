/*
 * Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.Feature
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.geodatabase.SyncGeodatabaseParameters
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.arcgismaps.tasks.geodatabase.SyncLayerOption
import com.arcgismaps.tasks.geodatabase.SyncDirection
import com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice.databinding.EditAndSyncDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // button for the generate, sync actions
    private val actionButton by lazy {
        activityMainBinding.actionButton
    }

    // shows the progress of the generate, sync tasks
    private val progressDialog by lazy {
        EditAndSyncDialogLayoutBinding.inflate(layoutInflater)
    }

    // local file path to the geodatabase
    private val geodatabaseFilePath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.geodatabase_file)
    }

    // create a geodatabase sync task with the feature service url
    // This feature service illustrates a collection schema for wildlfire information
    // in the san fransisco area
    private val geodatabaseSyncTask by lazy {
        GeodatabaseSyncTask(getString(R.string.feature_server_url))
    }

    // a red boundary line showing the filtered map extents for the features
    private val boundarySymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

    // graphics overlay to draw graphics on
    private val graphicsOverlay = GraphicsOverlay()

    // current edit state to track when feature edits can be performed on the geodatabase
    private var geodatabaseEditState = GeodatabaseEditState.NOT_READY

    // list of selected features to edit
    private var selectedFeatures = mutableListOf<Feature>()

    // geodatabase instance that is loaded
    private lateinit var geodatabase: Geodatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView.keepScreenOn = true
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a Topographic basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISStreets)
        // set the max map extents to that of the feature service
        // representing san fransisco area
        map.maxExtent = Envelope(
            -1.3641320770825155E7,
            4524676.716562641,
            -1.3617221998199359E7,
            4567228.901189857,
            spatialReference = SpatialReference.webMercator()
        )

        mapView.apply {
            this.map = map
            // add the graphics overlay to display the boundary
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // if map load failed, show the error and return
            map.load().onFailure {
                showInfo("Unable to load map: ${it.message}")
                return@launch
            }
            // if the metadata load fails, show the error and return
            geodatabaseSyncTask.load().onFailure {
                showInfo("Failed to fetch geodatabase metadata: ${it.message}")
                return@launch
            }
            // enable the sync button since the task is now loaded
            actionButton.isEnabled = true
            // collect and handle map touch events
            mapView.onSingleTapConfirmed.collect { event ->
                event.mapPoint?.let { point ->
                    // perform an action based on the current edit state
                    when (geodatabaseEditState) {
                        GeodatabaseEditState.NOT_READY -> {
                            // if not ready, show info message
                            showInfo("Can't edit yet. The geodatabase hasn't been generated!")
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
        }

        // set the button's onClickListener
        actionButton.setOnClickListener {
            when (geodatabaseEditState) {
                // if geodatabase hasn't been generated
                GeodatabaseEditState.NOT_READY -> {
                    mapView.visibleArea?.let { polygon ->
                        // start the geodatabase generation process
                        generateGeodatabase(geodatabaseSyncTask, map, polygon.extent)
                    }
                }
                // cannot sync in the middle of edit
                GeodatabaseEditState.EDITING -> showInfo("Features are currently being edited!")
                // if the edits have been completed
                GeodatabaseEditState.READY -> syncGeodatabase(geodatabase)
            }
        }
    }

    /**
     * Starts a [geodatabaseSyncTask] with the given [map] and [extents],
     * runs a GenerateGeodatabaseJob and saves the geodatabase file into local storage
     */
    private fun generateGeodatabase(
        geodatabaseSyncTask: GeodatabaseSyncTask,
        map: ArcGISMap,
        extents: Envelope
    ) {
        // clear any layers already on the map
        map.operationalLayers.clear()
        // clear all symbols drawn
        graphicsOverlay.graphics.clear()
        // create a boundary representing the extents selected
        val boundary = Graphic(extents, boundarySymbol)
        // add this boundary to the graphics overlay
        graphicsOverlay.graphics.add(boundary)

        lifecycleScope.launch {
            // create generate geodatabase parameters for the selected extents
            val defaultParameters =
                geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents).getOrElse {
                    // show the error and return if the sync task fails
                    showInfo("Error creating geodatabase parameters")
                    return@launch
                }.apply {
                    // set return attachments option to false
                    // indicates if any attachments are added to the geodatabase from the feature service
                    returnAttachments = false
                }

            // create a generate geodatabase job
            val generateGeodatabaseJob = geodatabaseSyncTask.createGenerateGeodatabaseJob(
                defaultParameters,
                geodatabaseFilePath
            )
            // create a dialog to show the jobs progress
            val dialog = createProgressDialog("Generating geodatabase") {
                // set the onDismissListener to cancel the job when the dialog is dismissed
                launch {
                    generateGeodatabaseJob.cancel()
                }
            }
            // show the dialog
            dialog.show()
            // start the generateGeodatabase job
            generateGeodatabaseJob.start()
            // launch a progress collector to display progress
            launch {
                generateGeodatabaseJob.progress.collect { value ->
                    // update the progress bar and progress text
                    progressDialog.progressBar.progress = value
                    progressDialog.progressTextView.text = "$value%"
                }
            }
            // if the job completed successfully, get the geodatabase from the result
            geodatabase = generateGeodatabaseJob.result().getOrElse {
                // show an error and return if job failed
                showInfo("Error fetching geodatabase: ${it.message}")
                // dismiss the dialog
                dialog.dismiss()
                // clear any drawn boundary
                graphicsOverlay.graphics.clear()
                return@launch
            }
            // load and display the geodatabase
            loadGeodatabase(geodatabase, map)
            // dismiss the dialog
            dialog.dismiss()
        }
    }

    /**
     * Loads the [geodatabase] and renders the feature layers on to the [map]
     */
    private suspend fun loadGeodatabase(geodatabase: Geodatabase, map: ArcGISMap) {
        // load the geodatabase
        geodatabase.load().onFailure {
            // if the load failed, show the error and return
            showInfo("Error loading geodatabase")
            return
        }
        // add all of the geodatabase feature tables to the map as feature layers
        map.operationalLayers += geodatabase.featureTables.map { featureTable ->
            FeatureLayer(featureTable)
        }
        // update the sync button text to show a sync action
        actionButton.text = getString(R.string.sync_button_text)
        // update the geodatabase edit state to indicate its ready for edits and syncs
        geodatabaseEditState = GeodatabaseEditState.READY
    }

    /**
     * Syncs changes made on either the local [geodatabase] or web service geodatabase with each
     * other
     */
    private fun syncGeodatabase(geodatabase: Geodatabase) {
        // create parameters for the geodatabase sync task
        val syncGeodatabaseParameters = SyncGeodatabaseParameters().apply {
            // sync changes in either direction
            geodatabaseSyncDirection = SyncDirection.Bidirectional
            shouldRollbackOnFailure = false
        }

        // set synchronization option for each layer in the geodatabase we want to synchronize
        syncGeodatabaseParameters.layerOptions += geodatabase.featureTables.map { featureTable ->
            val serviceLayerId = featureTable.serviceLayerId
            // create a new sync layer option with the layer id of the feature table
            SyncLayerOption(serviceLayerId)
        }

        // create a new coroutine to run the SyncGeodatabaseJob
        lifecycleScope.launch {
            // create the SyncGeodatabaseJob using the parameters and the geodatabase
            val syncGeodatabaseJob =
                geodatabaseSyncTask.createSyncGeodatabaseJob(syncGeodatabaseParameters, geodatabase)

            // create a dialog to show the jobs progress
            val dialog = createProgressDialog("Syncing geodatabase") {
                // set the onDismissListener to cancel the job when the dialog is dismissed
                launch {
                    syncGeodatabaseJob.cancel()
                }
            }
            // show the dialog
            dialog.show()
            // start the sync geodatabase job
            syncGeodatabaseJob.start()
            // launch a progress collector to display progress
            launch {
                syncGeodatabaseJob.progress.collect { value ->
                    // update the progress bar and progress text
                    progressDialog.progressBar.progress = value
                    progressDialog.progressTextView.text = "$value%"
                }
            }
            // launch a job status collector
            launch {
                syncGeodatabaseJob.status.collect { status ->
                    when (status) {
                        JobStatus.Failed, JobStatus.Succeeded -> {
                            // if the job failed show an failed message
                            if (status is JobStatus.Failed) {
                                showInfo("Database did not sync correctly")
                            } else {
                                showInfo("Sync Complete")
                            }
                            // dismiss the dialog
                            dialog.dismiss()
                            // set the edit state to indicate geodatabase is ready for edits
                            geodatabaseEditState = GeodatabaseEditState.READY
                        }
                        else -> { /* don't have to handle other states */
                        }
                    }
                }
            }
        }
    }

    /**
     * Queries and selects features on FeatureLayers at the tapped [screenCoordinate] on the [map]
     */
    private fun selectFeatures(screenCoordinate: ScreenCoordinate, map: ArcGISMap) {
        // set the current edit state to editing
        geodatabaseEditState = GeodatabaseEditState.EDITING
        // create a new coroutine to handle the selection
        lifecycleScope.launch {
            // flag to indicate if any features were selected
            var featuresSelected = false
            // for each feature layer in the map
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                // identify the layer at the tapped screenCoordinate
                val identifyLayerResult = mapView.identifyLayer(
                    featureLayer,
                    screenCoordinate,
                    12.0,
                    false
                ).getOrElse {
                    // show an error and return if the identifyLayer operation failed
                    showInfo("Unable to identify selected layer: ${it.message}")
                    return@launch
                }
                // get the identified features in the feature layer
                val identifiedFeatures = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                if(identifiedFeatures.isNotEmpty()) {
                    // select the features on the map
                    featureLayer.selectFeatures(identifiedFeatures)
                    // add the identified features to the selectedFeatures list
                    selectedFeatures.addAll(identifiedFeatures)
                    // set the flag to true
                    featuresSelected = true
                }
            }
            // if no features were selected
            if (!featuresSelected) {
                // show a message
                showInfo("No features found at the tapped location!")
                // reset the current edit state to ready
                geodatabaseEditState = GeodatabaseEditState.READY
            }
        }
    }

    /**
     * Moves the selected features to a new [point] on the [map]
     */
    private fun moveSelectedFeatures(point: Point, map: ArcGISMap) {
        // create a new coroutine to move the features
        lifecycleScope.launch {
            selectedFeatures.forEach { feature ->
                // update each selected features geometry
                feature.geometry = point
                // update the feature
                feature.featureTable?.updateFeature(feature)
            }
            // clear the list of selected features once all have been updated
            selectedFeatures.clear()
            // clear any selected features on the map
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                featureLayer.clearSelection()
            }
            // set the current edit state to ready
            geodatabaseEditState = GeodatabaseEditState.READY
        }
    }


    /**
     * Creates and returns a new alert dialog using the progressDialog with the given [title] and
     * provides an [onDismissListener] callback when the dialog is dismissed
     */
    private fun createProgressDialog(title: String, onDismissListener: () -> Unit): AlertDialog {
        // build and return a new alert dialog
        return AlertDialog.Builder(this).apply {
            // setting it title
            setTitle(title)
            // allow it to be cancellable
            setCancelable(false)
            // sets negative button configuration
            setNegativeButton("Cancel") { _, _ ->
                // call the dismiss listener
                onDismissListener()
            }
            // removes parent of the progressDialog layout, if previously assigned
            progressDialog.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            // set the progressDialog Layout to this alert dialog
            setView(progressDialog.root)
        }.create()
    }

    private fun showInfo(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Enum state class to track editing of features
     */
    enum class GeodatabaseEditState {
        NOT_READY,  // geodatabase has not yet been generated
        EDITING,  // a feature is in the process of being moved
        READY // the geodatabase is ready for synchronization or further edits
    }
}
