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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components.MapViewModel
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components.SharedRepository
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@Composable
fun MapScreen(sampleName: String, locationPermissionGranted: Boolean, onNavigateToARScreen: () -> Unit) {

    val mapViewModel: MapViewModel = viewModel()

    // Initialize the location display with auto pan mode set to recenter
    val locationDisplay = rememberLocationDisplay().apply {
        setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
    }
    var isViewmodelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isViewmodelInitialized) {
        if (!isViewmodelInitialized && locationPermissionGranted) {
            mapViewModel.initialize(locationDisplay)
            isViewmodelInitialized = true
        }
    }

    val isGeometryBeingEdited by remember { mutableStateOf(mapViewModel.isGeometryBeingEdited) }
    val showElevationDialog by mapViewModel.showElevationDialog
    val pipeInfoList = SharedRepository.pipeInfoList

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }, content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    locationDisplay = locationDisplay,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = {
                        if (!mapViewModel.geometryEditor.isStarted.value) {
                            mapViewModel.startPolylineEditing()
                        }
                    }
                )
                if (mapViewModel.statusText.value != "") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mapViewModel.statusText.value, color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isGeometryBeingEdited.value) {
                            Button(onClick = { mapViewModel.completePolyline() }) {
                                Text("Complete polyline")
                            }
                        }
                        if (pipeInfoList.isNotEmpty()) {
                            Button(
                                onClick = onNavigateToARScreen,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Show hidden infrastructure in AR")
                            }
                        }
                    }
                }
            }
        }
    )

    if (showElevationDialog) {
        var elevationInput by remember { mutableFloatStateOf(mapViewModel.elevationInput.floatValue) }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Enter an elevation offset") },
            text = {
                Column {
                    Slider(
                        value = elevationInput,
                        onValueChange = { elevationInput = it },
                        valueRange = -10f..10f,
                        steps = 19
                    )
                    Text(modifier = Modifier.align(Alignment.End), text = "${elevationInput.roundToInt()} m")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mapViewModel.onElevationConfirmed(elevationInput)
                }) {
                    Text("Confirm")
                }
            },
        )
    }
}
