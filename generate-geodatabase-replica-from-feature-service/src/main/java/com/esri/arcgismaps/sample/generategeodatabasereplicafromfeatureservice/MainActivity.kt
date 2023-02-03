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
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.JobStatus
import com.arcgismaps.tasks.geodatabase.GenerateGeodatabaseJob
import com.arcgismaps.tasks.geodatabase.GeodatabaseSyncTask
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generategeodatabasereplicafromfeatureservice.databinding.GenerateGeodatabaseDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

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
        val map = ArcGISMap(BasemapStyle.ArcGISImageryStandard)
        mapView.apply {
            this.map = map
            graphicsOverlays.add(graphicsOverlay)
        }

        val featureServiceURL =
            "https://services2.arcgis.com/ZQgQTuoyBrtmoGdP/arcgis/rest/services/Mobile_Data_Collection_WFL1/FeatureServer"
        val geodatabaseSyncTask = GeodatabaseSyncTask(featureServiceURL)


        // TO DO
        // add check for map.load
        lifecycleScope.launch {
            geodatabaseSyncTask.load().onSuccess { it ->
                Log.d(TAG, "onCreate: OnSuccess")
                generateButton.isEnabled = true
                generateButton.setOnClickListener {
                    mapView.visibleArea?.let { polygon ->
                        generateGeodatabase(geodatabaseSyncTask, map, polygon.extent)
                    }
                }
            }.onFailure {
                Log.d(TAG, "onCreate: OnFailure")
                showError("Unable to fetch feature service")
            }
        }
    }

    private fun generateGeodatabase(
        syncTask: GeodatabaseSyncTask,
        map: ArcGISMap,
        extents: Envelope
    ) {
        map.operationalLayers.clear()
        graphicsOverlay.graphics.clear()

        val boundary = Graphic(extents, boundarySymbol)
        graphicsOverlay.graphics.add(boundary)

        lifecycleScope.launch {
            syncTask.createDefaultGenerateGeodatabaseParameters(extents)
                .onSuccess { defaultParameters ->
                    defaultParameters.outSpatialReference = SpatialReference(102100)
                    defaultParameters.returnAttachments = false

                    val filepath = getExternalFilesDir(null)!!.path + "/gbdf.geodatabase"
                    syncTask.createGenerateGeodatabaseJob(defaultParameters, filepath)
                        .run {
                            start()
                            val dialog = createProgressDialog(this)
                            dialog.show()
                            // launch a progress collector with the lifecycle scope
                            launch {
                                progress.collect { value ->
                                    Log.d(TAG, "generateGeodatabase: $value")
                                    progressDialog.progressBar.progress = value
                                    progressDialog.progressTextView.text = "$value%"
                                }
                            }
                            // launch a status changed collector within this uiscope
                            launch {
                                status.collect { status ->
                                    Log.d(TAG, "generateGeodatabase: $status")
                                    if (status == JobStatus.Succeeded) {
                                        generateButton.visibility = View.GONE
                                        Log.d(TAG, "generateGeodatabase: Done")
                                    }
                                    //dialog.dismiss()
                                }
                            }

                            launch {
                                messages.collect { msg ->
                                    Log.d(TAG, "generateGeodatabase: ${msg.message}")
                                }
                            }
                        }
                }.onFailure {
                    showError("Error creating geodatabase parameters")
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
            setCancelable(true)
            progressDialog.root.parent?.let {
                (it as ViewGroup).removeAllViews()
            }
            setView(progressDialog.root)
        }.create()
    }

    private fun loadAndDisplayGeodatabase() {

    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
