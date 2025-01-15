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

package com.esri.arcgismaps.sample.createandeditkmltracks.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.createandeditkmltracks.components.CreateAndEditKMLTracksViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar


/**
 * Main screen layout for the sample app
 */
@Composable
fun CreateAndEditKMLTracksScreen(sampleName: String) {
    val locationDisplay = rememberLocationDisplay()
    val mapViewModel = viewModel<CreateAndEditKMLTracksViewModel>().apply {
        setLocationDisplay(locationDisplay)
    }

    var hint by remember { mutableStateOf("") }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    locationDisplay = locationDisplay,
                    onSingleTapConfirmed = {
                        if (locationDisplay.autoPanMode.value == LocationDisplayAutoPanMode.Off) {
                            locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
                        }
                        if (mapViewModel.isTrackLocation) {
                            mapViewModel.isTrackLocation = false
                            hint = "Tracking has stopped"
                        } else {
                            mapViewModel.isTrackLocation = true
                            hint = "Tracking has started"
                        }
                    },
                    graphicsOverlays = listOf(
                        mapViewModel.locationHistoryOverlay,
                        mapViewModel.locationHistoryLineOverlay
                    )
                )
                Text(text = hint)

                // TODO: Add UI components in this Column ...
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}
