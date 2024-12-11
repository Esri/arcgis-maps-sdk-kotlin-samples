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

package com.esri.arcgismaps.sample.findroute.components

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.findroute.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class FindRouteViewModel(private val application: Application) : AndroidViewModel(application) {

    // use a map with navigation basemap style
    val map = ArcGISMap(BasemapStyle.ArcGISNavigation).apply {
        initialViewpoint = Viewpoint(32.7222, -117.1530, 100000.0)
    }

    // create a graphic overlay
    val graphicsOverlay = GraphicsOverlay()

    // create a proxy for viewpoint animations
    val mapViewProxy = MapViewProxy()

    // keep track of the list of directions maneuvers obtained from the RouteResult
    var directions by mutableStateOf(listOf<DirectionManeuver>())
        private set

    var routeDirectionsInfo by mutableStateOf("")
        private set

    // start point
    private val startPoint = Point(-117.1508, 32.7411, SpatialReference.wgs84())

    // destination point
    private val destinationPoint = Point(-117.1555, 32.7033, SpatialReference.wgs84())

    // create a messageDialogViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        setupSymbols()
    }

    /**
     * Solves the route using a Route Task, populates the navigation drawer with the directions,
     * and displays a graphic of the route on the map.
     */
    suspend fun solveRoute() {
        // set the applicationContext as it is required with RouteTask
        ArcGISEnvironment.applicationContext = application.applicationContext
        // create a route task instance
        val routeTask = RouteTask(
            "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
        )

        routeTask.createDefaultParameters().onSuccess { routeParams ->
            // create stops
            val stops = listOf(
                Stop(startPoint),
                Stop(destinationPoint)
            )

            routeParams.apply {
                setStops(stops)
                // set return directions as true to return turn-by-turn directions in the route's directionManeuvers
                returnDirections = true
            }
            // solve the route
            val routeResult = routeTask.solveRoute(routeParams).getOrElse {
                messageDialogVM.showMessageDialog("Error with SolveRoute", it.message.toString())
            } as RouteResult
            val route = routeResult.routes[0]
            // create a simple line symbol for the route
            val routeSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.cyan, 5f)

            // create a graphic for the route and add it to the graphics overlay
            graphicsOverlay.graphics.add(Graphic(route.routeGeometry, routeSymbol))
            // get the list of direction maneuvers and display it
            // NOTE: to get turn-by-turn directions the route parameters
            //  must have the returnDirections parameter set to true.
            directions = route.directionManeuvers
            // set the time and distance info for the route
            routeDirectionsInfo = "${Math.round(route.travelTime)} min " +
                    "(${Math.round(route.totalLength / 1000.0)} km)"

            route.routeGeometry?.extent?.center?.let { point ->
                mapViewProxy.setViewpointAnimated(Viewpoint(point, 70000.0))
            }
        }.onFailure {
            messageDialogVM.showMessageDialog("Error creating RouteTask ", it.message.toString())
        }
    }

    /**
     * Set up the source, destination and route symbols.
     */
    private fun setupSymbols() {
        val startDrawable = ContextCompat.getDrawable(
            application.applicationContext,
            R.drawable.ic_source
        ) as BitmapDrawable

        val endDrawable = ContextCompat.getDrawable(
            application.applicationContext,
            R.drawable.ic_destination
        ) as BitmapDrawable

        val pinSourceSymbol = PictureMarkerSymbol.createWithImage(startDrawable).apply {
            // make the graphic smaller
            width = 30f
            height = 30f
            offsetY = 20f
        }

        val pinDestinationSymbol = PictureMarkerSymbol.createWithImage(endDrawable).apply {
            // make the graphic smaller
            width = 30f
            height = 30f
            offsetY = 20f
        }

        // create graphics and it to the graphics overlay
        graphicsOverlay.graphics.addAll(
            listOf(
                Graphic(geometry = startPoint, symbol = pinSourceSymbol),
                Graphic(geometry = destinationPoint, symbol = pinDestinationSymbol)
            )
        )
    }

    fun selectDirectionManeuver(directionManeuver: DirectionManeuver) {
        directionManeuver.geometry?.let { geometry ->
            viewModelScope.launch {
                // set the viewpoint to the selected maneuver
                mapViewProxy.setViewpointAnimated(Viewpoint(geometry.extent))
            }
            // create a graphic with a symbol for the maneuver and add it to the graphics overlay
            val selectedRouteSymbol = SimpleLineSymbol(
                SimpleLineSymbolStyle.Solid,
                Color.green, 5f
            )
            graphicsOverlay.graphics.add(Graphic(geometry, selectedRouteSymbol))
            // TODO Collapse bottomsheeet
        }
    }

}
