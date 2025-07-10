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

import android.content.Context
import android.database.MatrixCursor
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.BaseColumns
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.esri.arcgismaps.sample.searchwithgeocode.databinding.SearchWithGeocodeActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: SearchWithGeocodeActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.search_with_geocode_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val addressTextView: TextView by lazy {
        activityMainBinding.addressTextView
    }

    private val extentSwitch: SwitchMaterial by lazy {
        activityMainBinding.extentSwitch
    }

    // create a locator task from an online service
    private val locatorTask: LocatorTask = LocatorTask(
        "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer"
    )

    // geocode parameters used to perform a search
    private val addressGeocodeParameters: GeocodeParameters = GeocodeParameters().apply {
        // get all attributes names for the geocode results
        resultAttributeNames.addAll(listOf("PlaceName", "Place_addr"))
    }

    // create a new Graphics Overlay
    private val graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    // instance of the map pin symbol
    private var pinSourceSymbol: PictureMarkerSymbol? = null

    // will search in the map view's viewpoint extent if enabled
    private var isSearchInExtent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        mapView.apply {
            // set the map to be displayed in the MapView
            map = ArcGISMap(BasemapStyle.ArcGISStreets)

            // set map initial viewpoint
            map?.initialViewpoint = Viewpoint(40.0, -100.0, 100000000.0)

            // define the graphics overlay and add it to the map view
            graphicsOverlays.add(graphicsOverlay)

            // set an on touch listener on the map view
            lifecycleScope.launch {
                onSingleTapConfirmed.collect { tapEvent ->
                    // identify the graphic at the tapped coordinate
                    val tappedGraphic = identifyGraphic(tapEvent.screenCoordinate)
                    if (tappedGraphic != null) {
                        // show the address of the identified graphic
                        showAddressForGraphic(tappedGraphic)
                    }
                }
            }
        }

        // once the map has loaded successfully, set up address finding UI
        lifecycleScope.launch {
            // load the map then set up UI
            mapView.map?.load()?.onSuccess {
                // create the pin symbol
                pinSourceSymbol = createPinSymbol()
            }?.onFailure {
                showError(it.message.toString())
            }
        }

        // set the switch to update the isSearchInExtent value
        extentSwitch.setOnCheckedChangeListener { _, isChecked -> isSearchInExtent = isChecked }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val search = menu.findItem(R.id.appSearchBar)
        // set up address search view and listeners
        setupAddressSearchView(search.actionView as SearchView)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets up the address SearchView and uses MatrixCursor to
     * show suggestions to the user as text is entered.
     */
    private fun setupAddressSearchView(addressSearchView: SearchView) {
        addressSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(address: String): Boolean {
                // geocode the typed address, search within map's viewpoint as keyword was submitted
                geocodeAddress(address, true)
                addressSearchView.clearAndHideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // if the newText string isn't empty, get suggestions from the locator task
                if (newText.isNotEmpty()) {
                    lifecycleScope.launch {
                        locatorTask.suggest(newText).onSuccess { suggestResults ->
                            // create a SimpleCursorAdapter and assign it to the suggestion adapter of the SearchView
                            val simpleCursorAdapter = createSimpleCursorAdapter()
                            addressSearchView.suggestionsAdapter = simpleCursorAdapter

                            // add each address suggestion to a new row
                            for ((key, result) in suggestResults.withIndex()) {
                                val suggestionCursor = simpleCursorAdapter.cursor as MatrixCursor
                                suggestionCursor.addRow(arrayOf<Any>(key, result.label))
                            }
                            // notify the adapter when the data updates, so the view can refresh itself
                            simpleCursorAdapter.notifyDataSetChanged()

                            // handle an address suggestion being chosen
                            addressSearchView.setOnSuggestionListener(object :
                                SearchView.OnSuggestionListener {
                                override fun onSuggestionSelect(position: Int): Boolean {
                                    return false
                                }

                                override fun onSuggestionClick(position: Int): Boolean {
                                    // get the selected row
                                    (simpleCursorAdapter.getItem(position) as? MatrixCursor)?.let { selectedRow ->
                                        // get the row's index
                                        val selectedCursorIndex =
                                            selectedRow.getColumnIndex("address")
                                        // get the string from the row at index and set it to query
                                        val selectedAddress =
                                            selectedRow.getString(selectedCursorIndex)
                                        addressSearchView.setQuery(selectedAddress, false)
                                        // geocode the typed address
                                        geocodeAddress(selectedAddress, false)
                                        addressSearchView.clearAndHideKeyboard()
                                    }
                                    return true
                                }
                            })
                        }.onFailure {
                            showError("Geocode suggestion error: ${it.message.toString()}")
                        }
                    }
                }
                return true
            }
        })
    }

    /**
     * Creates and returns a SimpleCursorAdapter.
     */
    private fun createSimpleCursorAdapter(): SimpleCursorAdapter {
        // set up parameters for searching with MatrixCursor
        val columnNames = arrayOf(BaseColumns._ID, "address")
        val suggestionsCursor = MatrixCursor(columnNames)
        // column names for the adapter to look at when mapping data
        val cols = arrayOf("address")
        // ids that show where data should be assigned in the layout
        val to = intArrayOf(R.id.suggestion_address)
        // define SimpleCursorAdapter
        return SimpleCursorAdapter(
            this@MainActivity,
            R.layout.suggestion, suggestionsCursor, cols, to, 0
        )
    }

    /**
     * Geocode an [address] passed in by the user.
     */
    private fun geocodeAddress(address: String, multipleResults: Boolean) = lifecycleScope.launch {
        // clear graphics on map before displaying search results
        graphicsOverlay.graphics.clear()

        // search the map view's extent if enabled
        if (isSearchInExtent)
            addressGeocodeParameters.searchArea =
                mapView.getCurrentViewpoint(ViewpointType.BoundingGeometry)?.targetGeometry
        else
            addressGeocodeParameters.searchArea = null

        // if locator task needs to find multiple results,
        // set maxResults to default to `6`.
        addressGeocodeParameters.maxResults = if (multipleResults) 6 else 1

        // load the locator task
        locatorTask.load().getOrThrow()

        // run the locatorTask geocode task, passing in the address
        val geocodeResults = locatorTask.geocode(address, addressGeocodeParameters).getOrThrow()
        // no address found in geocode so return
        when {
            geocodeResults.isEmpty() && isSearchInExtent -> {
                showError("Address not found in map's extent")
                return@launch
            }
            geocodeResults.isEmpty() && !isSearchInExtent -> {
                showError("No address found for $address")
                return@launch
            }
            // address found in geocode
            else -> displaySearchResultOnMap(geocodeResults)
        }

    }

    /**
     * Turns a list of [geocodeResultList] into a point markers and adds it to the graphic overlay of the map.
     */
    private fun displaySearchResultOnMap(geocodeResultList: List<GeocodeResult>) {
        // clear graphics overlay of existing graphics
        graphicsOverlay.graphics.clear()

        // create graphic object for each resulting location
        geocodeResultList.forEach { geocodeResult ->
            val resultLocationGraphic = Graphic(
                geocodeResult.displayLocation,
                geocodeResult.attributes, pinSourceSymbol
            )
            // add graphic to location layer
            graphicsOverlay.graphics.add(resultLocationGraphic)
        }

        when (geocodeResultList.size) {
            // if there is only one result, display location's address
            1 -> {
                val addressAttributes = geocodeResultList[0].attributes
                val addressString = SpannableStringBuilder()
                    .append("Selected address\n")
                    .bold { append("${addressAttributes["PlaceName"]} ${addressAttributes["Place_addr"]}") }
                addressTextView.text = addressString
            }
            // if there are multiple results, display tap pin message
            else -> addressTextView.text = getString(R.string.tap_on_pin_to_select_address)
        }

        // get the envelop to set the viewpoint
        val envelope = graphicsOverlay.extent ?: return showError("Geocode result extent is null")
        // animate viewpoint to geocode result's extent
        lifecycleScope.launch {
            mapView.setViewpointGeometry(envelope, 25.0)
        }
    }

    /**
     * Identifies the tapped graphic at the [screenCoordinate] and shows it's address.
     */
    private suspend fun identifyGraphic(screenCoordinate: ScreenCoordinate): Graphic? {
        // from the graphics overlay, get the graphics near the tapped location
        val identifyGraphicsOverlayResult = mapView.identifyGraphicsOverlay(
            graphicsOverlay,
            screenCoordinate,
            10.0,
            false
        ).getOrElse { throwable ->
            showError("Error with identifyGraphicsOverlay: ${throwable.message.toString()}")
            return null
        }

        // if not graphic selected, return
        if (identifyGraphicsOverlayResult.graphics.isEmpty()) {
            return null
        }

        // get the first graphic identified
        return identifyGraphicsOverlayResult.graphics[0]
    }

    /**
     *  Creates a picture marker symbol from the pin icon, and sets it to half of its original size.
     */
    private suspend fun createPinSymbol(): PictureMarkerSymbol {
        val pinDrawable = ContextCompat.getDrawable(this, R.drawable.pin) as BitmapDrawable
        val pinSymbol = PictureMarkerSymbol.createWithImage(pinDrawable)
        pinSymbol.load().getOrThrow()
        pinSymbol.width = 19f
        pinSymbol.height = 72f
        return pinSymbol
    }

    /**
     * Display the address for the tapped [identifiedGraphic] using the attribute values
     */
    private suspend fun showAddressForGraphic(identifiedGraphic: Graphic) {
        // get the non null value of the geometry
        val pinGeometry: Geometry = identifiedGraphic.geometry
            ?: return showError("Error retrieving geometry for tapped graphic")

        // set the viewpoint to the pin location
        mapView.apply {
            setViewpointGeometry(pinGeometry.extent)
            setViewpointScale(10e3)
        }

        // set the address text
        val addressAttributes = identifiedGraphic.attributes
        val addressString = SpannableStringBuilder()
            .append("Selected address\n")
            .bold { append("${addressAttributes["PlaceName"]} ${addressAttributes["Place_addr"]}") }
        addressTextView.text = addressString

    }

    fun SearchView.clearAndHideKeyboard() {
        // clear the searched text from the view
        this.clearFocus()
        // close the keyboard once search is complete
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
