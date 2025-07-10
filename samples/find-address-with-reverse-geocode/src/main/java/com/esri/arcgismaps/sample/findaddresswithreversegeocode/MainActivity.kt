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
import androidx.activity.enableEdgeToEdge
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
import com.arcgismaps.tasks.geocode.LocatorTask
import com.esri.arcgismaps.sample.findaddresswithreversegeocode.databinding.FindAddressWithReverseGeocodeActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // service url to be provided to the LocatorTask (geocoder)
    private val geocodeServer =
        "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"

    // set up data binding for the activity
    private val activityMainBinding: FindAddressWithReverseGeocodeActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.find_address_with_reverse_geocode_activity_main)
    }

    // create a MapView using binding
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // display the street of the tapped location
    private val titleTV by lazy {
        activityMainBinding.titleTV
    }

    // display the metro area of the tapped location
    private val descriptionTV by lazy {
        activityMainBinding.descriptionTV
    }

    // set the pin graphic for tapped location
    private val pinSymbol by lazy {
        createPinSymbol()
    }

    // create a graphics overlay
    private val graphicsOverlay = GraphicsOverlay()

    // locator task to provide geocoding services
    private val locatorTask = LocatorTask(geocodeServer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
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
            // load geocode locator task
            locatorTask.load().onSuccess {
                // locator task loaded, look for geo view tapped
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { mapPoint -> geoViewTapped(mapPoint) }
                }
            }.onFailure {
                showError(it.message.toString())
            }
        }
    }

    /**
     * Displays a pin of the tapped location using [mapPoint]
     * and finds address with reverse geocode
     */
    private suspend fun geoViewTapped(mapPoint: Point) {
        // create graphic for tapped point
        val pinGraphic = Graphic(mapPoint, pinSymbol)
        graphicsOverlay.graphics.apply {
            // clear existing graphics
            clear()
            // add the pin graphic
            add(pinGraphic)
        }
        // normalize the geometry - needed if the user crosses the international date line.
        val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
        // reverse geocode to get address
        locatorTask.reverseGeocode(normalizedPoint).onSuccess { addresses ->
            // get the first result
            val address = addresses.firstOrNull()
            if (address == null) {
                showError("Could not find address at tapped point")
                return@onSuccess
            }
            // use the street and region for the title
            val title = address.attributes["Address"].toString()
            // use the metro area for the description details
            val description = "${address.attributes["City"]} " +
                    "${address.attributes["Region"]} " +
                    "${address.attributes["CountryCode"]}"
            // set the strings to the text views
            titleTV.text = title
            descriptionTV.text = description
        }.onFailure {
            showError(it.message.toString())
        }
    }

    /**
     * Create a picture marker symbol to represent a pin at the tapped location
     */
    private fun createPinSymbol(): PictureMarkerSymbol {
        // get pin drawable
        val pinDrawable = ContextCompat.getDrawable(
            this,
            R.drawable.baseline_location_pin_red_48
        )
        //add a graphic for the tapped point
        val pinSymbol = PictureMarkerSymbol.createWithImage(
            pinDrawable as BitmapDrawable
        )
        pinSymbol.apply {
            // resize the dimensions of the symbol
            width = 50f
            height = 50f
            // the image is a pin so offset the image so that the pinpoint
            // is on the point rather than the image's true center
            leaderOffsetX = 30f
            offsetY = 25f
        }
        return pinSymbol
    }

    private fun showError(errorMessage: String) {
        Log.e(localClassName, errorMessage)
        Snackbar.make(mapView, errorMessage, Snackbar.LENGTH_SHORT).show()
    }
}
