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

package com.esri.arcgismaps.sample.generateofflinemap

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
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.portal.Portal
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.GenerateOfflineMapParameters
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.esri.arcgismaps.sample.generateofflinemap.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.generateofflinemap.databinding.GenerateOfflineMapDialogLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val takeMapOfflineButton by lazy {
        activityMainBinding.takeMapOfflineButton
    }

    private val resetMapButton by lazy {
        activityMainBinding.resetButton
    }

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    // create a symbol to show a box around the extent we want to download
    private val downloadArea: Graphic = Graphic().apply {
        symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2F)
    }

    private val progressDialogLayout by lazy {
        GenerateOfflineMapDialogLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // add mapView to the lifecycle
        lifecycle.addObserver(mapView)
        // set up the portal item to take offline
        setUpMapView()
    }

    /**
     * Sets up a portal item and displays map area to take offline
     */
    private fun setUpMapView() {
        // create a portal item with the itemId of the web map
        val portal = Portal(getString(R.string.portal_url))
        val portalItem = PortalItem(portal, getString(R.string.item_id))

        // clear graphic overlays
        graphicsOverlay.graphics.clear()
        mapView.graphicsOverlays.clear()

        // add the download graphic to the graphics overlay
        graphicsOverlay.graphics.add(downloadArea)
        val map = ArcGISMap(portalItem)
        lifecycleScope.launch {
            map.load()
                .onFailure {
                    showMessage(it.message.toString())
                }
                .onSuccess {
                    // limit the map scale to the largest layer scale
                    map.maxScale = map.operationalLayers[6].maxScale ?: 0.0
                    map.minScale = map.operationalLayers[6].minScale ?: 0.0
                    // set the map to the map view
                    mapView.map = map
                    // add the graphics overlay to the map view when it is created
                    mapView.graphicsOverlays.add(graphicsOverlay)
                }
        }
        lifecycleScope.launch {
            mapView.viewpointChanged.collect {
                // upper left corner of the area to take offline
                val minScreenPoint = ScreenCoordinate(200.0, 200.0)
                // lower right corner of the downloaded area
                val maxScreenPoint = ScreenCoordinate(
                    mapView.width - 200.0,
                    mapView.height - 200.0
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
        }
    }

    /**
     * Use the generate offline map job to generate an offline map.
     *
     * @param view: the button which calls this function
     */
    fun createOfflineMapJob(view: View) {
        // offline map path
        val offlineMapPath = getExternalFilesDir(null)?.path + "/offlineMap"
        // delete any offline map already in the cache
        File(offlineMapPath).deleteRecursively()
        // specify the extent, min scale, and max scale as parameters
        var minScale: Double = mapView.mapScale.value
        val maxScale: Double = mapView.map?.maxScale ?: 0.0
        // minScale must always be larger than maxScale
        if (minScale <= maxScale) {
            minScale = maxScale + 1
        }
        // get the geometry of the downloadArea
        val geometry = downloadArea.geometry
        if (geometry == null) {
            showMessage("Could not get geometry of the downloadArea")
            return
        }
        // set the offline map parameters
        val generateOfflineMapParameters = GenerateOfflineMapParameters(
            geometry, minScale, maxScale
        ).apply {
            // set job to cancel on any errors
            continueOnErrors = false
        }
        // get the map from the MapView
        val map = mapView.map
        if (map == null) {
            showMessage("Could not get map from MapView")
            return
        }
        // create an offline map task with the map
        val offlineMapTask = OfflineMapTask(map)
        // create an offline map job with the download directory path and parameters and start the job
        val offlineMapJob = offlineMapTask.createGenerateOfflineMapJob(
            generateOfflineMapParameters,
            offlineMapPath
        )
        // create an alert dialog to show the download progress
        val progressDialog = createProgressDialog(offlineMapJob).show()
        // handle offline job loading, error and succeed status
        lifecycleScope.launch {
            displayOfflineMapFromJob(offlineMapJob, progressDialog)
        }
    }

    /**
     * Initiates, and tracks the result of the [offlineMapJob] while displaying
     * the progress to the [progressDialog]
     */
    private suspend fun displayOfflineMapFromJob(
        offlineMapJob: GenerateOfflineMapJob,
        progressDialog: AlertDialog
    ) {

        // create a flow-collector for the job's progress
        lifecycleScope.launch {
            offlineMapJob.progress.collect {
                // display the current job's progress value
                val progressPercentage = offlineMapJob.progress.value
                progressDialogLayout.progressBar.progress = progressPercentage
                progressDialogLayout.progressTextView.text = "$progressPercentage%"
            }
        }

        // start the job
        offlineMapJob.start()
        offlineMapJob.result().onSuccess {
            mapView.map = it.offlineMap
            graphicsOverlay.graphics.clear()
            // disable the button to take the map offline once the offline map is showing
            takeMapOfflineButton.isEnabled = false
            resetMapButton.isEnabled = true

            showMessage("Map saved at: " + offlineMapJob.downloadDirectoryPath)

            // close the progress dialog
            progressDialog.dismiss()
        }.onFailure {
            progressDialog.dismiss()
            showMessage(it.message.toString())
        }

    }

    /**
     * Create a progress dialog box for tracking the generate offline map job.
     *
     * @param job the generate offline map job progress to be tracked
     * @return an MaterialAlertDialogBuilder set with the dialog layout view
     */
    private fun createProgressDialog(job: GenerateOfflineMapJob): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(this).apply {
            setTitle("Generating offline map...")
            // provide a cancel button on the dialog
            setNegativeButton("Cancel") { _, _ ->
                lifecycleScope.launch { job.cancel() }
            }
            // removes parent of the progressDialog layout, if previously assigned
            progressDialogLayout.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            setCancelable(false)
            setView(progressDialogLayout.root)
        }
    }

    /**
     * Clear the preview map and display the Portal Item
     */
    fun resetButtonClick(view: View) {
        // enable offline button
        takeMapOfflineButton.isEnabled = true
        resetMapButton.isEnabled = false
        // set up the portal item to take offline
        setUpMapView()
    }

    private fun showMessage(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
