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

package com.esri.arcgismaps.sample.showgeodesicpathbetweentwopoints

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.*
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.showgeodesicpathbetweentwopoints.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // set up data binding for the mapView
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // shows the distance information as a textview
    private val infoTextView by lazy {
        activityMainBinding.infoTextView
    }

    // starting location for the distance calculation
    private val startingPoint by lazy {
        Point(-73.7781, 40.6413, SpatialReference.wgs84())
    }

    // a blue marker symbol for the location points
    private val locationMarkerSymbol by lazy {
        SimpleMarkerSymbol(
            SimpleMarkerSymbolStyle.Circle,
            Color.blue,
            10f
        )
    }

    // the actual marker graphic for the starting location
    private val startingLocationMarkerGraphic by lazy {
        Graphic().apply {
            symbol = locationMarkerSymbol
            geometry = startingPoint
        }
    }

    // marker graphic for the destination, location information
    // populated on tap
    private val endLocationMarkerGraphic by lazy {
        Graphic().apply {
            symbol = locationMarkerSymbol
        }
    }

    // the geodesic path represented as line graphic
    private val geodesicPathGraphic by lazy {
        val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.blue)
        Graphic().apply {
            symbol = lineSymbol
        }
    }

    // the unit of distance measurement in kilometers
    private val unitsOfMeasurement by lazy {
        LinearUnit(LinearUnitId.Kilometers)
    }

    // creates a graphic overlay to draw all graphics
    private val graphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISImageryStandard)
        // configure mapView assignments
        mapView.apply {
            this.map = map
            // add the graphics overlay to display marker graphics
            graphicsOverlays.add(graphicsOverlay)
        }

        // add all our marker graphics to the graphics overlay
        graphicsOverlay.graphics.add(startingLocationMarkerGraphic)
        graphicsOverlay.graphics.add(endLocationMarkerGraphic)
        graphicsOverlay.graphics.add(geodesicPathGraphic)

        lifecycleScope.launch {
            // check if the map has loaded successfully
            map.load().onSuccess {
                // capture and collect when the user taps on the screen
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { point -> onGeoViewTapped(point) }
                }
            }.onFailure {
                // if map load failed, show the error
                showError("Error Loading Map", mapView)
            }
        }
    }

    /**
     * Handles the SingleTapEvent by drawing the destination location marker
     * and a geodesic path curve
     * using GeometryEngine.densifyGeodetic and computes the distance using
     * GeometryEngine.lengthGeodetic
     */
    private fun onGeoViewTapped(point: Point) {
        // project the tapped point into the same spatial reference
        // as the source point
        val destinationPoint = GeometryEngine.project(point, SpatialReference.wgs84())

        // check if the destination point is within the map bounds
        // isEmpty returns true if out of bounds
        if (!destinationPoint.isEmpty) {
            // update the end location marker location on map
            endLocationMarkerGraphic.geometry = destinationPoint
            // create a poly between source and destination points
            val polyline = Polyline(listOf(startingPoint, destinationPoint))
            // generate a geodesic curve using the polyline
            GeometryEngine.densifyGeodetic(
                polyline,
                1.0,
                unitsOfMeasurement,
                GeodeticCurveType.Geodesic
                // only compute the distance if the returned curved path geometry
                // is not null
            )?.let { pathGeometry ->
                // update the path graphic
                geodesicPathGraphic.geometry = pathGeometry
                // compute the path distance in kms
                val distance =
                    GeometryEngine.lengthGeodetic(
                        pathGeometry,
                        unitsOfMeasurement,
                        GeodeticCurveType.Geodesic
                    )
                // display the distance result
                infoTextView.text =
                    getString(R.string.distance_info_text, distance.roundToInt())
            }
        }
    }

    /**
     * Displays an error onscreen
     */
    private fun showError(message: String, view: View) {
        Log.e(TAG, message)
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}

/**
 * Extension property for the Color type that represents a
 * blue color
 */
private val Color.Companion.blue
    get() = fromRgba(0, 0, 255)
