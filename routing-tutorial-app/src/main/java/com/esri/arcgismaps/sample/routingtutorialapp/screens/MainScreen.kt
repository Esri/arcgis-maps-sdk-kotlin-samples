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

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.routingtutorialapp.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.JobLoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {

    // Create and remember a location display with a recenter auto pan mode.
    val locationDisplay = rememberLocationDisplay()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val mapViewModel = remember { MapViewModel(application, locationDisplay) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()


    // Get user current location after getting permissions
    if (mapViewModel.checkPermissions(context)) {
        // Permissions are already granted.
        LaunchedEffect(Unit) {
            locationDisplay.dataSource.start()
        }
    } else {
        RequestPermissions(
            context = context,
            onPermissionsGranted = {
                coroutineScope.launch {
                    locationDisplay.dataSource.start()
                }
            },
            mapViewModel
        )

    }

    BottomSheetScaffold(

            sheetContent = {

                    // This row holds the time and distance of a route on the bottom sheet
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${mapViewModel.travelTime} min",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "(${mapViewModel.travelDistance} mi)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                        )
                    }

                    // The rest of the sheetContent is a Directions label and list of directions
                    Text(
                        text = "Directions",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                    )
                    RoutesList(directionList = mapViewModel.directionsList)

            },
            // Show only time, distance, and Directions label.
            sheetPeekHeight = if (mapViewModel.showBottomSheet) 75.dp else 0.dp,
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

                    // Start address TextField
                    TextField(
                        value = mapViewModel.startAddress,
                        onValueChange = { mapViewModel.startAddress = it },
                        label = { Text(text = ("start")) },
                        placeholder = { Text("Search for a starting address") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "LocationPin"
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .fillMaxWidth()
                    )

                    // Destination address TextField
                    TextField(
                        value = mapViewModel.destinationAddress,
                        onValueChange = { mapViewModel.destinationAddress = it },
                        label = { Text(text = ("destination")) },
                        placeholder = { Text("Search for a destination address") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = "LocationPin"
                            )
                        },
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
                            .padding(4.dp)
                            .fillMaxWidth()
                    )

                    // This row holds the Quickest and Shortest route option buttons
                    Row(modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally)){
                        Button(onClick = {
                            mapViewModel.findQuickestRoute(context, snackbarHostState)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Finding quickest route...")
                            }
                        }, modifier = Modifier.padding(horizontal = 4.dp), enabled = mapViewModel.isQuickestChecked){
                            Text("Quickest")
                        }
                        Button(onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Finding shortest route...")
                            }
                            mapViewModel.findShortestRoute(context, snackbarHostState)
                        }, modifier = Modifier.padding(horizontal = 4.dp), enabled = mapViewModel.isShortestChecked) {
                            Text("Shortest")
                        }
                    }

                    // This box holds the map and the Refresh and Search Floating Action Buttons (FABs)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)) {

                        if (!mapViewModel.mapLoading) {
                            LoadingDialog(loadingMessage = "Loading Map...")

                        } else {
                                MapView(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            bottom = 20.dp,
                                            end = 8.dp,
                                            start = 8.dp
                                        ),
                                    arcGISMap = mapViewModel.map,
                                    graphicsOverlays = (listOf(mapViewModel.routeOverlay, mapViewModel.stopsOverlay)),
                                    mapViewProxy = mapViewModel.mapViewProxy,
                                    locationDisplay = locationDisplay
                                )

                            // This row holds the Refresh and Search FABS
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FloatingActionButton(

                                    onClick = {
                                        if (mapViewModel.showBottomSheet) {
                                            mapViewModel.showBottomSheet = false
                                        }
                                        mapViewModel.startAddress = "Your location"
                                        locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                                        mapViewModel.destinationAddress = ""
                                        mapViewModel.clearStops()
                                        mapViewModel.isQuickestChecked = false
                                        mapViewModel.isShortestChecked = false
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
                                        if (mapViewModel.showBottomSheet) {
                                            mapViewModel.showBottomSheet = false
                                        }

                                        mapViewModel.onSearchAndFindRoute(
                                            context,
                                            snackbarHostState,
                                            locationDisplay
                                        )
                                    },
                                    content = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Find Route"
                                        )
                                    },
                                ) }
                        }
                    }

                    // This box holds the bottom directions
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)){
                        Text(modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp), textAlign = TextAlign.Center, text = "Search two addresses to find a route between them.")
                    }
                }
            }

    }
}

// Request Permissions for user
@Composable
fun RequestPermissions(context: Context, onPermissionsGranted: () -> Unit, mapViewModel: MapViewModel) {

    // Create an activity result launcher using permissions contract and handle the result.
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if both fine & coarse location permissions are true.
        if (permissions.all { it.value }) {
            onPermissionsGranted()
        } else {
            mapViewModel.showMessage(context, "Location permissions were denied")
        }
    }

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(
            // Request both fine and coarse location permissions.
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

}


// Display the list of directions of the route
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