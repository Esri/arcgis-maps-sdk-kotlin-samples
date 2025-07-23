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
import kotlin.math.roundToLong

class FindRouteViewModel(private val application: Application) : AndroidViewModel(application) {

    // use a map with navigation basemap style
    val map = ArcGISMap(BasemapStyle.ArcGISNavigation).apply {
        initialViewpoint = Viewpoint(latitude = 32.7222, longitude = -117.1530, scale = 100000.0)
    }

    // create a graphic overlay
    val graphicsOverlay = GraphicsOverlay()

    // create a proxy for viewpoint animations
    val mapViewProxy = MapViewProxy()

    // start point
    private val startPoint = Point(
        x = -117.1508,
        y = 32.7411,
        spatialReference = SpatialReference.wgs84()
    )

    // destination point
    private val destinationPoint = Point(
        x = -117.1555,
        y = 32.7033,
        spatialReference = SpatialReference.wgs84()
    )

    // create a symbol for the selected maneuver
    private val selectedDirectionSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.red, width = 5f
    )

    // create a simple line symbol for the solved route
    private val routeSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.fromRgba(0, 0, 255, 255),
        width = 5f
    )

    // keep track of the list of directions maneuvers obtained from the RouteResult
    var directions by mutableStateOf(listOf<DirectionManeuver>())
        private set

    // text to display route distance and time
    var routeDirectionsInfo by mutableStateOf("")
        private set

    // create a messageDialogViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        setupSymbols()
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

    /**
     * Solves the route using a [RouteTask], populates the bottom sheet with the directions,
     * and displays a graphic of the [RouteResult] on the map.
     */
    fun solveRoute(onSolveRouteCompleted: () -> Unit) {
        // set the applicationContext as it is required with RouteTask
        ArcGISEnvironment.applicationContext = application.applicationContext

        // create a route task instance
        val routeTask = RouteTask(
            url = "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World"
        )

        viewModelScope.launch {
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
                val routeResult = routeTask.solveRoute(routeParameters = routeParams).getOrElse {
                    messageDialogVM.showMessageDialog(
                        title = "Error with SolveRoute",
                        description = it.message.toString()
                    )
                } as RouteResult

                // obtain the first route result
                val route = routeResult.routes.first()

                // create a graphic for the route and add it to the graphics overlay
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = route.routeGeometry,
                        symbol = routeSymbol
                    )
                )

                // get the list of direction maneuvers and display it
                // NOTE: to get turn-by-turn directions the route parameters
                // must have the returnDirections parameter set to true.
                directions = route.directionManeuvers

                // set the time and distance info for the route
                routeDirectionsInfo = "${route.travelTime.roundToLong()} min " +
                        "(${(route.totalLength / 1000.0).roundToLong()} km)"

                // animate the map to the route's geometry
                route.routeGeometry?.let { geometry ->
                    mapViewProxy.setViewpointGeometry(
                        boundingGeometry = geometry,
                        paddingInDips = 100.0
                    )
                }
                // notify UI on route task completion
                onSolveRouteCompleted()
            }.onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Error creating route parameters",
                    description = it.message.toString()
                )
                // notify UI on route task completion
                onSolveRouteCompleted()
            }
        }
    }

    /**
     * Selects the [directionManeuver] graphic and
     * set's the viewpoint to the bounds of the maneuver geometry
     */
    fun selectDirectionManeuver(directionManeuver: DirectionManeuver) {
        directionManeuver.geometry?.let { geometry ->
            // update the graphic of the selected direction maneuver
            graphicsOverlay.graphics.add(
                Graphic(geometry = geometry, symbol = selectedDirectionSymbol)
            )

            viewModelScope.launch {
                // set the viewpoint to the selected maneuver
                mapViewProxy.setViewpointGeometry(
                    boundingGeometry = geometry,
                    paddingInDips = 100.0
                )
            }
        }
    }
}
