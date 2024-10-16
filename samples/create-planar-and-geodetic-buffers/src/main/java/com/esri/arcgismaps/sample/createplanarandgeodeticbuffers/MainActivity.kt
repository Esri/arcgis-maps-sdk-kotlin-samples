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

package com.esri.arcgismaps.sample.createplanarandgeodeticbuffers

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.LinearUnitId
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.createplanarandgeodeticbuffers.databinding.CreatePlanarAndGeodeticBuffersActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        // set up data binding for the activity
        val activityMainBinding: CreatePlanarAndGeodeticBuffersActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.create_planar_and_geodetic_buffers_activity_main)

        // get the views from the layout
        val mapView = activityMainBinding.mapView
        val optionsButton = activityMainBinding.optionsButton
        val clearButton = activityMainBinding.clearButton
        // add mapview to the lifecycle
        lifecycle.addObserver(mapView)

        // create a map with a topographic basemap
        mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)

        // create a fill symbol for geodesic buffer polygons
        val geodesicOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2F)
        val geodesicBufferFillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid, Color.green,
            geodesicOutlineSymbol
        )

        // create a graphics overlay to display geodesic polygons and set its renderer
        val geodesicGraphicsOverlay = GraphicsOverlay().apply {
            renderer = SimpleRenderer(geodesicBufferFillSymbol)
            opacity = 0.5f
        }

        // create a fill symbol for planar buffer polygons
        val planarOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 2F)
        val planarBufferFillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid, Color.red,
            planarOutlineSymbol
        )

        // create a graphics overlay to display planar polygons and set its renderer
        val planarGraphicsOverlay = GraphicsOverlay().apply {
            renderer = SimpleRenderer(planarBufferFillSymbol)
            opacity = 0.5f
        }

        // create a marker symbol for tap locations
        val tapSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Cross, Color.white, 14F)

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

        // set the default buffer distance in miles
        var bufferInMiles = 500f

        // create a buffer around the clicked location
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { event ->
                // get map point tapped, return if null
                val mapPoint = event.mapPoint ?: return@collect

                // convert the input distance to meters, 1609.34 meters in one mile
                val bufferInMeters = bufferInMiles * 1609.34

                // create a planar buffer graphic around the input location at the specified distance
                val bufferGeometryPlanar = GeometryEngine.bufferOrNull(mapPoint, bufferInMeters)
                val planarBufferGraphic = Graphic(bufferGeometryPlanar)

                // create a geodesic buffer graphic using the same location and distance
                val bufferGeometryGeodesic =
                    GeometryEngine.bufferGeodeticOrNull(
                        mapPoint, bufferInMeters,
                        LinearUnit(LinearUnitId.Meters), Double.NaN, GeodeticCurveType.Geodesic
                    )
                val geodesicBufferGraphic = Graphic(bufferGeometryGeodesic)

                // create a graphic for the user tap location
                val locationGraphic = Graphic(mapPoint)

                // add the buffer polygons and tap location graphics to the appropriate graphic overlays
                planarGraphicsOverlay.graphics.add(planarBufferGraphic)
                geodesicGraphicsOverlay.graphics.add(geodesicBufferGraphic)
                tapLocationsOverlay.graphics.add(locationGraphic)

                // set button interaction
                clearButton.isEnabled = true

            }
        }
        // open the option dialog
        optionsButton.setOnClickListener {
            val optionsDialog: View = layoutInflater.inflate(R.layout.buffer_options_dialog, null)
            // set up the dialog builder and the title
            val dialogBuilder = MaterialAlertDialogBuilder(this)
                .setView(optionsDialog)
                .setPositiveButton("Set buffer", null)

            dialogBuilder.setTitle("Set buffer radius")
            // set up the dialog views
            val bufferValue = optionsDialog.findViewById<TextView>(R.id.bufferValue)
            val bufferSlider = optionsDialog.findViewById<Slider>(R.id.bufferInput)
            bufferSlider.value = bufferInMiles

            // set initial buffer text
            bufferValue.text = String.format(Locale.getDefault(),"%d miles", bufferSlider.value.toInt())
            bufferSlider.addOnChangeListener { _, value, _ ->
                // update buffer text value on slider change
                bufferValue.text = String.format(Locale.getDefault(),"%d miles", value.toInt())
                bufferInMiles = value
            }

            // display the dialog
            dialogBuilder.show()
        }

        // clear the graphics from the graphics overlays
        clearButton.setOnClickListener {
            planarGraphicsOverlay.graphics.clear()
            geodesicGraphicsOverlay.graphics.clear()
            tapLocationsOverlay.graphics.clear()
            // set button interaction
            clearButton.isEnabled = false
        }
    }
}
