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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.kml.KmlTrack
import com.arcgismaps.mapping.kml.KmlTrackElement
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

    val kmlTrackElements by mapViewModel.kmlTrackElements.collectAsStateWithLifecycle()
    val kmlTracks by mapViewModel.kmlTracks.collectAsStateWithLifecycle()

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
                TrackStatusText(
                    isRecordingTrackElements = mapViewModel.isRecordingTrack,
                    elementsAdded = kmlTrackElements.size
                )
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    locationDisplay = locationDisplay,
                    onSingleTapConfirmed = {},
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay)
                )
                TrackOptions(
                    isRecenterEnabled = mapViewModel.isRecenterButtonEnabled,
                    isRecordButtonEnabled = !mapViewModel.isRecordingTrack,
                    kmlTracks = kmlTracks,
                    kmlTrackElements = kmlTrackElements,
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

                // TODO: Display and browse through KML Tracks
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
fun TrackStatusText(
    isRecordingTrackElements: Boolean,
    elementsAdded: Int
) {
    if (isRecordingTrackElements) {
        Text(
            text = "Recording KML track. Elements added = $elementsAdded",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red
        )
    } else {
        Text(
            text = "Click button to capture KML track elements",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun TrackOptions(
    isRecenterEnabled: Boolean,
    isRecordButtonEnabled: Boolean,
    onRecordButtonClicked: () -> Unit,
    onRecenterClicked: () -> Unit,
    onExportClicked: () -> Unit,
    kmlTrackElements: List<KmlTrackElement>,
    kmlTracks: List<KmlTrack>,
) {

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Number of tracks in MultiTrack:",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = kmlTracks.size.toString(),
                style = MaterialTheme.typography.labelLarge
            )
        }
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
            FilledTonalIconButton(
                onClick = onExportClicked,
                enabled = isRecordButtonEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Export KML multi-track"
                )
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
                kmlTrackElements = mutableListOf(),
                kmlTracks = mutableListOf(),
            )
        }
    }
}
