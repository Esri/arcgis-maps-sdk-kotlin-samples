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

package com.esri.arcgismaps.sample.showlocationhistory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.*
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.*
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.showlocationhistory.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class MainActivity : AppCompatActivity() {

    private var isTrackLocation: Boolean = false

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val button: FloatingActionButton by lazy {
        activityMainBinding.button
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey =
            ApiKey.create("AAPK183faec8d5a64a8a94f599c97c65924bJ73yHdrlxWRiCEI8nUAeJVb9gTzv9_4JoReisBG0ozZpcuMuqM13Q3vcENl8VRJM")
        lifecycle.addObserver(mapView)

        // create a center point for the data in West Los Angeles, California
        val center = Point(-13185535.98, 4037766.28, SpatialReference(102100))

        // create a graphics overlay for the points and use a red circle for the symbols
        val locationHistoryOverlay = GraphicsOverlay()
        val locationSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)
        locationHistoryOverlay.renderer = SimpleRenderer(locationSymbol)

        // create a graphics overlay for the lines connecting the points and use a blue line for the symbol
        val locationHistoryLineOverlay = GraphicsOverlay()
        val locationLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2.0f)
        locationHistoryLineOverlay.renderer = SimpleRenderer(locationLineSymbol)

        mapView.apply {
            // create and add a map with a navigation night basemap style
            map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            setViewpoint(Viewpoint(center, 7000.0))
            graphicsOverlays.addAll(listOf(locationHistoryOverlay, locationHistoryLineOverlay))
        }

        // create a polyline builder to connect the location points
        val polylineBuilder = PolylineBuilder(SpatialReference(102100))

        // create a simulated location data source from json data with simulation parameters to set a consistent velocity
        val simulatedLocationDataSource = SimulatedLocationDataSource(
            Geometry.fromJson(getString(R.string.polyline_data)) as Polyline,
            SimulationParameters(Clock.System.now(), 30.0, 0.0, 0.0)
        )

        lifecycleScope.launch {
            simulatedLocationDataSource.locationChanged.collect { location ->
                // if location tracking is turned off, do not add to the polyline
                if (!isTrackLocation) {
                    return@collect
                }
                // get the point from the location
                val nextPoint = location.position
                // add the new point to the polyline builder
                polylineBuilder.addPoint(nextPoint)
                // add the new point to the two graphics overlays and reset the line connecting the points
                locationHistoryOverlay.graphics.add(Graphic(nextPoint))
                locationHistoryLineOverlay.graphics.apply {
                    clear()
                    add((Graphic(polylineBuilder.toGeometry())))
                }
            }
        }

        // configure the map view's location display to follow the simulated location data source
        mapView.locationDisplay.apply {
            dataSource = simulatedLocationDataSource
            setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            initialZoomScale = 7000.0
        }

        // reset location display and change the isTrackLocation flag and the button's icon
        button.setOnClickListener {
            // if the user has panned away from the location display, turn it on again
            if (mapView.locationDisplay.autoPanMode.value == LocationDisplayAutoPanMode.Off) {
                mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            }

            if (isTrackLocation) {
                isTrackLocation = false
                button.setImageResource(R.drawable.ic_my_location_white_24dp)
            } else {
                isTrackLocation = true
                button.setImageResource(R.drawable.ic_navigation_white_24dp)
            }
        }

        lifecycleScope.launch {
            // start the simulated location data source
            simulatedLocationDataSource.start()
        }

        // make sure the floating action button doesn't obscure the attribution bar
        mapView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val layoutParams = button.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin += bottom - oldBottom
        }
    }
}

