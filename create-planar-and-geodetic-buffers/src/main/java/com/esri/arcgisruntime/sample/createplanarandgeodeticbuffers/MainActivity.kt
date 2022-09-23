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

package com.esri.arcgisruntime.sample.createplanarandgeodeticbuffers

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.geometry.GeodeticCurveType
import arcgisruntime.geometry.GeometryEngine
import arcgisruntime.geometry.LinearUnit
import arcgisruntime.geometry.LinearUnitId
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.symbology.SimpleFillSymbol
import arcgisruntime.mapping.symbology.SimpleFillSymbolStyle
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.symbology.SimpleMarkerSymbol
import arcgisruntime.mapping.symbology.SimpleMarkerSymbolStyle
import arcgisruntime.mapping.symbology.SimpleRenderer
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.sample.createplanarandgeodeticbuffers.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        // get the views from the layout
        val mapView = activityMainBinding.mapView
        val bufferInput = activityMainBinding.bufferInput
        val clearButton = activityMainBinding.clearButton
        // add mapview to the lifecycle
        lifecycle.addObserver(mapView)

        // create a map with a topographic basemap
        mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)

        // create a fill symbol for geodesic buffer polygons
        val geodesicOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.BLACK, 2F)
        val geodesicBufferFillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid, Color.GREEN,
            geodesicOutlineSymbol
        )

        // create a graphics overlay to display geodesic polygons and set its renderer
        val geodesicGraphicsOverlay = GraphicsOverlay().apply {
            renderer = SimpleRenderer(geodesicBufferFillSymbol)
            opacity = 0.5f
        }

        // create a fill symbol for planar buffer polygons
        val planarOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.BLACK, 2F)
        val planarBufferFillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid, Color.RED,
            planarOutlineSymbol
        )

        // create a graphics overlay to display planar polygons and set its renderer
        val planarGraphicsOverlay = GraphicsOverlay().apply {
            renderer = SimpleRenderer(planarBufferFillSymbol)
            opacity = 0.5f
        }

        // create a marker symbol for tap locations
        val tapSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.WHITE, 14F)

        // create a graphics overlay to display tap locations for buffers and set its renderer
        val tapLocationsOverlay = GraphicsOverlay().apply {
            renderer = SimpleRenderer(tapSymbol)
        }

        // add overlays to the mapView
        mapView.graphicsOverlays.addAll(
            listOf(
                geodesicGraphicsOverlay,
                planarGraphicsOverlay,
                tapLocationsOverlay
            )
        )

        // create a buffer around the clicked location
        scope.launch {
            mapView.onSingleTapConfirmed.collect { event ->
                // get map point tapped
                val mapPoint = event.mapPoint

                // only draw a buffer if a value was entered
                if (bufferInput.text.toString().isNotEmpty()) {
                    // get the buffer distance (miles) entered in the text box
                    val bufferInMiles = bufferInput.text.toString().toDouble()

                    // convert the input distance to meters, 1609.34 meters in one mile
                    val bufferInMeters = bufferInMiles * 1609.34

                    // create a planar buffer graphic around the input location at the specified distance
                    val bufferGeometryPlanar =
                        mapPoint?.let { GeometryEngine.buffer(it, bufferInMeters) }
                    val planarBufferGraphic = Graphic(bufferGeometryPlanar)

                    // create a geodesic buffer graphic using the same location and distance
                    val bufferGeometryGeodesic = mapPoint?.let {
                        GeometryEngine.bufferGeodetic(
                            it, bufferInMeters,
                            LinearUnit(LinearUnitId.Meters), Double.NaN, GeodeticCurveType.Geodesic
                        )
                    }
                    val geodesicBufferGraphic = Graphic(bufferGeometryGeodesic)

                    // create a graphic for the user tap location
                    val locationGraphic = Graphic(mapPoint)

                    // add the buffer polygons and tap location graphics to the appropriate graphic overlays
                    planarGraphicsOverlay.graphics.add(planarBufferGraphic)
                    geodesicGraphicsOverlay.graphics.add(geodesicBufferGraphic)
                    tapLocationsOverlay.graphics.add(locationGraphic)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Please enter a buffer distance first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        // clear the graphics from the graphics overlays
        clearButton.setOnClickListener {
            planarGraphicsOverlay.graphics.clear()
            geodesicGraphicsOverlay.graphics.clear()
            tapLocationsOverlay.graphics.clear()
        }
    }
}
