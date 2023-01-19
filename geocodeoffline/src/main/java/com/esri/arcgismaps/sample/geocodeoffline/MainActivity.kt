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

package com.esri.arcgismaps.sample.geocodeoffline

import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.layers.TileCache
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.LocatorTask
import com.arcgismaps.tasks.geocode.ReverseGeocodeParameters
import com.esri.arcgismaps.sample.geocodeoffline.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import android.R

import android.widget.AutoCompleteTextView

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    private val geocodeParameters: GeocodeParameters by lazy { GeocodeParameters() }

    private val reverseGeocodeParameters: ReverseGeocodeParameters by lazy { ReverseGeocodeParameters() }

    private val pinSymbol by lazy {
        createPinSymbol()
    }

    // create a graphics overlay
    private val graphicsOverlay by lazy { GraphicsOverlay() }

    // display the street of the tapped location
    private val titleTV by lazy {
        activityMainBinding.titleTV
    }

    // display the metro area of the tapped location
    private val descriptionTV by lazy {
        activityMainBinding.descriptionTV
    }

    private val addressSearchView by lazy {
        activityMainBinding.searchView.addressSearchView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        val tileCache = TileCache("$provisionPath/streetmap_SD.tpkx")
        lifecycleScope.launch {
            tileCache.load().onSuccess {
                // set the map's viewpoint to the tileCache's full extent
                val extent = tileCache.fullExtent
                    ?: return@launch showError("Error retrieving extent of the feature layer")
                mapView.setViewpoint(Viewpoint(extent))
            }.onFailure {
                showError("Error loading tileCache")
            }
        }

        // create a tiled layer and add it to as the base map
        val tiledLayer = ArcGISTiledLayer(tileCache)
        mapView.map = ArcGISMap(Basemap(tiledLayer))

        // Locator Task

        val locatorTaskZip = File(provisionPath, getString(R.string.san_diego_loc))
        val locatorTask = LocatorTask(locatorTaskZip.path)

        lifecycleScope.launch {
            locatorTask.load().onSuccess {
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { mapPoint -> findAddressReverseGeocode(mapPoint, locatorTask) }
                }
                setupAddressSearchView()
            }.onFailure {
                showError(it.message.toString())
            }
        }
    }

    private fun setupAddressSearchView() {
        // get the list of pre-made suggestions
        val suggestions = resources.getStringArray(R.array.suggestion_items)

        // set up parameters for searching with MatrixCursor
        val columnNames = arrayOf(BaseColumns._ID, "address")
        val suggestionsCursor = MatrixCursor(columnNames)

        // add each address suggestion to a new row
        suggestions.forEachIndexed { i, s -> suggestionsCursor.addRow(arrayOf(i, s)) }

        // column names for the adapter to look at when mapping data
        val cols = arrayOf("address")

        // ids that show where data should be assigned in the layout
        val to = intArrayOf(R.id.suggestionAddress)

            val suggestionsAdapter = SimpleCursorAdapter(
                this@MainActivity,
                R.layout.suggestion_address,
                suggestionsCursor,
                cols,
                to,
                0)
        addressSearchView.suggestionsAdapter = suggestionsAdapter
        addressSearchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                geocode(suggestions[position])
                return true
            }
        })

        // show the suggestions as soon as the user opens the search view
        findViewById<AutoCompleteTextView>(R.id.search_src_text).threshold = 0

    }

    /**
     * Use the locator task to geocode the given address.
     *
     * @param address as a string to geocode
     */
    private fun geocode(address: String) {

    }

    private suspend fun findAddressReverseGeocode(mapPoint: Point, locatorTask: LocatorTask) {

        val pinGraphic = Graphic(mapPoint, pinSymbol)
        graphicsOverlay.graphics.apply {
            clear()
            add(pinGraphic)
        }

        // normalize the geometry - needed if the user crosses the international date line.
        val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
        locatorTask.reverseGeocode(normalizedPoint).onSuccess {
            // get the first address
            val address = it.firstOrNull()
            if (address == null) {
                showError("Could not find address at tapped point")
                return@onSuccess
            }

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

    private fun createPinSymbol(): PictureMarkerSymbol {
        val pinDrawable = ContextCompat.getDrawable(
            this, R.drawable.baseline_location_pin_red_48
        )

        val pinSymbol = PictureMarkerSymbol(
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

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
