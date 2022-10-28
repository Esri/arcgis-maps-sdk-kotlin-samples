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
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.layers.vectortiles.ArcGISVectorTiledLayer
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import arcgisruntime.mapping.view.MapView
import arcgisruntime.tasks.exportvectortiles.ExportVectorTilesJob
import com.esri.arcgisruntime.sample.downloadvectortilestolocalcache.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
                val vectorTiledLayer = mapView.map.basemap.baseLayers[0] as ArcGISVectorTiledLayer
                handleExportButton(vectorTiledLayer)
            }
        }

    }

    private fun handleExportButton(vectorTiledLayer: ArcGISVectorTiledLayer) {

    }

    private fun updateDownloadAreaGeometry() {
        TODO("Not yet implemented")
    }


    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
