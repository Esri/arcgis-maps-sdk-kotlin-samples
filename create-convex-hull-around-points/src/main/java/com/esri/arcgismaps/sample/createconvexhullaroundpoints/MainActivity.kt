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
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.createconvexhullaroundpoints.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val createButton by lazy {
        activityMainBinding.createButton
    }

    private val resetButton by lazy {
        activityMainBinding.resetButton
    }

    private val pointGraphic by lazy {
        val pointSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)
        Graphic(symbol = pointSymbol)
    }

    private val convexHullGraphic by lazy {
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 3f)
        Graphic(symbol = lineSymbol)
    }

    private val graphicsOverlay = GraphicsOverlay()

    private val inputPoints = mutableListOf<Point>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // add all the our graphics to the graphics overlay
        graphicsOverlay.graphics.addAll(listOf(pointGraphic, convexHullGraphic))

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // configure map view assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to the mapview
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // if the map load fails show the error and return
            map.load().onFailure {
                showError("Error loading map")
                return@launch
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
        createButton.enable()
        resetButton.enable()
    }

    /**
     * Creates and draws a convex hull graphic on the map using [pointGeometry] points
     */
    private fun createConvexHull(pointGeometry: Geometry) {
        // normalize the geometry for panning beyond the meridian
        // and check and proceed if the resulting geometry is not null
        GeometryEngine.normalizeCentralMeridian(pointGeometry)?.let { normalizedPointGeometry ->
            // create a convex hull from the points
            GeometryEngine.convexHull(normalizedPointGeometry)?.let { convexHull ->
                // if convex hull is not null then update the graphic's geometry
                convexHullGraphic.geometry = convexHull
                // disable the create button until new input points are created
                createButton.disable()
            }
        }
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
        // disabling the buttons
        resetButton.disable()
        createButton.disable()
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

/**
 * Simple extension function to enable a button, if not already enabled
 */
private fun Button.enable() {
    if (!isEnabled) isEnabled = true
}

/**
 * Simple extension function to disable a button, if not already disabled
 */
private fun Button.disable() {
    if (isEnabled) isEnabled = false
}
