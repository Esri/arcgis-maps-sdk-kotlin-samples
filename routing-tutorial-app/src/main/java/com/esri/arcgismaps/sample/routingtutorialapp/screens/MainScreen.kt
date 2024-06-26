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

package com.esri.arcgismaps.sample.routingtutorialapp.screens

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arcgismaps.geometry.Distance
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.TimeValue
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
import com.arcgismaps.tasks.networkanalysis.Route
import com.arcgismaps.tasks.networkanalysis.RouteParameters
import com.arcgismaps.tasks.networkanalysis.RouteResult
import com.arcgismaps.tasks.networkanalysis.RouteTask
import com.arcgismaps.tasks.networkanalysis.Stop
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.routingtutorialapp.R
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
//@Composable
//fun MainScreen(sampleName: String) {
//    val application = LocalContext.current.applicationContext as Application
//    // create a ViewModel to handle MapView interactions
//    val mapViewModel = MapViewModel(application)
//    val arcGISMap by remember {
//        mutableStateOf(ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
//            initialViewpoint = mapViewModel.viewpoint.value
//        })
//    }
//
//    Scaffold(
//        topBar = { SampleTopAppBar(title = sampleName) },
//        content = {
//            MapView(
//                modifier = Modifier.fillMaxSize().padding(it),
//                arcGISMap = arcGISMap,
//                onSingleTapConfirmed = {
//                    mapViewModel.changeBasemap()
//                }
//            )
//        }
//    )
//}

// Define a functional interface for the callback
typealias RouteCallback = (String, String) -> Unit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val directionList = remember { mutableStateListOf("Search two addresses to find a route between them.") }
    val routeStops = remember { mutableListOf<Stop>() }
    val currentJob = remember { mutableStateOf<Job?>(null) }
    val graphicsOverlay = remember { GraphicsOverlay() }
    val graphicsOverlays = remember { listOf(graphicsOverlay) }
    val map = remember { createMap() }
    val mapViewProxy = remember { MapViewProxy() }
    var startAddress by remember { mutableStateOf("highland, california") }
    var destinationAddress by remember { mutableStateOf("redlands") }
    var travelTime by remember { mutableStateOf("") }
    var travelDistance by remember { mutableStateOf("") }
    val currentSpatialReference = remember { mutableStateOf<SpatialReference?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showBottomSheet by remember { mutableStateOf(false)}
    val keyboardController = LocalSoftwareKeyboardController.current
    var mapLoading by remember { mutableStateOf(true) }
    val scaffoldState = rememberBottomSheetScaffoldState()


    val onRouteCalculated: RouteCallback = {
            calculatedTime, calculatedDistance ->
        travelTime = calculatedTime
        travelDistance = calculatedDistance
    }

    // Function to simulate map loading
    LaunchedEffect(true) {
        // Simulate map loading delay
        delay(1000)
        mapLoading = false
        snackbarHostState.currentSnackbarData?.dismiss()
    }

     BottomSheetScaffold(
                sheetContent = {
                    if(directionList.size > 1) {

                        showBottomSheet = true
                        showMessage(context, directionList.size.toString())
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$travelTime min",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "($travelDistance mi)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                            )
                        }

                        Text(
                            text = "Directions",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                        )
                        RoutesList(directionList = directionList)
                    }
                },
                sheetPeekHeight = if (showBottomSheet) 130.dp else 0.dp,
                scaffoldState = scaffoldState
            ) {

             Scaffold(topBar = { SampleTopAppBar(title = sampleName) },
                 snackbarHost = { SnackbarHost(hostState = snackbarHostState) })
             {

                 Column(
                     modifier = Modifier
                         .fillMaxSize()
                         .padding(it)
                 ) {

                     TextField(
                         value = startAddress,
                         onValueChange = { startAddress = it },
                         label = { Text(text = ("start"))},
                         placeholder = {Text("Search for a starting address")},
                         leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = "LocationPin") },
                         singleLine = true,
                         keyboardOptions = KeyboardOptions.Default.copy(
                             imeAction = ImeAction.Done
                         ),
                         keyboardActions = KeyboardActions(
                             onDone = {
                                 keyboardController?.hide()
                             }
                         ),
                         modifier = Modifier
                             .padding(8.dp)
                             .fillMaxWidth()
                     )

                     TextField(
                         value = destinationAddress,
                         onValueChange = { destinationAddress = it },
                         label = { Text(text = ("destination"))},
                         placeholder = {Text("Search for a destination address")},
                         leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = "LocationPin") },
                         singleLine = true,
                         keyboardOptions = KeyboardOptions.Default.copy(
                             imeAction = ImeAction.Done
                         ),
                         keyboardActions = KeyboardActions(
                             onDone = {
                                 keyboardController?.hide()
                             }
                         ),
                         modifier = Modifier
                             .padding(8.dp)
                             .fillMaxWidth()
                     )

                     Box(modifier = Modifier.fillMaxSize()){

                         Box(modifier = Modifier
                             .fillMaxSize()
                             .align(Alignment.Center)
                             ) {
                             if (mapLoading) {
                                 CircularProgressIndicator(
                                     modifier = Modifier.align(Alignment.Center)
                                 )
                             } else {
                                 MapView(
                                     modifier = Modifier.fillMaxSize(),
                                     arcGISMap = map,

                                     graphicsOverlays = graphicsOverlays,
                                     mapViewProxy = mapViewProxy,
                                     onSpatialReferenceChanged = { spatialReference ->
                                         currentSpatialReference.value = spatialReference
                                     }
                                 )
                             }
                         }

                         // Show Snackbar while map is loading
                         if (mapLoading) {
                             LaunchedEffect(scaffoldState.snackbarHostState) {
                                 scaffoldState.snackbarHostState.showSnackbar(
                                     message = "Loading map...",
                                     actionLabel = null // No action needed
                                 )
                             }
                         }

                         if(!mapLoading) {

                             Row(
                                 modifier = Modifier
                                     .align(Alignment.TopEnd)
                                     .padding(8.dp),
                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                             ) {

                                 FloatingActionButton(

                                     onClick = {
                                         if (showBottomSheet) {
                                             showBottomSheet = false
                                         }
                                         startAddress = ""
                                         destinationAddress = ""
                                         clearStops(routeStops, directionList, graphicsOverlay)
                                     },
                                 ) {
                                     Icon(
                                         Icons.Outlined.Refresh,
                                         contentDescription = "Clear Results"
                                     )
                                 }

                                 FloatingActionButton(
                                     onClick = {
                                         focusManager.clearFocus()
                                         if (showBottomSheet) {
                                             showBottomSheet = false
                                         }
                                         // Ensure there's no ongoing job before starting a new one
                                         currentJob.value?.cancel()
                                         // Launch a new coroutine for the route finding process
                                         currentJob.value = coroutineScope.launch {

                                             // Clear previous stops and directions
                                             if (routeStops.isNotEmpty()) {
                                                 clearStops(routeStops, directionList, graphicsOverlay)
                                             }
                                             searchAddress(
                                                 context,
                                                 coroutineScope,
                                                 startAddress,
                                                 currentSpatialReference.value,
                                                 graphicsOverlay,
                                                 mapViewProxy,
                                                 true,
                                                 routeStops,
                                                 snackbarHostState
                                             )
                                             searchAddress(
                                                 context,
                                                 coroutineScope,
                                                 destinationAddress,
                                                 currentSpatialReference.value,
                                                 graphicsOverlay,
                                                 mapViewProxy,
                                                 false,
                                                 routeStops,
                                                 snackbarHostState
                                             )

                                             println("start address: $startAddress")
                                             println("des addres: $destinationAddress")
                                             findRoute(context, routeStops, graphicsOverlay, directionList, snackbarHostState, onRouteCalculated )
                                         }
                                     },
                                     content = {
                                         Icon(
                                             Icons.Outlined.Search,
                                             contentDescription = "Find Route"
                                         )
                                     },
                                 )
                             }
                         }


                     }

                 }

             }
        }
        }


@Composable
fun RoutesList(directionList: MutableList<String>) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        items(directionList.size) { index ->
            if(index != directionList.size - 1) {
                Text(text = directionList[index] + ".", modifier = Modifier.padding(8.dp))

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
            } else {
                Text(text = directionList[index] + ".", modifier = Modifier.padding(8.dp))
            }

        }
    }
}



suspend fun searchAddress(
    context: Context,
    coroutineScope: CoroutineScope,
    query: String,
    currentSpatialReference: SpatialReference?,
    graphicsOverlay: GraphicsOverlay,
    mapViewProxy: MapViewProxy,
    isStartAddress: Boolean, // Added to distinguish between start and destination addresses
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

                coroutineScope.launch {
                    val centerPoint = geocodeResult.displayLocation
                        ?: return@launch showMessage(context, "The locatorTask.geocode() call failed")

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


fun createMap(): ArcGISMap {

    return ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(
            latitude = 34.0539,
            longitude = -118.2453,
            scale = 144447.638572
        )
    }

}

fun createMarkerGraphic(geocodeResult: GeocodeResult, context: Context, isStartAddress: Boolean): Graphic {

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

suspend fun findRoute(
    context: Context,
    routeStops: MutableList<Stop>,
    graphicsOverlay: GraphicsOverlay,
    directionsList: MutableList<String>,
    snackbarHostState: SnackbarHostState,
    callback: RouteCallback
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
            val travelTime = route.travelTime.roundToInt()
            val travelDistance = "%.2f".format(
                route.totalLength * 0.000621371192 // convert meters to miles and round 2 decimals
            )
            callback(travelTime.toString(), travelDistance)
        }

    } catch (e: Exception) {
        showMessage(context, "Failed to find route: ${e.message}")
    }

}

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}