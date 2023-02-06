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

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.GenerateGeodatabaseDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

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

    // shows the geodatabase loading progress
    private val progressDialog by lazy {
        GenerateGeodatabaseDialogLayoutBinding.inflate(layoutInflater)
    }

    // local file path to the geodatabase
    private val geodatabaseFilePath by lazy {
        getExternalFilesDir(null)?.path + getString(R.string.portland_trees_geodatabase_file)
    }

    // a red boundary line showing the filtered map extents for the features
    private val boundarySymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

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
            -13687689.2185849, 5687273.88331375,
            -13622795.3756647, 5727520.22085841,
            spatialReference = SpatialReference.webMercator()
        )
        // configure mapview assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to display the boundary
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // show the error and return if map load failed
            map.load().onFailure {
                showError("Unable to load map")
                return@launch
            }
            // create a geodatabase sync task with the feature service url
            // This feature service shows a web map of portland street trees,
            // their attributes, as well as related inspection information
            val geodatabaseSyncTask = GeodatabaseSyncTask(getString(R.string.feature_server_url))
            geodatabaseSyncTask.load().onFailure {
                // if the metadata load fails, show the error and return
                showError("Failed to fetch geodatabase metadata")
                return@launch
            }
            // enable the generate button and add an onClickListener
            generateButton.isEnabled = true
            generateButton.setOnClickListener {
                mapView.visibleArea?.let { polygon ->
                    // start the geodatabase generation process
                    generateGeodatabase(geodatabaseSyncTask, map, polygon.extent)
                }
            }
        }
    }

    /**
     * Starts a GeodatabaseSyncTask, runs a GenerateGeodatabaseJob and saves
     * the geodatabase file into local storage
     *
     * @param geodatabaseSyncTask the GeodatabaseSyncTask object
     * @param map mapView to render the feature layers on
     * @param extents the selected extents of the mapView
     */
    private fun generateGeodatabase(geodatabaseSyncTask: GeodatabaseSyncTask, map: ArcGISMap, extents: Envelope) {
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
            val defaultParameters = geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents).getOrElse {
                // show the error and return if the task fails
                showError("Error creating geodatabase parameters")
                return@launch
            }

            // set return attachments option to false
            defaultParameters.returnAttachments = false
            // create a generate geodatabase job
            geodatabaseSyncTask.createGenerateGeodatabaseJob(defaultParameters, geodatabaseFilePath).run {
                // create a dialog to show the jobs progress
                val dialog = createProgressDialog(this).apply {
                    // show the dialog
                    show()
                    // allow it to be cancellable
                    setCancelable(true)
                    // disable dismissal when tapped outside the dialog
                    setCanceledOnTouchOutside(false)
                }
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
                    showError("Error fetching geodatabase")
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
            }
        }
    }

    /**
     * Loads the geodatabase and renders the feature layers on to the map
     *
     * @param geodatabase geodatabase to load
     * @param map map to display it on
     */
    private suspend fun loadGeodatabase(geodatabase: Geodatabase, map: ArcGISMap) {
        // load the geodatabase
        geodatabase.load().onFailure {
            // if the load failed, show the error and return
            showError("Error loading geodatabase")
            return
        }
        // add all of the geodatabase feature tables to the map as feature layers
        map.operationalLayers += geodatabase.featureTables.map { featureTable ->
            FeatureLayer(featureTable)
        }
        // hide the generate button as the task is now complete
        generateButton.visibility = View.GONE
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
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
