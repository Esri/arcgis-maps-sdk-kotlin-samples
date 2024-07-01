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
import android.content.Context
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

class MapViewModel(application: Application, locationDisplay: LocationDisplay) : AndroidViewModel(application) {

    val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude =  34.0539,
            longitude =  -118.2453,
            scale = 200000.0
        )
    }
    val directionsList by lazy { mutableStateListOf("") }
    val stopsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    val routeOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    val mapViewProxy =  MapViewProxy()
    var startAddress by mutableStateOf("Your location")
    var destinationAddress by mutableStateOf("irvine, california")
    var travelTime by mutableStateOf("")
    var travelDistance by mutableStateOf("")
    var mapLoading by mutableStateOf(false)
    var showBottomSheet by  mutableStateOf(false)
    var isQuickestChecked by mutableStateOf(false)
    var isShortestChecked by mutableStateOf(false)
    private val routeStops by lazy {mutableListOf<Stop>()}
    private var currentJob by mutableStateOf<Job?>(null)
    private var routeParameters: RouteParameters? = null
    private val routeTask = RouteTask(
        url = "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
    )

    // Performing the following during the app's loading process.
    init {
        viewModelScope.launch {
            map.load().onSuccess {

                // Map has loaded
                mapLoading = true

                // Pan to user's current location
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)

                // Set route parameters
                routeParameters = routeTask.createDefaultParameters().getOrThrow()
            }.onFailure {
                showMessage(application, "map failed to load")
            }
        }
    }

    // This function is used when the "search" floating action button is pressed.
    fun onSearchAndFindRoute(
        context: Context,
        snackbarHostState: SnackbarHostState,
        locationDisplay: LocationDisplay
    ) {
            // Ensure there's no ongoing job before starting a new one
            currentJob?.cancel()
            // Launch a new coroutine for the route finding process
            currentJob = viewModelScope.launch {

                // Clear previous stops and directions
                if (routeStops.isNotEmpty()) {
                    clearStops()
                }

                // Skipping searching for a location if it is current user's location.
                if (startAddress != "Your location") {

                    // Search for starting address
                    searchAddress(
                        context,
                        startAddress,
                        true,
                        snackbarHostState
                    )
                } else {
                        // Add current user's location as a stop
                        val currentLocation = locationDisplay.mapLocation
                        if (currentLocation != null) {
                            val currentLocationStop = Stop(currentLocation)
                            routeStops.add(currentLocationStop)

                        } else {
                            showMessage(context, "Failed to obtain current location")
                        }
                }

                // Search for destination address
                searchAddress(
                    context,
                    destinationAddress,
                    false,
                    snackbarHostState
                )

                // Find route between starting address and destination locations
                findRoute(
                    context,
                    snackbarHostState,
                )

                // Enabling the Quickest/Shortest route options
                isShortestChecked = true
                isQuickestChecked = true
            }
        }

    // This function is used when the "Refresh" Floating action button is pressed.
    fun clearStops() {
        // Remove all stop points
        stopsOverlay.graphics.clear()

        // Remove current route
        routeOverlay.graphics.clear()

        // Clear the list of stops
        routeStops.clear()

        // Clear list of directions
        directionsList.clear()
    }

    // This function is used for when the user uses the app for first time, the app request user's permission to use their location
    fun checkPermissions(context: Context): Boolean {
        // Check permissions to see if both permissions are granted.
        // Coarse location permission.
        val permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        // Fine location permission.
        val permissionCheckFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return permissionCheckCoarseLocation && permissionCheckFineLocation
    }

    // A helper function used when "Quickest" button is clicked.
    fun findQuickestRoute(
        context: Context,
        snackbarHostState: SnackbarHostState
    ){

        // Set the route parameters to finding quickest route.
        routeParameters?.travelMode = routeTask.getRouteTaskInfo().travelModes[0]

        // Clear any previous route
        routeOverlay.graphics.clear()

        // Find the route
        viewModelScope.launch {
            findRoute(
                context,
                snackbarHostState
            )
        }

        // Disable button, will only be enabled if the Refresh button is clicked or restart the app
        isQuickestChecked = false
    }

    // A helper function used when "Shortest" button is clicked.
    fun findShortestRoute(
        context: Context,
        snackbarHostState: SnackbarHostState
    ){

        // Set the route parameters to finding shortest route.
        routeParameters?.travelMode = routeTask.getRouteTaskInfo().travelModes[1]

        // Clear any previous route
        routeOverlay.graphics.clear()

        // Find the route
        viewModelScope.launch {
            findRoute(
                context,
                snackbarHostState
            )
        }
        // Disable button, will only be enabled if the Refresh button is clicked or restart the app
        isShortestChecked = false
    }

    // A helper function used to pinpoint the given query and pin it on the map.
    private suspend fun searchAddress(
        context: Context,
        query: String,
        isStartAddress: Boolean,
        snackbarHostState: SnackbarHostState,
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
                    addStop(isStartAddress, geocodeResult, context)

                    viewModelScope.launch {
                        val centerPoint = geocodeResult.displayLocation
                            ?: return@launch showMessage(
                                context,
                                "The locatorTask.geocode() call failed"
                            )

                        // Animate the map view to the center point.
                        mapViewProxy.setViewpointCenter(centerPoint)
                            .onFailure { error ->
                                println("Failed to set Viewpoint center: ${error.message}")
                            }
                    }
                } else {
                    showMessage(context, "No address found for the given query")
                }
            }.onFailure { error ->
                showMessage(context, "The locatorTask.geocode() call failed: ${error.message}")
            }
    }

    // A helper function to find a route between starting and destination address
     private suspend fun findRoute(
        context: Context,
        snackbarHostState: SnackbarHostState,
    ) {
        snackbarHostState.showSnackbar("Routing...")

        // Set router parameters
        routeParameters?.setStops(routeStops)
        routeParameters?.returnDirections = true

        val routeResult = routeParameters?.let { routeTask.solveRoute(it) }
        if (routeResult != null) {
            routeResult.onFailure {
                showMessage(context, "No route solution. ${it.message}")
                routeOverlay.graphics.clear()
            }.onSuccess {
                // Get the first solved route result
                val route = it.routes[0]

                // Create graphic for route
                val routeGraphic = Graphic(
                    geometry = route.routeGeometry,
                    symbol = SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Solid,
                        color = Color.cyan,
                        width = 2f
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
                travelTime = route.travelTime.roundToInt().toString()
                travelDistance = "%.2f".format(
                    route.totalLength * 0.000621371192 // convert meters to miles and round 2 decimals
                )

                // Animate to show the stops and the route
                viewModelScope.launch {
                    val routeExtent = route.routeGeometry?.extent

                    // Create a Viewpoint from the route's extent.
                    val viewpoint = routeExtent?.let { Viewpoint(it) }

                    // Animate the map view to the new viewpoint
                    if (viewpoint != null) {
                        mapViewProxy.setViewpointAnimated(viewpoint)
                            .onFailure { error ->
                                println("Failed to set Viewpoint: ${error.message}")
                            }
                    }
                }
            }
        }
    }


    // A helper function to add both the stop to routeStops and adds the graphic marker for that stop
    private fun addStop(
        isStartAddress: Boolean,
        geocodeResult: GeocodeResult,
        context: Context
    ) {

        // Create the graphic for the stop
        val markerGraphic = createMarkerGraphic(isStartAddress, geocodeResult, context)

        // Add the geocoded point as a stop
        val stop = geocodeResult.displayLocation?.let { Stop(it) }
        if (stop != null) {
            routeStops.add(stop)
        }

        // Add the stop to the map
        stopsOverlay.graphics.add(markerGraphic)

    }

    // A helper function to create a graphic dependent on the address type.
    private fun createMarkerGraphic(
        isStartAddress: Boolean,
        geocodeResult: GeocodeResult,
        context: Context,
    ): Graphic {

        // Create the graphic depending on the address type
        val drawable = if(isStartAddress && startAddress != "Your location"){
            ContextCompat.getDrawable(context, R.drawable.ic_start) as BitmapDrawable
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_destination) as BitmapDrawable
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

    fun showMessage(
        context: Context,
        message: String
    ) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

}

