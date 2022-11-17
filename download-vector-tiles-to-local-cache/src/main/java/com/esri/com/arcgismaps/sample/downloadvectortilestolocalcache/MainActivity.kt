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

package com.esri.com.arcgismaps.sample.downloadvectortilestolocalcache

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.layers.ArcGISVectorTiledLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.exportvectortiles.ExportVectorTilesJob
import com.arcgismaps.tasks.exportvectortiles.ExportVectorTilesParameters
import com.arcgismaps.tasks.exportvectortiles.ExportVectorTilesResult
import com.arcgismaps.tasks.exportvectortiles.ExportVectorTilesTask
import com.esri.com.arcgismaps.sample.downloadvectortilestolocalcache.databinding.ActivityMainBinding
import com.esri.com.arcgismaps.sample.downloadvectortilestolocalcache.databinding.ProgressDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val downloadArea: Graphic = Graphic()
    private var dialog: AlertDialog? = null
    private var hasCurrentJobCompleted: Boolean = true

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val previewMapView by lazy {
        activityMainBinding.previewMapView
    }

    private val exportVectorTilesButton: Button by lazy {
        activityMainBinding.exportVectorTilesButton
    }

    private val closePreviewButton: Button by lazy {
        activityMainBinding.closePreviewButton
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // add mapView to the lifecycle
        lifecycle.addObserver(mapView)
        lifecycle.addObserver(previewMapView)

        // create a graphic to show a red outline square around the vector tiles to be downloaded
        downloadArea.symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.red, 2f)

        // create a graphics overlay and add the downloadArea graphic
        val graphicsOverlay = GraphicsOverlay(listOf(downloadArea))

        mapView.apply {
            // set the map to BasemapType navigation night
            map = ArcGISMap(BasemapStyle.ArcGISStreetsNight)
            // disable rotation
            interactionOptions.isRotateEnabled = false
            // set the viewpoint of the sample to ESRI Redlands, CA campus
            setViewpoint(Viewpoint(34.056295, -117.195800, 100000.0))
            // add the graphics overlay to the MapView
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            mapView.map?.load()?.onSuccess {
                // enable the export tiles button
                exportVectorTilesButton.isEnabled = true
                // update the red square whenever the viewpoint changes
                mapView.viewpointChanged.collect {
                    updateDownloadAreaGeometry()
                }
            }?.onFailure {
                showMessage("Error loading map")
            }
        }
    }

    /**
     * Sets up the ExportVectorTilesTask on export button click.
     * Then calls handleExportVectorTilesJob()
     */
    fun exportButtonClick(view: View) {
        // check that the layer from the basemap is a vector tiled layer
        val vectorTiledLayer =
            mapView.map?.basemap?.value?.baseLayers?.get(0) as ArcGISVectorTiledLayer

        lifecycleScope.launch {
            // update the download area's geometry using the current viewpoint
            updateDownloadAreaGeometry()
            // create a new export vector tiles task
            val exportVectorTilesTask = ExportVectorTilesTask(vectorTiledLayer.uri.toString())
            val geometry = downloadArea.geometry
            if (geometry == null) {
                showMessage("Error retrieving download area geometry")
                return@launch
            }
            // set the parameters of the export vector tiles task
            // using the geometry of the area to export and it's max scale
            val exportVectorTilesParametersResult = exportVectorTilesTask
                .createDefaultExportVectorTilesParameters(
                    geometry,
                    // set the max scale parameter to 10% of the map's scale so the
                    // number of tiles exported are within the vector tiled layer's max tile export limit
                    mapView.mapScale.value * 0.1
                )

            // get the loaded vector tile parameters
            val exportVectorTilesParameters = exportVectorTilesParametersResult.getOrElse {
                showMessage(it.message.toString())
            } as ExportVectorTilesParameters

            if (hasCurrentJobCompleted) {
                // create a job to export vector tiles
                initiateExportTilesJob(
                    exportVectorTilesParameters,
                    exportVectorTilesTask
                )
            } else {
                showMessage("Previous job is cancelling asynchronously")
            }
        }

    }

    /**
     * Start the export vector tiles job using [exportVectorTilesTask] and the
     * [exportVectorTilesParameters]. The vector tile package is exported as "file.vtpk"
     */
    private fun initiateExportTilesJob(
        exportVectorTilesParameters: ExportVectorTilesParameters,
        exportVectorTilesTask: ExportVectorTilesTask
    ) {
        // create a .vtpk and directory in the app's cache for saving exported tiles
        val vtpkFile = File(externalCacheDir, "/StyleItemResources/myVectorTiles.vtpk")
        val resDir = File(externalCacheDir, "/StyleItemResources")
        resDir.deleteRecursively()
        resDir.mkdir()

        // create a job with the export vector tile parameters
        // and exports the vector tile package as "file.vtpk"
        val exportVectorTilesJob = exportVectorTilesTask.exportVectorTiles(
            exportVectorTilesParameters,
            vtpkFile.absolutePath, resDir.absolutePath
        ).apply {
            // start the export vector tile cache job
            start()
        }

        // inflate the progress dialog
        val dialogLayoutBinding = createProgressDialog(exportVectorTilesJob)
        // display the progress dialog
        dialog?.show()
        // since job is now started, set to false
        hasCurrentJobCompleted = false

        // set the value of the job's progress
        with(lifecycleScope) {
            // collect the progress of the job
            launch {
                exportVectorTilesJob.progress.collect {
                    val progress = exportVectorTilesJob.progress.value
                    dialogLayoutBinding.progressBar.progress = progress
                    dialogLayoutBinding.progressTextView.text = "$progress% completed"
                    Log.e(TAG, progress.toString())
                }
            }
            // display map if job succeeds
            launch {
                exportVectorTilesJob.result().onSuccess {
                    // display the map preview using the result from the completed job
                    showMapPreview(it)
                    // set job is completed
                    hasCurrentJobCompleted = true
                    // display the path of the saved vector tiles
                    showMessage(it.vectorTileCache?.path.toString())
                    // dismiss loading dialog
                    dialog?.dismiss()
                }.onFailure {
                    showMessage(it.message.toString())
                    dialog?.dismiss()
                    hasCurrentJobCompleted = true
                }
            }

        }
    }

    /**
     * Updates the [downloadArea]'s geometry when called with viewpoint change
     * or when export tiles button is clicked.
     */
    private fun updateDownloadAreaGeometry() {
        // upper left corner of the downloaded tile cache area
        val minScreenPoint = ScreenCoordinate(150.0, 175.0)
        // lower right corner of the downloaded tile cache area
        val maxScreenPoint = ScreenCoordinate(
            mapView.width - 150.0,
            mapView.height - 250.0
        )
        // convert screen points to map points
        val minPoint = mapView.screenToLocation(minScreenPoint)
        val maxPoint = mapView.screenToLocation(maxScreenPoint)
        if (minPoint != null && maxPoint != null) {
            // use the points to define and return an envelope
            downloadArea.geometry = Envelope(minPoint, maxPoint)
        } else {
            showMessage("Error getting screen coordinate")
        }
    }

    /**
     * Create a progress dialog to track the progress of the [exportVectorTilesJob]
     */
    private fun createProgressDialog(exportVectorTilesJob: ExportVectorTilesJob): ProgressDialogLayoutBinding {
        val dialogLayoutBinding = ProgressDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(this@MainActivity).apply {
            setTitle("Exporting vector tiles")
            setNegativeButton("Cancel job") { _, _ ->
                lifecycleScope.launch {
                    // cancels the export job asynchronously
                    exportVectorTilesJob.cancel().getOrElse {
                        showMessage(it.message.toString())
                    }
                    // cancel is completed, so set to true
                    hasCurrentJobCompleted = true
                }
            }
            setCancelable(false)
            setView(dialogLayoutBinding.root)
        }
        dialog = dialogBuilder.create()
        return dialogLayoutBinding
    }

    /**
     * Display the preview of the exported map using the
     * [vectorTilesResult] from the completed job
     */
    private fun showMapPreview(vectorTilesResult: ExportVectorTilesResult) {
        val vectorTileCache = vectorTilesResult.vectorTileCache
        if (vectorTileCache == null) {
            showMessage("Cannot find tile cache")
            return
        }
        // get the layer exported for the preview MapView
        val vectorTiledLayer = ArcGISVectorTiledLayer(
            vectorTileCache,
            vectorTilesResult.itemResourceCache
        )

        // control UI visibility
        previewMapVisibility(true)

        // set up the preview MapView
        previewMapView.apply {
            map = ArcGISMap(Basemap(vectorTiledLayer))
            mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)?.let { setViewpoint(it) }
        }
        closePreviewButton.setOnClickListener {
            previewMapVisibility(false)
        }

    }

    /**
     * Controls the visibility of the preview map and the export buttons.
     */
    private fun previewMapVisibility(isVisible: Boolean) = if (isVisible) {
        exportVectorTilesButton.visibility = View.INVISIBLE
        closePreviewButton.visibility = View.VISIBLE
        previewMapView.visibility = View.VISIBLE
    } else {
        exportVectorTilesButton.visibility = View.VISIBLE
        closePreviewButton.visibility = View.INVISIBLE
        previewMapView.visibility = View.GONE
    }

    private fun showMessage(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
