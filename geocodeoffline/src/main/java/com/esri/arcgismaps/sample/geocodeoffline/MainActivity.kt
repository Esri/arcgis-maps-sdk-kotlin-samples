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
import android.view.Menu
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
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.layers.TileCache
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.esri.arcgismaps.sample.geocodeoffline.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

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

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    private val pinSymbol by lazy {
        createPinSymbol()
    }

    // create a graphics overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    private val geocodeParameters: GeocodeParameters by lazy {
        GeocodeParameters().apply {
            // get all attributes
            resultAttributeNames.add("*")
            // get only the closest result
            maxResults = 1
        }
    }

    // Locator Task
//    private val locatorTaskZip = File(provisionPath, getString(R.string.san_diego_loc))
    private val locatorTask: LocatorTask by lazy {
        LocatorTask(File(provisionPath, getString(R.string.san_diego_loc)).path)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        val tileCache = TileCache("$provisionPath/streetmap_SD.tpkx")
//        lifecycleScope.launch {
//            tileCache.load().onSuccess {
//                // set the map's viewpoint to the tileCache's full extent
//                val extent = tileCache.fullExtent
//                    ?: return@launch showError("Error retrieving extent of the feature layer")
//                mapView.setViewpoint(Viewpoint(extent))
//            }.onFailure {
//                showError("Error loading tileCache")
//            }
//        }

        // create a tiled layer and add it to as the base map
        val tiledLayer = ArcGISTiledLayer(tileCache)
        mapView.map = ArcGISMap(Basemap(tiledLayer))
        // set map initial viewpoint
        mapView.map?.initialViewpoint = Viewpoint(32.72, -117.155, 120000.0)

        // add a graphics overlay to the map view
        mapView.graphicsOverlays.add(graphicsOverlay)

//        // Locator Task
//
//        val locatorTaskZip = File(provisionPath, getString(R.string.san_diego_loc))
//        val locatorTask = LocatorTask(locatorTaskZip.path)

        lifecycleScope.launch {
            locatorTask.load().onSuccess {
                mapView.onSingleTapConfirmed.collect { event ->
                    event.mapPoint?.let { mapPoint -> findAddressReverseGeocode(mapPoint, locatorTask) }
                }
            }.onFailure {
                showError(it.message.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val search = menu.findItem(R.id.appSearchBar)
        // set up address search view and listeners
        setupAddressSearchView(search.actionView as SearchView)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupAddressSearchView(addressSearchView: SearchView) {
        // get the list of pre-made suggestions
        val suggestions = resources.getStringArray(R.array.suggestion_items)
        // set up parameters for searching with MatrixCursor
        val columnNames = arrayOf(BaseColumns._ID, "address")
        val suggestionsCursor = MatrixCursor(columnNames)
        // add each address suggestion to a new row
        suggestions.forEachIndexed { i, s -> suggestionsCursor.addRow(arrayOf(i, s)) }

//        // show the suggestions as soon as the user opens the search view
//        findViewById<AutoCompleteTextView>(R.id.search_src_text).threshold = 0

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
                geocodeAddress(suggestions[position])
                addressSearchView.clearFocus()
                return true
            }
        })

        // geocode the searched address on submit
        addressSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(address: String): Boolean {
                geocodeAddress(address)
                addressSearchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?) = true
        })
    }

    /**
     * Use the locator task to geocode the given address.
     *
     * @param address as a string to geocode
     */
    private fun geocodeAddress(address: String) = lifecycleScope.launch {

        // clear graphics on map before displaying search results
        graphicsOverlay.graphics.clear()

        // load the locator task
        locatorTask.load().getOrThrow()

        // run the locatorTask geocode task, passing in the address
        val geocodeResults = locatorTask.geocode(address, geocodeParameters).getOrThrow()
        // no address found in geocode so return
        if(geocodeResults.isEmpty()) {
                showError("No address found for $address")
                return@launch
            }
            // address found in geocode
            else displaySearchResultOnMap(geocodeResults)
    }

    /**
     * Turns a list of [geocodeResultList] into a point markers and adds it to the graphic overlay of the map.
     */
    private fun displaySearchResultOnMap(geocodeResultList: List<GeocodeResult>) {
        // clear graphics overlay of existing graphics
        graphicsOverlay.graphics.clear()

        val resultLocationGraphic = Graphic(
            geocodeResultList[0].displayLocation,
            geocodeResultList[0].attributes, pinSymbol
        )
        graphicsOverlay.graphics.add(resultLocationGraphic)

        descriptionTV.text = geocodeResultList[0].label

        // get the envelop to set the viewpoint
        val envelope = graphicsOverlay.extent ?: return showError("Geocode result extent is null")
        // animate viewpoint to geocode result's extent
        lifecycleScope.launch {
            mapView.setViewpointGeometry(envelope, 25.0)
        }

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
