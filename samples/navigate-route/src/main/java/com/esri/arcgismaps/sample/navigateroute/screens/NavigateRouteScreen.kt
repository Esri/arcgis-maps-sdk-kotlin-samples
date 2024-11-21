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

package com.esri.arcgismaps.sample.navigateroute.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDataSourceStatus
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.location.RouteTrackerLocationDataSource
import com.arcgismaps.location.SimulatedLocationDataSource
import com.arcgismaps.location.SimulationParameters
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.navigation.RouteTracker
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Main screen layout for the sample app
 */
@Composable
fun NavigateRouteScreen(sampleName: String) {
    ArcGISEnvironment.applicationContext = LocalContext.current
    // Route settings:
    val initialStop = Stop(Point(-117.160386, 32.706608, SpatialReference.wgs84()))
    val destinationStop = Stop(Point(-117.173034, 32.712327, SpatialReference.wgs84()))
    var routeResult: RouteResult? by remember { mutableStateOf(null) }
    var routeGeometry by remember {
        mutableStateOf(Polyline(listOf(Point(0.0, 0.0)), SpatialReference.wgs84()))
    }
    val routeTask = RouteTask(
        url = "https://sampleserver7.arcgisonline.com/server/rest/services/NetworkAnalysis/SanDiego/NAServer/Route"
    )
    val simulationParameters = SimulationParameters(
        startTime = Instant.now(),
        velocity = 35.0,
        horizontalAccuracy = 5.0,
        verticalAccuracy = 5.0
    )

    // MapView settings:
    val locationDisplay = rememberLocationDisplay()
    val locationDisplayDataSourceStatus by locationDisplay.dataSource.status.collectAsState()
    val currentAutoPanMode by locationDisplay.autoPanMode.collectAsState()
    val mapViewProxy = MapViewProxy()
    val map = ArcGISMap(BasemapStyle.ArcGISStreets)
    val graphicsOverlay = GraphicsOverlay()

    val scope = rememberCoroutineScope()

    // Loads the route geometry using the stop points
    LaunchedEffect(Unit) {
        // load and set the route parameters
        val routeParameters = routeTask.createDefaultParameters().getOrThrow().apply {
            setStops(listOf(initialStop, destinationStop))
            returnDirections = true
            returnStops = true
            returnRoutes = true
        }

        // get the solved route result
        routeResult = routeTask.solveRoute(routeParameters).getOrThrow()

        // get the solved route geometry
        routeGeometry = routeResult?.routes?.get(0)?.routeGeometry
            ?: throw Exception("RouteResult error")

        // set the mapview extent to route geometry
        mapViewProxy.setViewpoint(Viewpoint(routeGeometry.extent))

        // clear any graphics from the current graphics overlay
        graphicsOverlay.graphics.clear()

        val routeGraphic = Graphic(
            geometry = routeGeometry,
            symbol = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Solid,
                color = Color.cyan,
                width = 3f
            )
        )

        // add the graphics to the mapView's graphics overlays
        graphicsOverlay.graphics.add(routeGraphic)
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = map,
                    mapViewProxy = mapViewProxy,
                    graphicsOverlays = listOf(graphicsOverlay),
                    locationDisplay = locationDisplay
                )

                Text("Current AutoPanMode: ${currentAutoPanMode.javaClass.simpleName}")
                NavigateRouteOptions(
                    onNavigateClicked = {
                        if (locationDisplayDataSourceStatus == LocationDataSourceStatus.Started) {
                            // don't navigate, if already in navigation mode
                            return@NavigateRouteOptions
                        }

                        routeResult?.let { routeResult ->
                            // set up a RouteTracker for navigation along the calculated route
                            val routeTracker = RouteTracker(
                                routeResult = routeResult,
                                routeIndex = 0,
                                skipCoincidentStops = true
                            )

                            // create a route tracker location data source to snap the location display to the route
                            val routeTrackerLocationDataSource = RouteTrackerLocationDataSource(
                                routeTracker = routeTracker,
                                locationDataSource = SimulatedLocationDataSource(
                                    polyline = routeGeometry,
                                    parameters = simulationParameters
                                )
                            )

                            locationDisplay.dataSource = routeTrackerLocationDataSource

                            // start location data source and set auto-pan-mode.
                            scope.launch {
                                locationDisplay.dataSource.start().getOrThrow()
                                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                            }
                        }
                    },
                    onRecenterClicked = {
                        // recenter to autopan mode
                        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
                    },
                    onResetClicked = {
                        // stop navigation
                        scope.launch {
                            locationDisplay.dataSource.stop()
                            // use these to reset back to initial state
                            //mapViewProxy.setViewpointRotation(0.0)
                            //mapViewProxy.setViewpoint(Viewpoint(routeGeometry.extent))
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun NavigateRouteOptions(
    onNavigateClicked: () -> Unit,
    onRecenterClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(onClick = onNavigateClicked) { Text("Start") }
        OutlinedButton(onClick = onRecenterClicked) { Text("Navigation AutoPan") }
        OutlinedButton(onClick = onResetClicked) { Text("Stop") }
    }
}
