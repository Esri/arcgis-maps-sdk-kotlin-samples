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

package com.esri.arcgismaps.sample.searchwithgeocode

import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.esri.arcgismaps.sample.searchwithgeocode.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val suggestionSpinner: Spinner by lazy {
        activityMainBinding.suggestionSpinner
    }

    private val addressSearchView: SearchView by lazy {
        activityMainBinding.addressSearchView
    }

    private var addressGeocodeParameters: GeocodeParameters = GeocodeParameters()

    // create a locator task from an online service
    private val locatorTask: LocatorTask =
        LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer")

    // create a picture marker symbol
    private var pinSourceSymbol: PictureMarkerSymbol? = null

    // create a new Graphics Overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a streets basemap style
        val streetsMap = ArcGISMap(BasemapStyle.ArcGISStreets)
        streetsMap.initialViewpoint = Viewpoint(40.0, -100.0, 100000000.0)
        // once the map has loaded successfully, set up address finding UI
        lifecycleScope.launch {
            streetsMap.load().getOrThrow()
            //TODO which one here?
            pinSourceSymbol = createPinSymbol()
            //initializeAddressFinding()
            setupAddressSearchView()
        }

        mapView.apply {
            // set the map to be displayed in the MapView
            map = streetsMap

            // define the graphics overlay and add it to the map view
            graphicsOverlays.add(graphicsOverlay)

            // set an on touch listener on the map view
            lifecycleScope.launch {
                onSingleTapConfirmed.collect { tapEvent ->
                    // identify the graphic at the tapped coordinate
                    identifyGraphic(tapEvent.screenCoordinate)
                }
            }
        }
    }

    /**
     * Populates the spinner with address suggestions and sets up the address search view.
     */
    private fun initializeAddressFinding() {
        TODO("Not yet implemented")
    }

    /**
     * Sets up the address SearchView and uses MatrixCursor to show suggestions to the user as text is entered.
     */
    private fun setupAddressSearchView() {
        addressGeocodeParameters.apply {
            // get place name and street address attributes
            resultAttributeNames.addAll(listOf("PlaceName", "Place_addr"))
            // return only the closest result
            maxResults = 1
        }

        addressSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(address: String): Boolean {
                // geocode typed address
                geoCodeTypedAddress(address)
                // clear focus from search views
                addressSearchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // if the newText string isn't empty, get suggestions from the locator task
                if (newText.isNotEmpty()) {
                    lifecycleScope.launch {
                        locatorTask.suggest(newText).onSuccess { suggestResults ->

                            // set up parameters for searching with MatrixCursor
                            val address = "address"
                            val columnNames = arrayOf(BaseColumns._ID, address)
                            val suggestionsCursor = MatrixCursor(columnNames)

                            // add each address suggestion to a new row
                            for ((key, result) in suggestResults.withIndex()) {
                                suggestionsCursor.addRow(arrayOf<Any>(key, result.label))
                            }
                            // column names for the adapter to look at when mapping data
                            val cols = arrayOf(address)
                            // ids that show where data should be assigned in the layout
                            val to = intArrayOf(R.id.suggestion_address)
                            // define SimpleCursorAdapter
                            val suggestionAdapter = SimpleCursorAdapter(
                                this@MainActivity,
                                R.layout.suggestion, suggestionsCursor, cols, to, 0
                            )

                            addressSearchView.suggestionsAdapter = suggestionAdapter
                            // handle an address suggestion being chosen
                            addressSearchView.setOnSuggestionListener(object :
                                SearchView.OnSuggestionListener {
                                override fun onSuggestionSelect(position: Int): Boolean {
                                    return false
                                }

                                override fun onSuggestionClick(position: Int): Boolean {
                                    // get the selected row
                                    (suggestionAdapter.getItem(position) as? MatrixCursor)?.let { selectedRow ->
                                        // get the row's index
                                        val selectedCursorIndex =
                                            selectedRow.getColumnIndex(address)
                                        // get the string from the row at index
                                        val selectedAddress =
                                            selectedRow.getString(selectedCursorIndex)
                                        // use clicked suggestion as query
                                        addressSearchView.setQuery(selectedAddress, true)
                                    }
                                    return true
                                }
                            })
                        }.onFailure {
                            showError("Geocode suggesstion error: ${it.message.toString()}")
                        }
                    }
                }
                return true
            }
        })
    }

    /**
     * Geocode an [address] passed in by the user.
     */
    private fun geoCodeTypedAddress(address: String) = lifecycleScope.launch {
        locatorTask.load().getOrThrow()

        // run the locatorTask geocode task, passing in the address
        val geocodeResults = locatorTask.geocode(address, addressGeocodeParameters).getOrThrow()

        // No address found in geocode so return
        if (geocodeResults.isEmpty()) {
            showError("No location with address: $address")
            return@launch
        }

        // Address found in geocode
        displaySearchResultOnMap(geocodeResults[0])

    }

    /**
     * Turns a [geocodeResult] into a Point and adds it to the graphic overlay of the map.
     */
    private fun displaySearchResultOnMap(geocodeResult: GeocodeResult) {
        // clear graphics overlay of existing graphics
        graphicsOverlay.graphics.clear()

        // create graphic object for resulting location
        val resultLocationGraphic = Graphic(
            geocodeResult.displayLocation,
            geocodeResult.attributes, pinSourceSymbol
        )

        // add graphic to location layer
        graphicsOverlay.graphics.add(resultLocationGraphic)

        // get the envelop to set the viewpoint
        val envelope = geocodeResult.extent
            ?: return showError("Geocode result extent is null")

        // animate viewpoint to geocode result's extent
        lifecycleScope.launch {
            mapView.setViewpointAnimated(
                Viewpoint(envelope),
                1f
            )
        }
    }

    /**
     * Identifies the tapped graphic at the [screenCoordinate] and shows it's address.
     */
    private suspend fun identifyGraphic(screenCoordinate: ScreenCoordinate) {
        // from the graphics overlay, get the graphics near the tapped location
        mapView.identifyGraphicsOverlay(
            graphicsOverlay,
            screenCoordinate,
            10.0,
            false
        ).onSuccess { identifyGraphicsOverlayResult ->
            // if not graphic selected, return
            if (identifyGraphicsOverlayResult.graphics.isEmpty()) {
                clearAddress()
                return@onSuccess
            }

            // get the first graphic identified
            val identifiedGraphic = identifyGraphicsOverlayResult.graphics[0]

            // show the address of the identified graphic
            showAddressForGraphic(identifiedGraphic)

        }.onFailure {
            showError("Error with identifyGraphicsOverlay: ${it.message.toString()}")
        }
    }

    /**
     *  Creates a picture marker symbol from the pin icon, and sets it to half of its original size.
     */
    private suspend fun createPinSymbol(): PictureMarkerSymbol {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.pin) as BitmapDrawable
        val pinSymbol = PictureMarkerSymbol(pinDrawable)
        pinSymbol.load().getOrThrow()
        pinSymbol.width = 19f
        pinSymbol.height = 72f
        return pinSymbol
    }

    private fun clearAddress() {
        //TODO("Not yet implemented")
    }

    private fun showAddressForGraphic(identifiedGraphic: Graphic) {
        //TODO("Not yet implemented")
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
