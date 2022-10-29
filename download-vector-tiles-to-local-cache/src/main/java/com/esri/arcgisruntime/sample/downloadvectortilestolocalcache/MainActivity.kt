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

package com.esri.arcgisruntime.sample.downloadvectortilestolocalcache

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.geometry.Envelope
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.Basemap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.ViewpointType
import arcgisruntime.mapping.layers.vectortiles.ArcGISVectorTiledLayer
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import arcgisruntime.mapping.view.MapView
import arcgisruntime.mapping.view.ScreenCoordinate
import arcgisruntime.tasks.JobStatus
import arcgisruntime.tasks.exportvectortiles.ExportVectorTilesJob
import arcgisruntime.tasks.exportvectortiles.ExportVectorTilesParameters
import arcgisruntime.tasks.exportvectortiles.ExportVectorTilesResult
import arcgisruntime.tasks.exportvectortiles.ExportVectorTilesTask
import com.esri.arcgisruntime.sample.downloadvectortilestolocalcache.databinding.ActivityMainBinding
import com.esri.arcgisruntime.sample.downloadvectortilestolocalcache.databinding.ProgressDialogLayoutBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private var downloadArea: Graphic? = null
    private var exportVectorTilesJob: ExportVectorTilesJob? = null
    private var dialog: AlertDialog? = null
    private var isJobFinished: Boolean = true

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val mapPreviewLayout: ConstraintLayout by lazy {
        activityMainBinding.mapPreviewLayout
    }

    private val exportVectorTilesButton: Button by lazy {
        activityMainBinding.exportVectorTilesButton
    }

    private val previewMapView: MapView by lazy {
        activityMainBinding.previewMapView
    }

    private val dimBackground: View by lazy {
        activityMainBinding.dimBackground
    }

    private val closeButton: Button by lazy {
        activityMainBinding.closeButton
    }

    private val previewTextView: TextView by lazy {
        activityMainBinding.previewTextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // add mapView to the lifecycle
        lifecycle.addObserver(mapView)

        // create a graphic to show a red outline square around the vector tiles to be downloaded
        downloadArea = Graphic().apply {
            symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.RED, 2F)
        }

        // create a graphics overlay and add the downloadArea graphic
        val graphicsOverlay = GraphicsOverlay().apply {
            graphics.add(downloadArea!!)
        }

        mapView.apply {
            // set the map to BasemapType navigation night
            map = ArcGISMap(BasemapStyle.ArcGISStreetsNight)
            // disable rotation
            rotation = 0F
            // set the viewpoint of the sample to ESRI Redlands, CA campus
            setViewpoint(Viewpoint(34.056295, -117.195800, 100000.0))
            // add the graphics overlay to the MapView
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.apply {
            launch {
                // update the red square whenever the viewpoint changes
                mapView.viewpointChanged.collect {
                    updateDownloadAreaGeometry()
                }
            }
            launch {
                // when the map has loaded, create a vector tiled layer from the basemap and export the tiles
                mapView.map?.load()?.getOrElse {
                    showError(it.message.toString())
                }
                // check that the layer from the basemap is a vector tiled layer
                val vectorTiledLayer =
                    mapView.map?.basemap?.value?.baseLayers?.get(0) as ArcGISVectorTiledLayer
                handleExportButton(vectorTiledLayer)
            }
        }

    }

    /**
     * Sets up the ExportVectorTilesTask using the [vectorTiledLayer]
     * on export button click. Then call handleExportVectorTilesJob()
     */
    private fun handleExportButton(vectorTiledLayer: ArcGISVectorTiledLayer) {
        exportVectorTilesButton.setOnClickListener {
            // the max scale parameter is set to 10% of the map's scale so the
            // number of tiles exported are within the vector tiled layer's max tile export limit
            lifecycleScope.launch {
                // update the download area's geometry using the current viewpoint
                updateDownloadAreaGeometry()
                // create a new export vector tiles task
                val exportVectorTilesTask = ExportVectorTilesTask(vectorTiledLayer.uri)
                val geometry = downloadArea?.geometry
                if (geometry == null) {
                    showError("Error retrieving download area geometry")
                    return@launch
                }
                val exportVectorTilesParametersFuture = exportVectorTilesTask
                    .createDefaultExportVectorTilesParameters(
                        geometry,
                        mapView.mapScale.value * 0.1
                    )

                val exportVectorTilesParameters = exportVectorTilesParametersFuture.getOrElse {
                    showError(it.message.toString())
                } as ExportVectorTilesParameters

                if (isJobFinished) {
                    // create a job to export vector tiles
                    handleExportVectorTilesJob(
                        exportVectorTilesParameters,
                        exportVectorTilesTask
                    )
                } else {
                    showError("Previous job is cancelling asynchronously")
                }
            }

        }
    }

    /**
     * Start the export vector tiles job using [exportVectorTilesTask] and the
     * [exportVectorTilesParameters]. The vector tile package is exported as "file.vtpk"
     */
    private fun handleExportVectorTilesJob(
        exportVectorTilesParameters: ExportVectorTilesParameters,
        exportVectorTilesTask: ExportVectorTilesTask
    ) {
        // create a .vtpk and directory in the app's cache for saving exported tiles
        val vtpkFile = File(externalCacheDir, "/StyleItemResources/myVectorTiles.vtpk")
        val resDir = File(externalCacheDir, "/StyleItemResources")
        resDir.deleteRecursively()
        resDir.mkdir()

        // inflate the progress dialog
        val dialogLayoutBinding = createProgressDialog()

        // create a job with the export vector tile parameters
        // and exports the vector tile package as "file.vtpk"
        exportVectorTilesJob = exportVectorTilesTask.exportVectorTiles(
            exportVectorTilesParameters,
            vtpkFile.absolutePath, resDir.absolutePath
        ).apply {
            // start the export vector tile cache job
            start()
            // since job is now started, set to false
            isJobFinished = false
            // display the progress dialog
            dialog?.show()
        }

        // set the value of the job's progress
        lifecycleScope.launch {
            val progress = exportVectorTilesJob?.progress?.value ?: 0
            dialogLayoutBinding.progressBar.progress = progress
            dialogLayoutBinding.progressTextView.text = "$progress% completed"
        }

        // display map if job succeeds
        lifecycleScope.launch {
            exportVectorTilesJob?.status?.collect {
                if (it is JobStatus.Succeeded) {
                    // get the result of the job
                    val exportVectorTilesResult =
                        exportVectorTilesJob?.result()?.getOrElse { error ->
                            showError(error.message.toString())
                        } as ExportVectorTilesResult

                    // display the map preview using the result from the completed job
                    showMapPreview(exportVectorTilesResult)
                } else if (it is JobStatus.Failed) {
                    showError("Failed to load the export tiles job")
                }
            }
        }
    }

    /**
     * Updates the [downloadArea]'s geometry on ViewPoint change
     * or when export tiles button is clicked.
     */
    private suspend fun updateDownloadAreaGeometry() {
        mapView.map?.load()?.onSuccess {
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
                downloadArea?.geometry = Envelope(minPoint, maxPoint)
            } else {
                showError("Error getting screen coordinate")
            }
        }?.onFailure {
            showError(it.message.toString())
        }
    }

    /**
     * Create a progress dialog to track the progress of the [exportVectorTilesJob]
     */
    private fun createProgressDialog(): ProgressDialogLayoutBinding {
        val dialogLayoutBinding = ProgressDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder = AlertDialog.Builder(this@MainActivity).apply {
            setTitle("Exporting vector tiles")
            setNegativeButton("Cancel job") { _, _ ->
                lifecycleScope.launch {
                    // cancels the export job asynchronously
                    exportVectorTilesJob?.cancel()?.getOrElse {
                        showError(it.message.toString())
                    }
                    // cancel is completed, so set to true
                    isJobFinished = true
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
            showError("Cannot find tile cache")
            return
        }
        // get the layer exported for the preview MapView
        val vectorTiledLayer = ArcGISVectorTiledLayer(
            vectorTileCache,
            vectorTilesResult.itemResourceCache
        )
        // set up the preview MapView
        previewMapView.apply {
            map = ArcGISMap(Basemap(vectorTiledLayer))
            mapView.getCurrentViewpoint(ViewpointType.CenterAndScale)?.let { setViewpoint(it) }
        }
        // control UI visibility
        previewMapView.getChildAt(0).visibility = View.VISIBLE
        show(closeButton, dimBackground, previewTextView, previewMapView)
        exportVectorTilesButton.visibility = View.GONE

        // required for some Android devices running older OS (combats Z-ordering bug in Android API)
        mapPreviewLayout.bringToFront()
    }

    /**
     * Makes the given views in the UI visible.
     * @param views the views to be made visible
     */
    private fun show(vararg views: View) {
        for (view in views) {
            view.visibility = View.VISIBLE
        }
    }

    /**
     * Makes the given views in the UI visible.
     * @param views the views to be made visible
     */
    private fun hide(vararg views: View) {
        for (view in views) {
            view.visibility = View.INVISIBLE
        }
    }

    /**
     * Called when close preview MapView is clicked
     */
    fun clearPreview(view: View) {
        // control UI visibility
        hide(closeButton, dimBackground, previewTextView, previewMapView)
        show(exportVectorTilesButton, mapView)
        downloadArea?.isVisible = true
        // required for some Android devices running older OS (combats Z-ordering bug in Android API)
        mapView.bringToFront()
    }


    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
