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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.navigateroute.components.NavigateRouteViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun NavigateRouteScreen(sampleName: String) {
    val locationDisplay = rememberLocationDisplay()
    val mapViewModel: NavigateRouteViewModel = viewModel<NavigateRouteViewModel>().apply {
        setLocationDisplay(locationDisplay)
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
                        .weight(1f)
                        .animateContentSize(),
                    arcGISMap = mapViewModel.map,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    locationDisplay = locationDisplay
                )

                mapViewModel.apply {
                    NavigateRouteOptions(
                        isNavigateEnabled = isNavigateButtonEnabled,
                        isRecenterEnabled = isRecenterButtonEnabled,
                        onNavigateClicked = ::startNavigation,
                        onRecenterClicked = ::recenterNavigation,
                        onResetClicked = ::resetNavigation
                    )


                    AnimatedVisibility(!isNavigateButtonEnabled) {
                        NavigationRouteInfo(
                            nextStopText,
                            distanceRemainingText,
                            timeRemainingText,
                            nextDirectionText
                        )
                    }

                    // display a MessageDialog if the sample encounters an error
                    messageDialogVM.apply {
                        if (dialogStatus) {
                            MessageDialog(
                                title = messageTitle,
                                description = messageDescription,
                                onDismissRequest = ::dismissDialog
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun NavigationRouteInfo(
    nextStopText: String,
    distanceRemainingText: String,
    timeRemainingText: String,
    nextDirectionText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = nextStopText,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = distanceRemainingText,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = timeRemainingText,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = nextDirectionText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NavigateRouteOptions(
    isNavigateEnabled: Boolean,
    onNavigateClicked: () -> Unit,
    isRecenterEnabled: Boolean,
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
        OutlinedButton(
            enabled = isNavigateEnabled,
            onClick = onNavigateClicked
        ) {
            Text("Navigate")
        }

        OutlinedButton(
            enabled = isRecenterEnabled,
            onClick = onRecenterClicked
        ) {
            Text("Recenter")
        }

        OutlinedButton(
            onClick = onResetClicked
        ) {
            Text("Reset")
        }
    }
}

