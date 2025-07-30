/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import kotlinx.coroutines.launch

class RouteViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)
    private val stopGraphicsOverlay = GraphicsOverlay()
    private val routeGraphicsOverlay = GraphicsOverlay()
    val graphicsOverlays = listOf(stopGraphicsOverlay, routeGraphicsOverlay)

    var startPoint: Point? = null
    private var endPoint: Point? = null

    private val _statusText = mutableStateOf("Tap on map or use current location to create start point")
    val statusText: MutableState<String> = _statusText

    var isCurrentLocationAsStartButtonEnabled by mutableStateOf(false)

    // Generate a route with directions and stops for navigation
    private val routeTask =
        RouteTask("https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World")
    private var routeParameters = RouteParameters()

    init {
        viewModelScope.launch {
            routeTask.load().onSuccess {
                setupRouteParameters()
            }
        }
    }

    /**
     * Initialize the location display.
     */
    fun initialize(locationDisplay: LocationDisplay) {
        with(viewModelScope) {
            launch {
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                locationDisplay.dataSource.start().onSuccess {
                    isCurrentLocationAsStartButtonEnabled = true
                }.onFailure {
                    _statusText.value = "Failed to start location data source: ${it.message}"
                }
            }
        }
    }

    /**
     * Set up the route parameters with default values.
     */
    private fun setupRouteParameters() {
        viewModelScope.launch {
            routeParameters = routeTask.createDefaultParameters().getOrNull()!!.apply {
                travelMode = routeTask.getRouteTaskInfo().travelModes.firstOrNull { it.name.contains("Walking") }
                returnDirections = true
                returnStops = true
                returnRoutes = true
            }
        }
    }

    /**
     * Add start and stop points to the route. Once both have been added, calculate the route.
     */
    fun addRoutePoint(point: Point) {
        if (startPoint == null) {
            startPoint = point
            addStopGraphic(point)
            _statusText.value = "Tap to place destination."
            isCurrentLocationAsStartButtonEnabled = false
        } else if (endPoint == null) {
            endPoint = point
            addStopGraphic(point)
            calculateRoute(Stop(startPoint!!), Stop(endPoint!!))
        }
    }


    /**
     * Add a graphic to the map at the given point.
     */
    private fun addStopGraphic(point: Point) {
        val symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10f)
        val graphic = Graphic(point, symbol)
        stopGraphicsOverlay.graphics.add(graphic)
    }

    /**
     * Calculate the route between the start and end points.
     */
    private fun calculateRoute(start: Stop, end: Stop) {
        viewModelScope.launch {
            // Add the start and end points to the route parameters' stops
            routeParameters.setStops(listOf(start, end))
            // Calculate the route
            val routeResult = routeTask.solveRoute(routeParameters).getOrNull()
            routeResult?.let {
                if (SharedRepository.route == null) {
                    // Add the route to the repository so it can be shared with the augmented reality screen
                    SharedRepository.updateRoute(routeResult)
                }
                val routeGraphic =
                    Graphic(
                        routeResult.routes.first().routeGeometry,
                        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.yellow, 5f)
                    )
                routeGraphicsOverlay.graphics.add(routeGraphic)
                _statusText.value = "Route ready. Tap to start navigation."
            }
        }
    }
}
