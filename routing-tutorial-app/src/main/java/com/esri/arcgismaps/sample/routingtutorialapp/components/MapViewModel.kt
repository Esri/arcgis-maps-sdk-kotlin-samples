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

import android.app.Application
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.routingtutorialapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MapViewModel(application: Application) : AndroidViewModel(application) {

    val map = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude = 34.0539,
            longitude = -118.2453,
            scale = 144447.638572
        )
    }
    val directionList by lazy {mutableListOf("Search two addresses to find a route between them.")}
    val routeStops by lazy {mutableListOf<Stop>()}
    var currentJob by mutableStateOf<Job?>(null)
    val graphicsOverlay = GraphicsOverlay()
    val graphicsOverlays = listOf(graphicsOverlay)
    val mapViewProxy =  MapViewProxy()
    var startAddress by mutableStateOf("highland, california")
    var destinationAddress by mutableStateOf("redlands")
    var travelTime by mutableStateOf("")
    var travelDistance by mutableStateOf("")
    val currentSpatialReference by mutableStateOf<SpatialReference?>(null)
    var mapLoading by mutableStateOf(false)
    var showBottomSheet by  mutableStateOf(false)


    init {
        viewModelScope.launch {
            map.load().onSuccess {
                mapLoading = true
            }.onFailure {
                showMessage(application, "map failed to load")
            }
        }
    }


    fun clearStops(
        routeStops: MutableList<Stop>,
        directionList: MutableList<String>,
        graphicsOverlay: GraphicsOverlay
    ) {
        graphicsOverlay.graphics.clear()
        routeStops.clear()
        directionList.clear()
        directionList.add("Search two addresses to find a route between them.")
    }

    fun onFindRoute(
        context: Context,
        snackbarHostState: SnackbarHostState
    ) {
        // Ensure there's no ongoing job before starting a new one
        currentJob?.cancel()
        // Launch a new coroutine for the route finding process
        currentJob = viewModelScope.launch {

            // Clear previous stops and directions
            if (routeStops.isNotEmpty()) {
                clearStops(
                    routeStops,
                    directionList,
                    graphicsOverlay
                )
            }
            searchAddress(
                context,
                viewModelScope,
                startAddress,
                currentSpatialReference,
                graphicsOverlay,
                mapViewProxy,
                true,
                routeStops,
                snackbarHostState
            )
            searchAddress(
                context,
                viewModelScope,
                destinationAddress,
                currentSpatialReference,
                graphicsOverlay,
                mapViewProxy,
                false,
                routeStops,
                snackbarHostState
            )

            findRoute(
                context,
                routeStops,
                graphicsOverlay,
                directionList,
                snackbarHostState
            )
        }
    }

    private suspend fun searchAddress(
        context: Context,
        viewModelScope: CoroutineScope,
        query: String,
        currentSpatialReference: SpatialReference?,
        graphicsOverlay: GraphicsOverlay,
        mapViewProxy: MapViewProxy,
        isStartAddress: Boolean,
        routeStops: MutableList<Stop>,
        snackbarHostState: SnackbarHostState
    ) {
        snackbarHostState.showSnackbar("locating $query...")
        val geocodeServerUri = "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
        val locatorTask = LocatorTask(geocodeServerUri)

        // Create geocode parameters
        val geocodeParameters = GeocodeParameters().apply {
            resultAttributeNames.add("*")
            maxResults = 1
            outputSpatialReference = currentSpatialReference
        }

        // Search for the address
        locatorTask.geocode(searchText = query, parameters = geocodeParameters)
            .onSuccess { geocodeResults: List<GeocodeResult> ->
                if (geocodeResults.isNotEmpty()) {
                    val geocodeResult = geocodeResults[0]
                    addStop(isStartAddress, geocodeResult, routeStops, graphicsOverlay, context)

                    viewModelScope.launch {
                        val centerPoint = geocodeResult.displayLocation
                            ?: return@launch showMessage(context, "The locatorTask.geocode() call failed")

//                        // todo: work with this later
//                        println(centerPoint.spatialReference)
//                        println(SpatialReference.wgs84().wkid) // helpful for adding a point on the map


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

    private fun createMarkerGraphic(geocodeResult: GeocodeResult, context: Context, isStartAddress: Boolean): Graphic {

        val drawable = if(isStartAddress){
            ContextCompat.getDrawable(context, R.drawable.ic_start) as BitmapDrawable
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_destination) as BitmapDrawable
        }

        val pinSourceSymbol = PictureMarkerSymbol.createWithImage(drawable).apply {
            // make the graphic smaller
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

    private fun addStop(
        isStartAddress: Boolean,
        geocodeResult: GeocodeResult,
        routeStops: MutableList<Stop>,
        graphicsOverlay: GraphicsOverlay,
        context: Context
    ) {

        val markerGraphic = if (isStartAddress) {
            println("it is a streetaddress")
            createMarkerGraphic(geocodeResult, context, true) // For start address
        } else {
            createMarkerGraphic(geocodeResult, context, false) // For destination address
        }

        // Add the geocoded point as a stop
        val stop = geocodeResult.displayLocation?.let { Stop(it) }
        if (stop != null) {
            routeStops.add(stop)
        }

        // Clear previous results and add graphics only if it's a destination address for simplicity
        if (!isStartAddress) {
            graphicsOverlay.graphics.add(markerGraphic)

        } else {
            // For start address, just add the marker without clearing previous graphics
            graphicsOverlay.graphics.add(markerGraphic)
        }
    }

    private suspend fun findRoute(
        context: Context,
        routeStops: MutableList<Stop>,
        graphicsOverlay: GraphicsOverlay,
        directionsList: MutableList<String>,
        snackbarHostState: SnackbarHostState,
    ) {

        val routeTask = RouteTask(
            url = "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
        )

        // Create a job to find the route.
        try {
            snackbarHostState.showSnackbar("Routing...")
            val routeParameters: RouteParameters = routeTask.createDefaultParameters().getOrThrow()
            routeParameters.setStops(routeStops)
            routeParameters.returnDirections = true

            // Solve a route using the route parameters created.
            val routeResult: RouteResult = routeTask.solveRoute(routeParameters).getOrThrow()
            val routes = routeResult.routes

            // If a route is found.
            if (routes.isNotEmpty()) {
                val route = routes[0]
                val routeGraphic = Graphic(
                    geometry = route.routeGeometry,
                    symbol = SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Solid,
                        color = com.arcgismaps.Color.cyan,
                        width = 2f
                    )
                )


                // Add the route graphic to the graphics overlay.
                graphicsOverlay.graphics.add(routeGraphic)
                // Get the direction text for each maneuver and display it as a list on the UI.
                directionsList.clear()
                route.directionManeuvers.forEach { directionManeuver: DirectionManeuver ->
                    directionsList.add(directionManeuver.directionText)
                }

                // set distance-time text
                travelTime = route.travelTime.roundToInt().toString()
                travelDistance = "%.2f".format(
                    route.totalLength * 0.000621371192 // convert meters to miles and round 2 decimals
                )
                if (directionList.size > 1) {
                    showBottomSheet = true
                }
            }

        } catch (e: Exception) {
            showMessage(context, "Failed to find route: ${e.message}")
        }

    }

    private fun showMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

}

