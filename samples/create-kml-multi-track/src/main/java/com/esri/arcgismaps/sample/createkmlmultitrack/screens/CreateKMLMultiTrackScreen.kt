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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.createkmlmultitrack.screens

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.mapping.kml.KmlTrackElement
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import com.esri.arcgismaps.sample.createkmlmultitrack.R
import com.esri.arcgismaps.sample.createkmlmultitrack.components.CreateKMLMultiTrackViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Main screen layout for the sample app
 */
@Composable
fun CreateKMLMultiTrackScreen(sampleName: String) {
    // Create a location display using the current application's context
    val locationDisplay = rememberLocationDisplay()
    // Create the map view-model and set the location display to be used for simulation
    val mapViewModel = viewModel<CreateKMLMultiTrackViewModel>().apply {
        setLocationDisplay(locationDisplay)
    }
    // Observe viewmodel states
    val currentKmlMultiTrack by mapViewModel.kmlTracks.collectAsStateWithLifecycle()
    var localMultiTrackGeometries by remember { mutableStateOf<List<Geometry>?>(null) }
    val isShowTracksFromFileEnabled = mapViewModel.isShowTracksFromFileEnabled
    // Update UI between recording option and browse tracks options.
    LaunchedEffect(isShowTracksFromFileEnabled) {
        if (isShowTracksFromFileEnabled) {
            mapViewModel.loadLocalKmlFile(onLocalKmlFileLoaded = { localMultiTrackGeometries = it })
        } else {
            mapViewModel.startNavigation()
            localMultiTrackGeometries = null
        }
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
                    mapViewProxy = mapViewModel.mapViewProxy,
                    locationDisplay = locationDisplay,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay)
                )
                if (!isShowTracksFromFileEnabled) {
                    TrackSimulationOptions(
                        isRecenterEnabled = mapViewModel.isRecenterButtonEnabled,
                        isRecordButtonEnabled = !mapViewModel.isRecordingTrack,
                        kmlTracksSize = currentKmlMultiTrack.size,
                        kmlTrackElementsFlow = mapViewModel.kmlTrackElements,
                        onRecenterClicked = mapViewModel::recenter,
                        onExportClicked = mapViewModel::exportKmlMultiTrack,
                        onRecordButtonClicked = {
                            if (!mapViewModel.isRecordingTrack) {
                                mapViewModel.startRecordingKmlTrack()
                            } else {
                                mapViewModel.stopRecordingKmlTrack()
                            }
                        },
                    )
                } else {
                    TrackBrowseOptions(
                        multiTrackGeometries = localMultiTrackGeometries,
                        onTrackSelected = mapViewModel::previewKmlTrack,
                        onResetButtonClicked = mapViewModel::reset
                    )
                }
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
fun TrackSimulationOptions(
    isRecenterEnabled: Boolean,
    isRecordButtonEnabled: Boolean,
    kmlTrackElementsFlow: StateFlow<List<KmlTrackElement>>,
    kmlTracksSize: Int,
    onRecordButtonClicked: () -> Unit,
    onRecenterClicked: () -> Unit,
    onExportClicked: () -> Unit
) {
    // Observe the track element size
    var kmlTrackElementSize by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        kmlTrackElementsFlow.collect { kmlTrackElementSize = it.size }
    }

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackStatusText(
            isDisplayingTracks = false,
            isRecordingTrackElements = !isRecordButtonEnabled,
            kmlTrackElementsSize = kmlTrackElementSize
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.kml_multi_track_hint),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = kmlTracksSize.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(onClick = onRecenterClicked, enabled = isRecenterEnabled) {
                Icon(
                    painter = painterResource(R.drawable.gps_on_24),
                    contentDescription = "RecenterIcon"
                )
            }
            Button(onClick = onRecordButtonClicked) {
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = if (isRecordButtonEnabled) stringResource(R.string.record_track_hint)
                    else stringResource(R.string.stop_recording_hint)
                )
            }
            FilledTonalIconButton(
                onClick = onExportClicked,
                enabled = isRecordButtonEnabled
            ) {
                Icon(
                    painter = painterResource(R.drawable.save_24),
                    contentDescription = "Export KML multi-track"
                )
            }
        }
    }
}

@Composable
fun TrackStatusText(
    isRecordingTrackElements: Boolean = false,
    kmlTrackElementsSize: Int = 0,
    isDisplayingTracks: Boolean
) {
    Box(Modifier.animateContentSize()) {
        if (isDisplayingTracks) {
            Text(
                text = stringResource(R.string.kml_multi_track_browse_hint),
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            if (isRecordingTrackElements) {
                Text(
                    text = stringResource(R.string.kml_multi_track_recording_hint) + kmlTrackElementsSize,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(R.string.kml_multi_track_start_record_hint),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TrackBrowseOptions(
    multiTrackGeometries: List<Geometry>?,
    onTrackSelected: (Geometry) -> Unit,
    onResetButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackStatusText(isDisplayingTracks = true)
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracks Dropdown Menu
            var expanded by remember { mutableStateOf(false) }
            var selectedTrackIndex by remember { mutableIntStateOf(0) }
            ExposedDropdownMenuBox(
                modifier = Modifier,
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = if (selectedTrackIndex == 0) stringResource(R.string.show_all_kml_tracks_hint)
                    else stringResource(R.string.kml_track_number_hint) + selectedTrackIndex,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    multiTrackGeometries?.forEachIndexed { index, kmlTrackGeometry ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (index == 0) stringResource(R.string.show_all_kml_tracks_hint)
                                    else stringResource(R.string.kml_track_number_hint) + index
                                )
                            },
                            onClick = {
                                onTrackSelected(kmlTrackGeometry)
                                selectedTrackIndex = index
                                expanded = false
                            })
                        // Show a divider between dropdown menu options
                        if (index < multiTrackGeometries.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onResetButtonClicked
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset KML multi-track"
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
            TrackSimulationOptions(
                isRecenterEnabled = true,
                isRecordButtonEnabled = true,
                onRecordButtonClicked = { },
                onRecenterClicked = { },
                onExportClicked = { },
                kmlTracksSize = 1,
                kmlTrackElementsFlow = MutableStateFlow(listOf())
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun TrackBrowseOptionsPreview() {
    SampleAppTheme {
        Surface {
            TrackBrowseOptions(
                multiTrackGeometries = listOf(),
                onTrackSelected = { },
                onResetButtonClicked = { }
            )
        }
    }
}
