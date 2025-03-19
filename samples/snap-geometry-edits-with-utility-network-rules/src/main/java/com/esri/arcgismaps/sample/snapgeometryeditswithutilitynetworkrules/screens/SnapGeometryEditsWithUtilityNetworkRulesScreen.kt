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

package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun SnapGeometryEditsWithUtilityNetworkRulesScreen(sampleName: String) {
    val mapViewModel = viewModel<SnapGeometryEditsWithUtilityNetworkRulesViewModel>()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Absolute.Left
                ) {
                    Text(text = "Feature selected:", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = "AssetGroup: ")
                    Text(text = mapViewModel.assetGroupNameState.collectAsState().value)
                }
                HorizontalDivider()
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = "AssetType: ")
                    Text(text = mapViewModel.assetTypeNameState.collectAsState().value)
                }

                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onSingleTapConfirmed = mapViewModel::identify
                )
                SnapSourcesPanel(
                    snapSourcePropertyList = mapViewModel.snapSourcePropertyList.collectAsState(),
                    onSnapSourcePropertyChanged = mapViewModel::updateSnapSourceProperty,
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    IconButton(
                        enabled = mapViewModel.isEditButtonEnabled.collectAsState().value,
                        onClick = { mapViewModel.editFeatureGeometry() }
                    ) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = "Start")
                    }
                    IconButton(
                        enabled = mapViewModel.geometryEditor.isStarted.collectAsState().value,
                        onClick = { mapViewModel.discardGeometryChanges() }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard")
                    }
                    IconButton(
                        enabled = (mapViewModel.geometryEditor.isStarted.collectAsState().value &&
                                mapViewModel.geometryEditor.canUndo.collectAsState().value),
                        onClick = { mapViewModel.saveGeometryChanges() }
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                    }
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
