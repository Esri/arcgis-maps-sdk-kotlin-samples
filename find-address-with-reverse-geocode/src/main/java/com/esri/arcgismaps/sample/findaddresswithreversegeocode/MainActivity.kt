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

package com.esri.arcgismaps.sample.findaddresswithreversegeocode

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask

import com.esri.arcgismaps.sample.findaddresswithreversegeocode.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // create a MapView using binding
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // display the city of the tapped location
    private val titleTV by lazy {
        activityMainBinding.titleTV
    }

    // display the metro area of the tapped location
    private val descriptionTV by lazy {
        activityMainBinding.descriptionTV
    }

    // create a graphics overlay
    private val graphicsOverlay = GraphicsOverlay()

    // service url to be provided to the LocatorTask (geocoder)
    private val geocodeServer =
        "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"

    // locator task to provide geocoding services
    private val locatorTask = LocatorTask(geocodeServer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        mapView.apply {
            // add a map with a imagery basemap style
            map = ArcGISMap(BasemapStyle.ArcGISImagery)
            // add a graphics overlay to the map for showing where the user tapped
            graphicsOverlays.add(graphicsOverlay)
            // set initial viewpoint
            setViewpoint(Viewpoint(34.058, -117.195, 5e4))
        }

        lifecycleScope.launch {
            locatorTask.load().getOrElse {
                showError(it.message.toString())
            }
            // locator task loaded, look for geo view tapped
            geoViewTapped(mapView.onSingleTapConfirmed)
        }
    }

    /**
     * Displays a pin of the tapped location using [onSingleTapConfirmed]
     * and finds address with reverse geocode
     */
    private suspend fun geoViewTapped(onSingleTapConfirmed: SharedFlow<SingleTapConfirmedEvent>) {
        // clear existing graphics
        graphicsOverlay.graphics.clear()

        //add a graphic for the tapped point
        val pinSymbol = PictureMarkerSymbol(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_baseline_pin_24
            ) as BitmapDrawable
        )
        pinSymbol.apply {
            // resize the dimensions of the symbol
            width = 60f
            height = 60f
            // the image is a pin so offset the image so that the pinpoint
            // is on the point rather than the image's true center.
            leaderOffsetX = 30f
            offsetY = 14f
        }
        // collect map tapped event
        onSingleTapConfirmed.collect { event ->
            // get map point tapped, return if null
            val mapPoint = event.mapPoint ?: return@collect
            // add a graphic for the tapped point
            val pinGraphic = Graphic(mapPoint, pinSymbol)
            graphicsOverlay.graphics.add(pinGraphic)
            // normalize the geometry - needed if the user crosses the international date line.
            val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
            // reverse geocode to get address
            val addresses = locatorTask.reverseGeocode(normalizedPoint).getOrElse {
                showError(it.message.toString())
            } as List<GeocodeResult>
            // get the first result
            val address = addresses.first()
            // use the city and region for the title
            val title = address.attributes["Address"].toString()
            // use the metro area for the description details
            val description = "${address.attributes["City"]} " +
                    "${address.attributes["Region"]} " +
                    "${address.attributes["CountryCode"]}"
            // set the strings to the text views
            titleTV.text = title
            descriptionTV.text = description
        }
    }


    private fun showError(errorMessage: String) {
        Log.e(TAG, errorMessage)
        Snackbar.make(mapView, errorMessage, Snackbar.LENGTH_SHORT).show()
    }
}

