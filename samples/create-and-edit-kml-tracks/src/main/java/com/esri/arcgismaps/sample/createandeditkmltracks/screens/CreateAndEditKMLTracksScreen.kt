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

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.createandeditkmltracks.components.CreateAndEditKMLTracksViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme


/**
 * Main screen layout for the sample app
 */
@Composable
fun CreateAndEditKMLTracksScreen(sampleName: String) {
    val locationDisplay = rememberLocationDisplay()
    val mapViewModel = viewModel<CreateAndEditKMLTracksViewModel>().apply {
        setLocationDisplay(locationDisplay)
    }

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
                        } else {
                            mapViewModel.isTrackLocation = true
                        }
                    },
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay)
                )
                TrackOptions(
                    isRecenterEnabled = mapViewModel.isRecenterButtonEnabled,
                    isRecordButtonEnabled = !mapViewModel.isRecordingTrack,
                    sizeOfTracks = mapViewModel.kmlTracks.size,
                    sizeOfElements = mapViewModel.kmlTrackElements.size,
                    onRecenterClicked = { mapViewModel.recenter() },
                    onExportClicked = { mapViewModel.exportKmlMultiTrack() },
                    onRecordButtonClicked = {
                        if (!mapViewModel.isRecordingTrack) {
                            mapViewModel.startRecordingKmlTrack()
                        } else {
                            mapViewModel.stopRecordingKmlTrack()
                        }
                    },
                )

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

@Composable
fun TrackOptions(
    isRecenterEnabled: Boolean,
    isRecordButtonEnabled: Boolean,
    onRecordButtonClicked: () -> Unit,
    onRecenterClicked: () -> Unit,
    onExportClicked: () -> Unit,
    sizeOfElements: Int,
    sizeOfTracks: Int,
) {

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Is tracking: ${!isRecordButtonEnabled}\n" +
                    "KmlElements size: $sizeOfElements\n" +
                    "KmlTracks added: $sizeOfTracks"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalIconButton(onClick = onRecenterClicked, enabled = isRecenterEnabled) {
                Icon(Icons.Default.LocationOn, "RecenterIcon")
            }

            Button(
                modifier = Modifier.animateContentSize(),
                onClick = onRecordButtonClicked
            ) {
                Text(if (isRecordButtonEnabled) "Record Track" else "Stop Recording")
            }
            OutlinedButton(
                onClick = onExportClicked,
                enabled = isRecordButtonEnabled
            ) {
                Text("Export KmzMultiTrack")
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun TrackOptionsPreview() {
    SampleAppTheme {
        Surface {
            TrackOptions(
                isRecenterEnabled = true,
                isRecordButtonEnabled = true,
                onRecordButtonClicked = { },
                onRecenterClicked = { },
                onExportClicked = { },
                sizeOfElements = 0,
                sizeOfTracks = 0,
            )
        }
    }
}
