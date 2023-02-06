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
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.Job
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.GenerateGeodatabaseDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
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

    private val generateButton by lazy {
        activityMainBinding.generateButton
    }

    private val progressDialog by lazy {
        GenerateGeodatabaseDialogLayoutBinding.inflate(layoutInflater)
    }

    private val boundarySymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 5f)

    // creates a graphic overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // debug, remove for prod
        mapView.keepScreenOn = true

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        map.maxExtent = Envelope(
            -13687689.2185849, 5687273.88331375,
            -13622795.3756647, 5727520.22085841,
            spatialReference = SpatialReference.webMercator()
        )
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        val featureServiceURL =
            "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Mobile_Data_Collection_WFL1/FeatureServer"
        val geodatabaseSyncTask = GeodatabaseSyncTask(featureServiceURL)


        lifecycleScope.launch {
            map.load().onFailure {
                showError("Unable to load map")
                return@launch
            }
            geodatabaseSyncTask.load().onFailure {
                showError("Failed to fetch geodatabase metadata")
                return@launch
            }
            generateButton.isEnabled = true
            generateButton.setOnClickListener {
                mapView.visibleArea?.let { polygon ->
                    generateGeodatabase(geodatabaseSyncTask, map, polygon.extent)
                }
            }
        }
    }

    private fun generateGeodatabase(
        geodatabaseSyncTask: GeodatabaseSyncTask,
        map: ArcGISMap,
        extents: Envelope
    ) {
        map.operationalLayers.clear()
        graphicsOverlay.graphics.clear()

        val boundary = Graphic(extents, boundarySymbol)
        graphicsOverlay.graphics.add(boundary)

        lifecycleScope.launch {
            val defaultParameters = geodatabaseSyncTask.createDefaultGenerateGeodatabaseParameters(extents).getOrElse {
                showError("Error creating geodatabase parameters")
                return@launch
            }
            defaultParameters.returnAttachments = false
            geodatabaseSyncTask.createGenerateGeodatabaseJob(defaultParameters, makeGeodatabaseFilePath()).run {
                val dialog = createProgressDialog(this).apply {
                    show()
                    setCancelable(true)
                    setCanceledOnTouchOutside(false)
                }
                // launch a progress collector
                launch {
                    progress.collect { value ->
                        Log.d(TAG, "generateGeodatabase: $value")
                        progressDialog.progressBar.progress = value
                        progressDialog.progressTextView.text = "$value%"
                    }
                }
                // launch a status changed collector
                launch {
                    status.collect { status ->
                        when (status) {
                            JobStatus.Succeeded -> {

                            }
                            JobStatus.Failed -> {
                                showError("Failed to generate geodatabase")
                            }
                            else -> {}
                        }
                    }
                }
                start()
                result().onSuccess { geodatabase ->
                    geodatabase.load().onFailure {
                        showError("Error loading geodatabase")
                        cancelAndReturn(this@launch)
                    }
                    for (featureTable in geodatabase.featureTables) {
                        featureTable.load().onFailure {
                            showError("Error loading feature table")
                            cancelAndReturn(this@launch)
                        }
                        map.operationalLayers += FeatureLayer(featureTable)
                    }
                    generateButton.visibility = View.GONE
                    dialog.dismiss()
                    geodatabaseSyncTask.unregisterGeodatabase(geodatabase)
                }.onFailure {
                    showError("Error fetching geodatabase")
                    generateButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun createProgressDialog(generateGeodatabaseJob: GenerateGeodatabaseJob): AlertDialog {
        return AlertDialog.Builder(this).apply {
            setNegativeButton("Cancel") { _, _ ->
                lifecycleScope.launch {
                    generateGeodatabaseJob.cancel()
                }
            }
            progressDialog.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            setView(progressDialog.root)
        }.create()
    }

    private fun makeGeodatabaseFilePath() = getExternalFilesDir(null)!!.path + "/gdb_${System.currentTimeMillis()}.geodatabase"

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

private suspend fun <T> Job<T>.cancelAndReturn(coroutineScope: CoroutineScope) {
    cancel()
    return
}
