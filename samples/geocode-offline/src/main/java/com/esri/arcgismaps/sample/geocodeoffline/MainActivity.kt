/* Copyright 2023 Esri
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
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
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
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.esri.arcgismaps.sample.geocodeoffline.databinding.GeocodeOfflineActivityMainBinding
import com.esri.arcgismaps.sample.sampleslib.EdgeToEdgeCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : EdgeToEdgeCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: GeocodeOfflineActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.geocode_offline_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // display the metro area of the tapped location
    private val descriptionTV by lazy {
        activityMainBinding.descriptionTV
    }

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.geocode_offline_app_name)
    }

    // create a picture marker symbol
    private val pinSymbol: PictureMarkerSymbol by lazy {
        createPinSymbol()
    }

    // geocode parameters used to perform a search
    private val geocodeParameters: GeocodeParameters by lazy {
        GeocodeParameters().apply {
            // get all attributes
            resultAttributeNames.add("*")
            // get only the closest result
            maxResults = 1
        }
    }

    // locator task to provide geocoding services
    private val locatorTask: LocatorTask by lazy {
        LocatorTask(File(provisionPath, getString(R.string.san_diego_loc)).path)
    }

    // create a graphics overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // load the tile cache from local storage
        val tileCache = TileCache("$provisionPath/streetmap_SD.tpkx")
        // create a tiled layer and add it to as the base map
        val tiledLayer = ArcGISTiledLayer(tileCache)
        mapView.apply {
            map = ArcGISMap(Basemap(tiledLayer))
            // set map initial viewpoint
            map?.initialViewpoint = Viewpoint(32.72, -117.155, 120000.0)
            // add a graphics overlay to the map view
            graphicsOverlays.add(graphicsOverlay)
        }

        // load geocode locator task
        lifecycleScope.launch {
            locatorTask.load().onSuccess {
                mapView.onSingleTapConfirmed.collect { event ->
                    // find address with reverse geocode using the tapped location
                    event.mapPoint?.let { mapPoint -> findAddressReverseGeocode(mapPoint) }
                }
            }.onFailure {
                showError(it.message.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val search = menu.findItem(R.id.appSearchBar)
        val searchView = search.actionView as SearchView
        // set up address search view and listeners
        setupAddressSearchView(searchView)
        val surfaceContainerColor = getColor(com.esri.arcgismaps.sample.sampleslib.R.color.colorBackground)
        val onSurfaceColor = getColor(com.esri.arcgismaps.sample.sampleslib.R.color.textColor)
        searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text).apply {
            setTextColor(onSurfaceColor)
        }
        searchView.findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
            setBackgroundColor(surfaceContainerColor)
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets up the address SearchView and uses MatrixCursor to
     * show suggestions to the user as text is entered.
     */
    private fun setupAddressSearchView(addressSearchView: SearchView) {
        // disable threshold to show results from single character
        val autoCompleteTextViewID = resources.getIdentifier("search_src_text", "id", packageName)
        addressSearchView.findViewById<AutoCompleteTextView>(autoCompleteTextViewID).threshold = 0

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
        // define SimpleCursorAdapter
        val suggestionsAdapter = SimpleCursorAdapter(
            this@MainActivity,
            R.layout.suggestion_address, suggestionsCursor, cols, to, 0
        )

        addressSearchView.suggestionsAdapter = suggestionsAdapter
        // handle an address suggestion being chosen
        addressSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                // geocode the typed address
                addressSearchView.setQuery(suggestions[position], true)
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
        geocodeResults.ifEmpty {
            // no address found in geocode so return
            showError("No address found for $address")
            return@launch
        }
        // display address found in geocode
        displaySearchResultOnMap(geocodeResults)
    }

    /**
     * Get the reverse geocode result from the [mapPoint]
     */
    private suspend fun findAddressReverseGeocode(mapPoint: Point) {
        // normalize the geometry - needed if the user crosses the international date line.
        val normalizedPoint = GeometryEngine.normalizeCentralMeridian(mapPoint) as Point
        locatorTask.reverseGeocode(normalizedPoint).onSuccess { geocodeResults ->
            // no address found in geocode so return
            if (geocodeResults.isEmpty()) {
                showError("Could not find address at tapped point")
                return@onSuccess
            }
            displaySearchResultOnMap(geocodeResults)
        }.onFailure {
            showError(it.message.toString())
        }
    }

    /**
     * Turn the first address from [geocodeResultList] into a point marker and adds it to the graphic overlay of the map.
     */
    private fun displaySearchResultOnMap(geocodeResultList: List<GeocodeResult>) {
        // clear graphics overlay of existing graphics
        graphicsOverlay.graphics.clear()

        // create graphic object
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

    /**
     *  Creates a picture marker symbol from the pin icon.
     */
    private fun createPinSymbol(): PictureMarkerSymbol {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.pin) as BitmapDrawable
        val pinSymbol = PictureMarkerSymbol.createWithImage(pinDrawable)
        pinSymbol.apply {
            // resize the dimensions of the symbol
            width = 18f
            height = 65f
        }
        return pinSymbol
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
