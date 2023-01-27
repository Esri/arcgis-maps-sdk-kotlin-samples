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

package com.esri.arcgismaps.sample.projectgeometry

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.*
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.esri.arcgismaps.sample.projectgeometry.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // setup the data binding for the MapView
    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    // shows the projection information as a TextView
    private val infoTextView: TextView by lazy {
        activityMainBinding.infoTextView
    }

    // setup the red pin marker image as bitmap drawable
    private val markerDrawable: BitmapDrawable by lazy {
        // load the bitmap from resources and create a drawable
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pin_symbol)
        BitmapDrawable(resources, bitmap)
    }

    // setup the red pin marker as a Graphic
    private val markerGraphic: Graphic by lazy {
        // creates a symbol from the marker drawable
        Graphic().apply {
            symbol = PictureMarkerSymbol(markerDrawable).apply {
                // resize the symbol into a smaller size
                width = 30f
                height = 30f
                // offset in +y axis so the marker spawned
                // is right on the touch point
                offsetY = 25f
            }
        }
    }

    // creates a graphic overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // configure mapView assignments
        mapView.apply {
            // create and add a map with a navigation night basemap style
            map = ArcGISMap(BasemapStyle.ArcGISNavigationNight)
            // add the graphics overlay to display marker graphics
            graphicsOverlays.add(graphicsOverlay)
            // set the default viewpoint to Redlands,CA
            setViewpoint(Viewpoint(34.058, -117.195, 5e4))
        }

        lifecycleScope.launch {
            // check if the map has loaded successfully
            mapView.map?.load()?.onSuccess {
                // capture and collect when the user taps on the screen
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { point -> geoViewTapped(point) }
                }
            }?.onFailure {
                // if map load failed, show the error
                showError("Error Loading Map", mapView)
            }
        }
    }

    /**
     * Handles the SingleTapEvent by drawing a marker and performs a Spatial reference transformation
     * of the tapped Location using GeometryEngine and displays the result
     */
    private fun geoViewTapped(point: Point) {
        // show the marker where the user tapped on the map
        graphicsOverlay.graphics.apply {
            // this removes previously added markers
            clear()
            // add a new marker graphic
            add(markerGraphic.apply {
                // updates the geometry location to the current tap
                geometry = point
            })
        }
        // project the web mercator location into a WGS84
        val projectedPoint = GeometryEngine.project(point, SpatialReference.wgs84())
        // build and display the projection result as a string
        infoTextView.text = getString(
            R.string.projection_info_text,
            point.toDisplayFormat(),
            projectedPoint.toDisplayFormat()
        )
    }

    /**
     * Extension function for the Point type that returns
     * a float-precision formatted string suitable for display
     */
    private fun Point.toDisplayFormat() =
        "${String.format("%.5f", x)}, ${String.format("%.5f", y)}"

    /**
     * Displays an error onscreen
     */
    private fun showError(message: String, view: View) {
        Log.e(TAG, message)
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}