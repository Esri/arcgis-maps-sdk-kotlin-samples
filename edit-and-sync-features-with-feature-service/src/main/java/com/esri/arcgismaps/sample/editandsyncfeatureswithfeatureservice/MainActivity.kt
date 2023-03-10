/* Copyright 2022 Esri
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
import com.arcgismaps.tasks.Job
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.geodatabase.*
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

    private val syncButton by lazy {
        activityMainBinding.syncButton
    }

    private val progressDialog by lazy {
        EditAndSyncDialogLayoutBinding.inflate(layoutInflater)
    }

    // local file path to the geodatabase
    private val geodatabaseFilePath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.geodatabase_file)
    }

    // create a geodatabase sync task with the feature service url
    // This feature service shows a web map of portland street trees,
    // their attributes, as well as related inspection information
    private val geodatabaseSyncTask by lazy {
        GeodatabaseSyncTask("https://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/WildfireSync/FeatureServer")
        // GeodatabaseSyncTask(getString(R.string.feature_server_url))
    }

    // a red boundary line showing the filtered map extents for the features
    private val boundarySymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

    private val graphicsOverlay = GraphicsOverlay()

    private var geodatabaseEditState = GeodatabaseEditState.NOT_READY

    private var selectedFeatures = mutableListOf<Feature>()

    private lateinit var geodatabase: Geodatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView.keepScreenOn = true
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a Topographic basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the max map extents to that of the feature service
        // representing portland area
        map.maxExtent = Envelope(
            -1.3641320770825155E7,
            4524676.716562641,
            -1.3617221998199359E7,
            4567228.901189857,
            spatialReference = SpatialReference.webMercator()
        )
        // configure mapview assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to display the boundary
            graphicsOverlays.add(graphicsOverlay)

            lifecycleScope.launch {
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { point ->
                        when (geodatabaseEditState) {
                            GeodatabaseEditState.NOT_READY -> {
                                showInfo("Can't edit yet. The geodatabase hasn't been generated!")
                            }
                            GeodatabaseEditState.EDITING -> {
                                moveSelectedFeatures(point, map)
                            }
                            GeodatabaseEditState.READY -> {
                                selectFeatures(event.screenCoordinate, map)
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            // show the error and return if map load failed
            map.load().onFailure {
                showInfo("Unable to load map")
                return@launch
            }

            geodatabaseSyncTask.load().onFailure {
                // if the metadata load fails, show the error and return
                showInfo("Failed to fetch geodatabase metadata")
                return@launch
            }

            // enable the sync button since the task is now loaded
            syncButton.isEnabled = true


        }

        // set the button's onClickListener
        syncButton.setOnClickListener {
            when (geodatabaseEditState) {
                GeodatabaseEditState.NOT_READY -> {
                    mapView.visibleArea?.let { polygon ->
                        Log.e(TAG, "onCreate: ${polygon.extent}", )
                        // start the geodatabase generation process
                        generateGeodatabase(geodatabaseSyncTask, map, polygon.extent)
                    }
                }
                GeodatabaseEditState.EDITING -> showInfo("Unexpected edit state!")
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
                    returnAttachments = true
                }

            // create a generate geodatabase job
            geodatabaseSyncTask.createGenerateGeodatabaseJob(defaultParameters, geodatabaseFilePath)
                .run {
                    // create a dialog to show the jobs progress
                    val dialog = createProgressDialog("Generating geodatabase", this)
                    // show the dialog
                    dialog.show()
                    // start the generateGeodatabase job
                    start()
                    // launch a progress collector to display progress
                    launch {
                        progress.collect { value ->
                            // update the progress bar and progress text
                            progressDialog.progressBar.progress = value
                            progressDialog.progressTextView.text = "$value%"
                        }
                    }
                    // if the job completed successfully, get the geodatabase from the result
                    geodatabase = result().getOrElse {
                        // show an error and return if job failed
                        showInfo("Error fetching geodatabase: ${it.message}")
                        // dismiss the dialog
                        dialog.dismiss()
                        return@launch
                    }

                    // load and display the geodatabase
                    loadGeodatabase(geodatabase, map)
                    // dismiss the dialog view
                    dialog.dismiss()
                }
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
        syncButton.text = getString(R.string.sync_button_text)
        // update the geodatabase edit state
        geodatabaseEditState = GeodatabaseEditState.READY
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

        lifecycleScope.launch {
            geodatabaseSyncTask.createSyncGeodatabaseJob(syncGeodatabaseParameters, geodatabase)
                .run {
                    // create a dialog to show the jobs progress
                    val dialog = createProgressDialog("Syncing geodatabase", this)
                    // show the dialog
                    dialog.show()
                    // start the sync geodatabase job
                    start()
                    // launch a progress collector to display progress
                    launch {
                        progress.collect { value ->
                            // update the progress bar and progress text
                            progressDialog.progressBar.progress = value
                            progressDialog.progressTextView.text = "$value%"
                        }
                    }

                    // if the job failed show an error and return
                    result().onFailure {
                        showInfo("Database did not sync correctly: ${it.message}")
                        return@onFailure
                    }

                    // dismiss the dialog
                    dialog.dismiss()
                    syncButton.isEnabled = false
                    geodatabaseEditState = GeodatabaseEditState.READY
                    showInfo("Sync Complete")
                }
        }
    }

    private fun selectFeatures(screenCoordinate: ScreenCoordinate, map: ArcGISMap) {
        geodatabaseEditState = GeodatabaseEditState.EDITING
        lifecycleScope.launch {
            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                val identifyLayerResult = mapView.identifyLayer(
                    featureLayer,
                    screenCoordinate,
                    12.0,
                    false
                ).getOrElse {
                    showInfo("Unable to identify selected layer: ${it.message}")
                    return@launch
                }

                val identifiedFeatures = identifyLayerResult.geoElements.filterIsInstance<Feature>()
                featureLayer.selectFeatures(identifiedFeatures)
                selectedFeatures.addAll(identifiedFeatures)
            }
        }
    }

    private fun moveSelectedFeatures(point: Point, map: ArcGISMap) {
        lifecycleScope.launch {
            selectedFeatures.forEach { feature ->
                feature.geometry = point
                feature.featureTable?.updateFeature(feature)
            }

            selectedFeatures.clear()

            map.operationalLayers.filterIsInstance<FeatureLayer>().forEach { featureLayer ->
                featureLayer.clearSelection()
            }

            geodatabaseEditState = GeodatabaseEditState.READY
            syncButton.isEnabled = true
        }
    }


    /**
     * Creates and returns a new alert dialog using the progressDialog with the given [title] and
     * provides an [onDismissListener] callback on dialog cancellation
     */
    private fun createProgressDialog(title: String, job : Job<*>): AlertDialog {
        // build and return a new alert dialog
        return AlertDialog.Builder(this).apply {
            // setting it title
            setTitle(title)
            // allow it to be cancellable
            setCancelable(false)
            // sets negative button configuration
            setNegativeButton("Cancel") { _, _ ->
                // call the dismiss listener
                lifecycleScope.launch {
                    job.cancel()
                }
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
}
