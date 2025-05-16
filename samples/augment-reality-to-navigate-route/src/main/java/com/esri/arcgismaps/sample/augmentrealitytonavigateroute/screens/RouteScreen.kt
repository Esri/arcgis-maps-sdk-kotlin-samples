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

package com.esri.arcgismaps.sample.augmentrealitytonavigateroute.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components.RouteViewModel
import com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components.SharedRepository
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun RouteScreen(
    sampleName: String, locationPermissionGranted: Boolean, onNavigateToARScreen: () -> Unit
) {
    val locationDisplay = rememberLocationDisplay()
    val routeViewModel: RouteViewModel = viewModel()
    var isViewmodelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isViewmodelInitialized) {
        if (!isViewmodelInitialized && locationPermissionGranted) {
            routeViewModel.initialize(locationDisplay)
            isViewmodelInitialized = true
        }
    }
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = routeViewModel.arcGISMap,
                locationDisplay = locationDisplay,
                graphicsOverlays = routeViewModel.graphicsOverlays,
                onSingleTapConfirmed = { tap -> tap.mapPoint?.let { it -> routeViewModel.addRoutePoint(it) } })
            if (routeViewModel.statusText.value != "") {
                // Add directions text box at the top of the screen
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = routeViewModel.statusText.value, color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }, floatingActionButton = {
        if (routeViewModel.startPoint == null && SharedRepository.route == null) {
            if (routeViewModel.isCurrentLocationAsStartButtonEnabled) {
                Button(
                    onClick = { locationDisplay.mapLocation?.let { routeViewModel.addRoutePoint(it) } },
                    modifier = Modifier.padding(bottom = 32.dp),
                ) {
                    Text("Use current location as start point")
                }
            }
        }
        if (SharedRepository.route != null) {
            Button(
                onClick = onNavigateToARScreen, modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("Navigate in augmented reality")
            }
        }
    }, floatingActionButtonPosition = FabPosition.Center
    )
}
