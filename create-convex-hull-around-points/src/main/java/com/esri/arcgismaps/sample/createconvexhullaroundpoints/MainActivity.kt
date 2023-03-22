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

package com.esri.arcgismaps.sample.createconvexhullaroundpoints

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.createconvexhullaroundpoints.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // setup binding for the MapView
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // action button that creates the canvas hull
    private val createButton by lazy {
        activityMainBinding.createButton
    }

    // action button to reset the map
    private val resetButton by lazy {
        activityMainBinding.resetButton
    }

    // a red marker symbol for points
    private val pointSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)

    // a blue line symbol
    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.blue, 3f)

    // a fill symbol with an empty fill for polygons
    private val fillSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Null, Color.red, lineSymbol)

    // set up the point graphic with point symbol
    private val pointGraphic = Graphic(symbol = pointSymbol)

    // init the convex hull graphic
    private val convexHullGraphic = Graphic()

    // create a graphics overlay to draw all graphics
    private val graphicsOverlay = GraphicsOverlay()

    // list to store the selected map points
    private val inputPoints = mutableListOf<Point>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // add point and convex hull graphics to the graphics overlay
        graphicsOverlay.graphics.addAll(listOf(pointGraphic, convexHullGraphic))

        // create and add a map with topographic basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            // set a default initial point and scale
            initialViewpoint = Viewpoint(Point(34.77, -10.24), 20e7)
        }

        // configure map view assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to the mapview
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // if the map load fails show the error and return
            map.load().onFailure {
                return@launch showError("Error loading map")
            }
            // capture and collect when the user taps on the screen
            mapView.onSingleTapConfirmed.collect { event ->
                event.mapPoint?.let { point ->
                    addMapPoint(point)
                }
            }
        }

        // add a click listener to create a convex hull
        createButton.setOnClickListener {
            // check if the pointGraphic's geometry is not null
            pointGraphic.geometry?.let { geometry ->
                createConvexHull(geometry)
            }
        }

        // add a click listener to reset the map
        resetButton.setOnClickListener {
            resetMap()
        }
    }

    /**
     * Adds the [point] to the map drawn as a Multipoint geometry
     */
    private fun addMapPoint(point: Point) {
        // add the new point to the points list
        inputPoints.add(point)
        // recreate the graphics geometry representing the input points
        pointGraphic.geometry = Multipoint(inputPoints)
        // enable all the action buttons, since we have at least one point drawn
        createButton.isEnabled = true
        resetButton.isEnabled = true
    }

    /**
     * Creates and draws a convex hull graphic on the map using [pointGeometry] points
     */
    private fun createConvexHull(pointGeometry: Geometry) {
        // normalize the geometry for panning beyond the meridian
        // and proceed if the resulting geometry is not null
        val normalizedPointGeometry = GeometryEngine.normalizeCentralMeridian(pointGeometry)
            ?: return showError("Error normalizing point geometry")

        // create a convex hull from the points and proceed if it's not null
        val convexHullGeometry = GeometryEngine.convexHull(normalizedPointGeometry)

        // the convex hull's geometry may be a point or polyline if the number of
        // points is less than 3, set its symbol accordingly
        convexHullGraphic.symbol = when (convexHullGeometry) {
            is Point -> {
                // set symbol to use the pointSymbol
                pointSymbol
            }
            is Polyline -> {
                // set symbol to use the lineSymbol
                lineSymbol
            }
            is Polygon -> {
                // set symbol to use the fillSymbol
                fillSymbol
            }
            else -> {
                showError("Unknown geometry for convex hull")
                null
            }
        }
        // update the convex hull graphics geometry
        convexHullGraphic.geometry = convexHullGeometry
        // disable the create button until new input points are created
        createButton.isEnabled = false
    }


    /**
     * Resets the map by clearing any drawn points, graphics and disables all buttons
     */
    private fun resetMap() {
        // remove all the selected points
        inputPoints.clear()
        // remove the geometry for the point graphic and convex hull graphics
        pointGraphic.geometry = null
        convexHullGraphic.geometry = null
        // disable the buttons
        resetButton.isEnabled = false
        createButton.isEnabled = false
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

/**
 * Simple extension property that represents a blue color
 */
private val Color.Companion.blue
    get() = fromRgba(0, 0, 255)
