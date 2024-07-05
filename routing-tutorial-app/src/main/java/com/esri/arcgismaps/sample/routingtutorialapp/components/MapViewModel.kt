/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.routingtutorialapp.components

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.routingtutorialapp.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MapViewModel(private var application: Application, locationDisplay: LocationDisplay) :
    AndroidViewModel(application) {

    val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude = 34.0539, longitude = -118.2453, scale = 200000.0
        )
    }
    val snackbarHostState = SnackbarHostState()
    val directionsList by lazy { mutableStateListOf("") }
    val stopsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    val routeOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    val mapViewProxy = MapViewProxy()
    var startAddress by mutableStateOf("")
    var destinationAddress by mutableStateOf("")
    var travelTime by mutableStateOf("")
    var travelDistance by mutableStateOf("")
    var mapLoading by mutableStateOf(false)
    var showBottomSheet by mutableStateOf(false)
    var isQuickestButtonEnabled by mutableStateOf(false)
    var isShortestButtonEnabled by mutableStateOf(false)
    var isStartAddressTextFieldEnabled by mutableStateOf(true)
    var isDestinationAddressTextFieldEnabled by mutableStateOf(true)
    private val routeStops by lazy { mutableListOf<Stop>() }
    private var currentJob by mutableStateOf<Job?>(null)
    private var routeParameters: RouteParameters? = null
    private val routeTask = RouteTask(
        url = "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
    )

    /**
     * Performing the following during the app's loading process.
     *
     */
    init {
        currentJob = viewModelScope.launch {
            map.load().onSuccess {

                // Map has loaded
                mapLoading = true

                // Pan to user's current location
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

                // Set route parameters
                routeParameters = routeTask.createDefaultParameters().getOrThrow()
            }.onFailure {
                showMessage("map failed to load")
            }
        }
    }

    fun onGetCurrentLocationButtonClicked(locationDisplay: LocationDisplay) {
        viewModelScope.launch {

            // Look for user's current location
            locationDisplay.dataSource.start()

            // Pan to the user's current location
            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

            // Start address value is now "Your location" and is read only.
            startAddress = "Your location"
            isStartAddressTextFieldEnabled = false
        }
    }

    fun onRefreshButtonClicked(locationDisplay: LocationDisplay) {

        // Hide bottom sheet if it was visible
        if (showBottomSheet) {
            showBottomSheet = false
        }

        // Reset address values
        startAddress = ""
        destinationAddress = ""

        // Enable the route options
        isQuickestButtonEnabled = false
        isShortestButtonEnabled = false

        // Make the addresses editable
        isStartAddressTextFieldEnabled = true
        isDestinationAddressTextFieldEnabled = true

        // Remove any previous data about routes and stops
        clearRouteAndStops()

        // Pan to user's location
        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

    }

    fun onSearchRouteButtonClicked(
        locationDisplay: LocationDisplay
    ) {

        if (startAddress == "" || destinationAddress == "") {
            return showMessage("Please enter the start and/or destination address(es)")
        }
        // Hide bottom sheet with any previous directions
        if (showBottomSheet) {
            showBottomSheet = false
        }

        // Ensure there's no ongoing job before starting a new one
        currentJob?.cancel()

        // Launch a new coroutine for the route finding process
        currentJob = viewModelScope.launch {

            // Add current user's location as a stop if user used default argument for the start address
            if (startAddress == "Your location") {
                // Add current user's location as a stop
                val currentLocation = locationDisplay.mapLocation
                if (currentLocation != null) {
                    val currentLocationStop = Stop(currentLocation)
                    routeStops.add(currentLocationStop)

                } else {
                    showMessage("Failed to obtain current location")
                }
            }

            // Find route between starting address and destination locations
            findRoute()
        }
    }

    /**
     * Once a start address is entered, it's searched, and its textField becomes read only
     *
     */
    fun onSearchStartingAddress() {

        viewModelScope.launch {

            // Skipping searching for a location if it is current user's location.
            if (startAddress != "Your location") {
                isStartAddressTextFieldEnabled = false
                // Search for starting address
                searchAddress(
                    startAddress, true
                )
            }
        }


    }

    /**
     * Once a destination address is entered, it's searched, and its textField becomes read only
     *
     */
    fun onSearchDestinationAddress() {
        viewModelScope.launch {
            // Search for destination address
            isDestinationAddressTextFieldEnabled = false
            searchAddress(
                destinationAddress, false
            )
        }
    }

    /**
     * Once a destination address is entered, it's searched, and its textField becomes read only
     *
     */
    fun checkPermissions(): Boolean {
        // Check permissions to see if both permissions are granted.
        // Coarse location permission.
        val permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(
            application, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        // Fine location permission.
        val permissionCheckFineLocation = ContextCompat.checkSelfPermission(
            application, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return permissionCheckCoarseLocation && permissionCheckFineLocation
    }

    /**
     * Used when "Quickest" button is clicked.
     *
     */
    fun findQuickestRoute() {

        // Set the route parameters to finding quickest route.
        routeParameters?.travelMode = routeTask.getRouteTaskInfo().travelModes[0]

        // Clear any previous route
        routeOverlay.graphics.clear()

        // Find the route
        viewModelScope.launch {
            findRoute()
        }

        // Disable button, will only be enabled if the Refresh button is clicked or restart the app
        isQuickestButtonEnabled = false
        isShortestButtonEnabled = false
    }

    /**
     * Used when "Shortest" button is clicked.
     *
     */
    fun findShortestRoute() {

        // Set the route parameters to finding shortest route.
        routeParameters?.travelMode = routeTask.getRouteTaskInfo().travelModes[1]

        // Clear any previous route
        routeOverlay.graphics.clear()

        // Find the route
        viewModelScope.launch {
            findRoute()
        }
        // Disable button, will only be enabled if the Refresh button is clicked or restart the app
        isShortestButtonEnabled = false
        isQuickestButtonEnabled = false
    }

    /**
     * Pinpoint the given query and pin it on the map.
     *
     */
    private suspend fun searchAddress(
        query: String, isStartAddress: Boolean
    ) {
        snackbarHostState.showSnackbar("locating $query...")

        val geocodeServerUri =
            "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
        val locatorTask = LocatorTask(geocodeServerUri)

        // Create geocode parameters
        val geocodeParameters = GeocodeParameters().apply {
            resultAttributeNames.add("*")
            maxResults = 1
        }

        // Search for the address
        locatorTask.geocode(searchText = query, parameters = geocodeParameters)
            .onSuccess { geocodeResults: List<GeocodeResult> ->
                if (geocodeResults.isNotEmpty()) {

                    // Find the location and add it as a stop
                    val geocodeResult = geocodeResults[0]
                    addStop(isStartAddress, geocodeResult)

                    viewModelScope.launch {
                        val centerPoint =
                            geocodeResult.displayLocation ?: return@launch showMessage(
                                "The locatorTask.geocode() call failed"
                            )


                        // Animate the map view to the center point.
                        mapViewProxy.setViewpointAnimated(
                            Viewpoint(centerPoint), duration = 0.5.seconds
                        ).onFailure { error ->
                            println("Failed to set Viewpoint center: ${error.message}")
                        }
                    }
                } else {
                    showMessage("No address found for the given query")
                }
            }.onFailure { error ->
                showMessage("The locatorTask.geocode() call failed: ${error.message}")
            }
    }

    /**
     * Find a route between starting and destination address
     *
     */
    private suspend fun findRoute() {
        snackbarHostState.showSnackbar("Routing...")

        // Set router parameters
        routeParameters?.setStops(routeStops)
        routeParameters?.returnDirections = true

        val routeResult = routeParameters?.let { routeTask.solveRoute(it) }
        if (routeResult != null) {
            routeResult.onFailure {
                showMessage("No route solution. ${it.message}")
                routeOverlay.graphics.clear()
            }.onSuccess {

                // Enabling the Quickest/Shortest route options
                isShortestButtonEnabled = true
                isQuickestButtonEnabled = true
                // Get the first solved route result
                val route = it.routes[0]

                // Create graphic for route
                val routeGraphic = Graphic(
                    geometry = route.routeGeometry, symbol = SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Solid, color = Color.cyan, width = 2f
                    )
                )

                // Add the route layout to the map
                routeOverlay.graphics.add(routeGraphic)

                // Clear any previous directions and add new directions
                directionsList.clear()
                route.directionManeuvers.forEach { directionManeuver: DirectionManeuver ->
                    directionsList.add(directionManeuver.directionText)
                }

                // Only show bottom sheet if there are directions
                if (directionsList.isNotEmpty()) {
                    showBottomSheet = true
                }

                // Get time and distance for the route
                val travelDuration = route.travelTime.roundToInt().toDuration(DurationUnit.MINUTES)
                travelTime = travelDuration.toString()
                travelDistance = "%.2f".format(
                    route.totalLength * 0.000621371192 // convert meters to miles and round 2 decimals
                )

                // Animate to show the stops and the route
                currentJob = viewModelScope.launch {
                    val routeExtent = route.routeGeometry?.extent

                    // Create a Viewpoint from the route's extent.
                    val viewpoint = routeExtent?.let { Viewpoint(it) }

                    // Animate the map view to the new viewpoint
                    if (viewpoint != null) {
                        mapViewProxy.setViewpointAnimated(viewpoint, duration = 0.25.seconds)
                            .onFailure { error ->
                                println("Failed to set Viewpoint: ${error.message}")
                            }
                    }
                }
            }
        }
    }

    /**
     * Clear any data and graphic about routes and stops
     *
     */
    private fun clearRouteAndStops() {
        // Remove all stop points
        stopsOverlay.graphics.clear()

        // Remove current route
        routeOverlay.graphics.clear()

        // Clear the list of stops
        routeStops.clear()

        // Clear list of directions
        directionsList.clear()
    }

    /**
     * Add both the stop to routeStops and adds the graphic marker for that stop
     *
     */
    private fun addStop(isStartAddress: Boolean, geocodeResult: GeocodeResult) {

        // Create the graphic for the stop
        val markerGraphic = createMarkerGraphic(isStartAddress, geocodeResult)

        // Add the geocoded point as a stop
        val stop = geocodeResult.displayLocation?.let { Stop(it) }
        if (stop != null) {
            routeStops.add(stop)
        }

        // Add the stop to the map
        stopsOverlay.graphics.add(markerGraphic)

    }

    /**
     * Create a graphic dependent on the address type.
     *
     */
    private fun createMarkerGraphic(
        isStartAddress: Boolean,
        geocodeResult: GeocodeResult,
    ): Graphic {

        // Create the graphic depending on the address type
        val drawable = if (isStartAddress && startAddress != "Your location") {
            ContextCompat.getDrawable(application, R.drawable.ic_start) as BitmapDrawable
        } else {
            ContextCompat.getDrawable(application, R.drawable.ic_destination) as BitmapDrawable
        }

        val pinSourceSymbol = PictureMarkerSymbol.createWithImage(drawable).apply {
            width = 30f
            height = 30f
            offsetY = 20f
        }

        return Graphic(
            geometry = geocodeResult.displayLocation,
            attributes = geocodeResult.attributes,
            symbol = pinSourceSymbol
        )

    }

    /**
     * Used on both MapViewMode.kt and MainScreen.kt to display any errors or messages
     *
     */
    fun showMessage(
        message: String
    ) {
        Toast.makeText(application, message, Toast.LENGTH_LONG).show()
    }

}

