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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.view.LocationDisplay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.routingtutorialapp.components.MapViewModel
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
    val locationDisplay = rememberLocationDisplay().apply {
        setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
    }

    val mapViewModel: MapViewModel = viewModel()

    //val mapViewModel : MapViewModel = viewModel(locationDisplay)
    val snackbarHostState = remember { mapViewModel.snackbarHostState }
    val bottomSheetState = rememberModalBottomSheetState()

    // Everytime user enters app, check if they already granted permission otherwise request it.
    if (mapViewModel.checkPermissions()) {
        // Permissions are already granted.
        // Do nothing until the location button is clicked.
    } else {
        val message = "Location permissions were denied"
        RequestPermissions(onPermissionsGranted = {
            // Do nothing because we want the user to only grant permission and get location
            // only on Get Location Button Click click.
        }, message, { mapViewModel.showMessage(message) })
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {

            // This composable holds the top portion of the app, where addresses are entered
            // alongside having options for getting shortest or quickest route
            RouteOptions(
                startAddress = mapViewModel.startAddress,
                onStartedAddressEntered = { newStartAddress ->
                    mapViewModel.startAddress = newStartAddress
                },
                onDestinationAddressEntered = { newDestinationAddress ->
                    mapViewModel.destinationAddress = newDestinationAddress
                },
                destinationAddress = mapViewModel.destinationAddress,
                onQuickestRoute = mapViewModel::findQuickestRoute,
                onShortestRoute = mapViewModel::findShortestRoute,
                onSearchStartingAddress = mapViewModel::onSearchStartingAddress,
                onSearchDestinationAddress = mapViewModel::onSearchDestinationAddress,
                isStartAddressTextFieldEnabled = mapViewModel.isStartAddressTextFieldEnabled,
                isDestinationAddressTextFieldEnabled = mapViewModel.isDestinationAddressTextFieldEnabled,
            )



            // This box holds the map and all Floating Action Buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {

                if (!mapViewModel.mapLoading) {
                    LoadingDialog(loadingMessage = "Loading Map...")

                } else {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISMap = mapViewModel.map,
                        graphicsOverlays = (listOf(
                            mapViewModel.routeOverlay, mapViewModel.stopsOverlay
                        )),
                        mapViewProxy = mapViewModel.mapViewProxy,
                        locationDisplay = locationDisplay
                    )

                    // This row holds all the Floating Action Buttons
                    Row(
                        modifier = Modifier.Companion
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        ShowFloatingActionButtons(snackbarHostState = snackbarHostState,
                            onRefreshButtonClicked = {
                                mapViewModel.onRefreshButtonClicked(locationDisplay)
                            },
                            onSearchRouteButtonClicked = {
                                mapViewModel.onSearchRouteButtonClicked(locationDisplay)
                            },
                            onGetCurrentLocationButtonClicked = {
                                mapViewModel.onGetCurrentLocationButtonClicked(locationDisplay)
                            })
                    }
                }
            }

            // Do not show the bottom instructions if user sees the bottomsheet
            if (!mapViewModel.showBottomSheet) {
                // This text holds the bottom directions
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                    textAlign = TextAlign.Center,
                    text = "Search two addresses to find a route between them."
                )
            }

            if (mapViewModel.showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { mapViewModel.showBottomSheet = false },
                    sheetState = bottomSheetState,
                    modifier = Modifier.fillMaxHeight(0.7f)
                    //sheetMaxWidth = if (mapViewModel.showBottomSheet) ((LocalConfiguration.current.screenHeightDp) / 8).dp else 0.dp,
                    // Show only time, distance, and Directions label during peek height
                ) {
                    // This row holds the time and distance of a route on the bottom sheet
                    BottomSheetContent(
                        travelTime = mapViewModel.travelTime,
                        travelDistance = mapViewModel.travelDistance,
                        directionsList = mapViewModel.directionsList
                    )
                }
            }
        }
    }
}


@Composable
private fun ShowFloatingActionButtons(
    onRefreshButtonClicked: (LocationDisplay) -> Unit,
    onSearchRouteButtonClicked: (LocationDisplay) -> Unit,
    onGetCurrentLocationButtonClicked: (LocationDisplay) -> Unit,
    snackbarHostState: SnackbarHostState,

    ) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Get Location FAB
    FloatingActionButton(
        onClick = {
            focusManager.clearFocus()
            onGetCurrentLocationButtonClicked(LocationDisplay())
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Getting your location...")
            }
        },
    ) {
        Icon(
            Icons.Outlined.LocationOn, contentDescription = "Get Current Location"
        )
    }

    // Refresh and Start Over FAB
    FloatingActionButton(
        onClick = {
            focusManager.clearFocus()
            onRefreshButtonClicked(LocationDisplay())
        },
    ) {
        Icon(
            Icons.Outlined.Refresh, contentDescription = "Clear Results"
        )
    }

    // Search for a Route FAB
    FloatingActionButton(
        onClick = {
            focusManager.clearFocus()
            onSearchRouteButtonClicked(LocationDisplay())
        },
        content = {
            Icon(
                Icons.Outlined.Search, contentDescription = "Find Route"
            )
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteOptions(
    startAddress: String,
    onStartedAddressEntered: (String) -> Unit,
    destinationAddress: String,
    onDestinationAddressEntered: (String) -> Unit,
    onQuickestRoute: () -> Unit,
    onShortestRoute: () -> Unit,
    onSearchStartingAddress: () -> Unit,
    onSearchDestinationAddress: () -> Unit,
    isStartAddressTextFieldEnabled: Boolean,
    isDestinationAddressTextFieldEnabled: Boolean,
) {
    // used to clear keyboard after entering addresses
    val keyboardController = LocalSoftwareKeyboardController.current
    val options = listOf("", "Shortest", "Quickest")
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(options[0]) }


    Column {

        // Start address TextField
        TextField(
            value = startAddress,
            onValueChange = { onStartedAddressEntered(it) },
            label = { Text(text = ("start")) },
            placeholder = { Text("Search for a starting address") },
            leadingIcon = {
                Icon(
                    Icons.Filled.LocationOn, contentDescription = "LocationPin"
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchStartingAddress()
            }),
            modifier = Modifier
                .fillMaxWidth(),
            enabled = isStartAddressTextFieldEnabled,
        )

        // Destination address TextField
        TextField(value = destinationAddress,
            onValueChange = { onDestinationAddressEntered(it) },
            label = { Text(text = ("destination")) },
            placeholder = { Text("Search for a destination address") },
            leadingIcon = {
                Icon(
                    Icons.Filled.LocationOn, contentDescription = "LocationPin"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchDestinationAddress()

            }),
            modifier = Modifier
                .fillMaxWidth(),
            enabled = isDestinationAddressTextFieldEnabled
        )

        // This row holds the Quickest and Shortest route option dropdown
        Row {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                TextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Choose quickest or shortest route.") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                text = option
                                expanded = false
                                when(text) {
                                    "Shortest" -> onShortestRoute()
                                    "Quickest" -> onQuickestRoute()
                                    else -> null
                                }
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetContent(
    travelTime: String, travelDistance: String, directionsList: List<String>
) {
    Column {

        Row(
            modifier = Modifier.Companion.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = travelTime,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "(${travelDistance} mi)",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            )
        }

        // The rest of the sheetContent is a Directions label and list of directions
        Text(
            text = "Directions",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
        )
        RoutesList(directionList = directionsList)
    }
}


// Display the list of directions of the route
@Composable
fun RoutesList(directionList: List<String>) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        items(directionList.size) { index ->
            if (index != directionList.size - 1) {
                Text(text = directionList[index] + ".", modifier = Modifier.padding(8.dp))

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(), thickness = 1.dp
                )
            } else {
                Text(text = directionList[index] + ".", modifier = Modifier.padding(8.dp))
            }

        }
    }
}


// Request Permissions for user
@Composable
fun RequestPermissions(
    onPermissionsGranted: () -> Unit, message: String, showMessage: (String) -> Unit
) {

    // Create an activity result launcher using permissions contract and handle the result.
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if both fine & coarse location permissions are true.
        if (permissions.all { it.value }) {
            onPermissionsGranted()
        } else {
            showMessage(message)
        }
    }

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(
            // Request both fine and coarse location permissions.
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

}
