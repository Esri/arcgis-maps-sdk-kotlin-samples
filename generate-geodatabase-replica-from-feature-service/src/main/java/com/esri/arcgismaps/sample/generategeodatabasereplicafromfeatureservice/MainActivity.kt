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

package com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.Geodatabase
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
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.GenerateGeodatabaseDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // setup data binding for the mapview
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // starts the geodatabase replica process
    private val generateButton by lazy {
        activityMainBinding.generateButton
    }

    private val resetButton by lazy {
        activityMainBinding.resetButton
    }

    // shows the geodatabase loading progress
    private val progressDialog by lazy {
        GenerateGeodatabaseDialogLayoutBinding.inflate(layoutInflater)
    }

    // local file path to the geodatabase
    private val geodatabaseFilePath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.portland_trees_geodatabase_file)
    }

    private val downloadArea: Graphic = Graphic()

    // creates a graphic overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a Topographic basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the max map extents to that of the feature service
        // representing portland area
        map.maxExtent = Envelope(
            -13687689.2185849,
            5687273.88331375,
            -13622795.3756647,
            5727520.22085841,
            spatialReference = SpatialReference.webMercator()
        )
        // configure mapview assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to display the boundary
            graphicsOverlays.add(graphicsOverlay)
        }

        // create a geodatabase sync task with the feature service url
        // This feature service shows a web map of portland street trees,
        // their attributes, as well as related inspection information
        val geodatabaseSyncTask = GeodatabaseSyncTask(getString(R.string.feature_server_url))

        // set the button's onClickListener
        generateButton.setOnClickListener {
            // start the geodatabase generation process
            generateGeodatabase(geodatabaseSyncTask, map, downloadArea.geometry?.extent)
        }

        resetButton.setOnClickListener {
            // clear any layers already on the map
            map.operationalLayers.clear()
            // clear all symbols drawn
            graphicsOverlay.graphics.clear()
            // add the download boundary
            graphicsOverlay.graphics.add(downloadArea)
            // show generate button
            generateButton.isEnabled = true
            resetButton.isEnabled = false
        }

        lifecycleScope.launch {
            // show the error and return if map load failed
            map.load().onFailure {
                showError("Unable to load map")
                return@launch
            }

            geodatabaseSyncTask.load().onFailure {
                // if the metadata load fails, show the error and return
                showError("Failed to fetch geodatabase metadata")
                return@launch
            }

            // show download area once map is loaded
            updateDownloadArea()

            // enable the generate button since the task is now loaded
            generateButton.isEnabled = true

            // create a symbol to show a box around the extent we want to download
            downloadArea.symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2F)
            // add the graphic to the graphics overlay when it is created
            graphicsOverlay.graphics.add(downloadArea)
            // update the download area on viewpoint change
            mapView.viewpointChanged.collect {
                updateDownloadArea()
            }
        }
    }

    /**
     * Displays a red border on the map to signify the [downloadArea]
     */
    private fun updateDownloadArea() {
        // define screen area to create replica
        val minScreenPoint = ScreenCoordinate(200.0, 200.0)
        val maxScreenPoint = ScreenCoordinate(
            mapView.measuredWidth - 200.0,
            mapView.measuredHeight - 200.0
        )
        // convert screen points to map points
        val minPoint = mapView.screenToLocation(minScreenPoint)
        val maxPoint = mapView.screenToLocation(maxScreenPoint)
        // use the points to define and return an envelope
        if (minPoint != null && maxPoint != null) {
            val envelope = Envelope(minPoint, maxPoint)
            downloadArea.geometry = envelope
        }
    }

    /**
     * Starts a [geodatabaseSyncTask] with the given [map] and [extents],
     * runs a GenerateGeodatabaseJob and saves the geodatabase file into local storage
     */
    private fun generateGeodatabase(
        geodatabaseSyncTask: GeodatabaseSyncTask,
        map: ArcGISMap,
        extents: Envelope?
    ) {
        if (extents == null) {
            return showError("Download area extent is null")
        }

        lifecycleScope.launch {
            // create generate geodatabase parameters for the selected extents
            val defaultParameters =
                geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents).getOrElse {
                    // show the error and return if the task fails
                    showError("Error creating geodatabase parameters")
                    return@launch
                }.apply {
                    // set the parameters to only create a replica of the Trees (0) layer
                    layerOptions.removeIf { layerOptions ->
                        layerOptions.layerId != 0L
                    }
                }

            // set return attachments option to false
            // indicates if any attachments are added to the geodatabase from the feature service
            defaultParameters.returnAttachments = false
            // create a generate geodatabase job
            geodatabaseSyncTask.createGenerateGeodatabaseJob(defaultParameters, geodatabaseFilePath)
                .run {
                    // create a dialog to show the jobs progress
                    val dialog = createProgressDialog(this)
                    // show the dialog
                    dialog.show()
                    // launch a progress collector to display progress
                    launch {
                        progress.collect { value ->
                            // update the progress bar and progress text
                            progressDialog.progressBar.progress = value
                            progressDialog.progressTextView.text = "$value%"
                        }
                    }
                    // start the generateGeodatabase job
                    start()
                    // if the job completed successfully, get the geodatabase from the result
                    val geodatabase = result().getOrElse {
                        // show an error and return if job failed
                        showError("Error fetching geodatabase: ${it.message}")
                        // dismiss the dialog
                        dialog.dismiss()
                        return@launch
                    }

                    // load and display the geodatabase
                    loadGeodatabase(geodatabase, map)
                    // dismiss the dialog view
                    dialog.dismiss()
                    // unregister since we are not syncing
                    geodatabaseSyncTask.unregisterGeodatabase(geodatabase)
                    // show reset button as the task is now complete
                    generateButton.isEnabled = false
                    resetButton.isEnabled = true
                }
        }
    }

    /**
     * Loads the [geodatabase] and renders the feature layers on to the [map]
     */
    private suspend fun loadGeodatabase(geodatabase: Geodatabase, map: ArcGISMap) {
        // clear any layers already on the map
        map.operationalLayers.clear()
        // clear all symbols drawn
        graphicsOverlay.graphics.clear()

        // load the geodatabase
        geodatabase.load().onFailure {
            // if the load failed, show the error and return
            showError("Error loading geodatabase")
            return
        }
        // add all of the geodatabase feature tables to the map as feature layers
        map.operationalLayers += geodatabase.featureTables.map { featureTable ->
            FeatureLayer.createWithFeatureTable(featureTable)
        }
    }

    /**
     * Creates a new alert dialog using the progressDialog and provides
     * GenerateGeodatabaseJob cancellation on dialog cancellation
     *
     * @param generateGeodatabaseJob the job to cancel
     *
     * @return returns an alert dialog
     */
    private fun createProgressDialog(generateGeodatabaseJob: GenerateGeodatabaseJob): AlertDialog {
        // build and return a new alert dialog
        return AlertDialog.Builder(this).apply {
            // setting it title
            setTitle(getString(R.string.dialog_title))
            // allow it to be cancellable
            setCancelable(false)
            // sets negative button configuration
            setNegativeButton("Cancel") { _, _ ->
                // cancels the generateGeodatabaseJob
                lifecycleScope.launch {
                    generateGeodatabaseJob.cancel()
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

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
