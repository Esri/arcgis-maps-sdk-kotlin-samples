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

package com.esri.arcgisruntime.sample.generateofflinemap

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.LoadStatus
import arcgisruntime.arcgisservices.LevelOfDetail
import arcgisruntime.geometry.Envelope
import arcgisruntime.geometry.Geometry
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import arcgisruntime.mapping.view.MapView
import arcgisruntime.mapping.view.ScreenCoordinate
import arcgisruntime.portal.Portal
import arcgisruntime.portal.PortalItem
import arcgisruntime.tasks.offlinemaptask.GenerateOfflineMapParameters
import arcgisruntime.tasks.offlinemaptask.OfflineMapTask
import com.esri.arcgisruntime.sample.generateofflinemap.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
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

    private val takeMapOfflineButton by lazy {
        activityMainBinding.takeMapOfflineButton
    }

    private val tempDirectoryPath: String by lazy {
        "$cacheDir/offlineMap"
    }

    private val graphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    private val downloadArea: Graphic = Graphic()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // add mapView to the lifecycle
        lifecycle.addObserver(mapView)

        // disable the button until the map is loaded
        takeMapOfflineButton.isEnabled = false

        // create a portal item with the itemId of the web map
        val portal = Portal(getString(R.string.portal_url), false)
        val portalItem = PortalItem(portal, getString(R.string.item_id))

        val map = ArcGISMap(portalItem).apply {
            lifecycleScope.launch {
                load().apply {
                    onSuccess {
                        // limit the map scale to the largest layer scale
                        maxScale = operationalLayers[6].maxScale
                        minScale = operationalLayers[6].minScale
                    }
                    onFailure {
                        showError(it.message.toString())
                    }
                }
            }
        }

        // create a symbol to show a box around the extent we want to download
        downloadArea.symbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.RED, 2F)
        // add the graphic to the graphics overlay when it is created
        graphicsOverlay.graphics.add(downloadArea)

        mapView.apply {
            // set the map to the map view
            this.map = map
            // add the graphics overlay to the map view when it is created
            graphicsOverlays.add(graphicsOverlay)
            // update the download area box whenever the viewpoint changes
            lifecycleScope.launch {
                viewpointChanged.collect {
                    map.loadStatus.collect {
                        if (it is LoadStatus.Loaded) {
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
                                // enable the take map offline button only after the map is loaded
                                if (!takeMapOfflineButton.isEnabled) takeMapOfflineButton.isEnabled =
                                    true
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Use the generate offline map job to generate an offline map.
     *
     * @param view: the button which calls this function
     */
    fun generateOfflineMap(view: View) {
        // delete any offline map already in the cache
        File(tempDirectoryPath).deleteRecursively()

        // specify the extent, min scale, and max scale as parameters
        var minScale: Double = mapView.mapScale.value
        val maxScale: Double = mapView.map?.maxScale ?: 0.0
        // minScale must always be larger than maxScale
        if (minScale <= maxScale) {
            minScale = maxScale + 1
        }
        val geometry = downloadArea.geometry
        if (geometry != null) {
            val generateOfflineMapParameters = GenerateOfflineMapParameters(
                geometry, minScale, maxScale
            ).apply {
                // set job to cancel on any errors
                isContinueOnErrors = false
            }

            val map = mapView.map
            if (map != null) {
                // create an offline map task with the map
                val offlineMapTask = OfflineMapTask(map)
                // create an offline map job with the download directory path and parameters and start the job
                val offlineMapJob =
                    offlineMapTask.generateOfflineMap(generateOfflineMapParameters, tempDirectoryPath)
                // create an alert dialog to show the download progress
                val progressDialogLayoutBinding = GenerateOfflineMapDialogLayoutBinding.inflate(layoutInflater)
                val progressDialog = createProgressDialog(offlineMapJob)
                progressDialog.setView(progressDialogLayoutBinding.root)
                progressDialog.show()
            }

        }



    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(activityMainBinding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
