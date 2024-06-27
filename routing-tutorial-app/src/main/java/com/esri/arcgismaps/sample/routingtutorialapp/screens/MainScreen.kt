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

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
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
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.routingtutorialapp.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {

    val application = LocalContext.current.applicationContext as Application
    val mapViewModel = remember { MapViewModel(application) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
            sheetContent = {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${mapViewModel.travelTime} min",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "(${mapViewModel.travelDistance} mi)",
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
                    RoutesList(directionList = mapViewModel.directionList)
            },
            sheetPeekHeight = if (mapViewModel.showBottomSheet) 130.dp else 0.dp,
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
                            .padding(8.dp)
                            .fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxSize()) {

                            if (!mapViewModel.mapLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )

                            } else {

                                MapView(
                                    modifier = Modifier.fillMaxSize(),
                                    arcGISMap = mapViewModel.map,
                                    graphicsOverlays = mapViewModel.graphicsOverlays,
                                    mapViewProxy = mapViewModel.mapViewProxy,
                                )

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
                                            mapViewModel.startAddress = ""
                                            mapViewModel.destinationAddress = ""
                                            mapViewModel.clearStops(
                                                mapViewModel.routeStops,
                                                mapViewModel.directionList,
                                                mapViewModel.graphicsOverlay
                                            )
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
                                            mapViewModel.onFindRoute(
                                                context,
                                                snackbarHostState,
                                            )
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