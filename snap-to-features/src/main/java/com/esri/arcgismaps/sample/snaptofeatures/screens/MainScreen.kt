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

package com.esri.arcgismaps.sample.snaptofeatures.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.snaptofeatures.R
import com.esri.arcgismaps.sample.snaptofeatures.components.MapViewModel
import com.esri.arcgismaps.sample.snaptofeatures.components.SnapSettings

/**
 * Main screen layout for the sample app.
 */
@Composable
fun MainScreen (sampleName: String) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // get the application property that will be used to construct MapViewModel
    val sampleApplication = LocalContext.current.applicationContext as Application
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(sampleApplication, sampleCoroutineScope) }
    // the collection of graphics overlays used by the MapView
    val graphicsOverlayCollection = listOf(mapViewModel.graphicsOverlay)

    Scaffold(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                SampleTopAppBar(title = sampleName)
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.map,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = graphicsOverlayCollection,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    mapViewInteractionOptions = MapViewInteractionOptions(isMagnifierEnabled = true),
                    onSingleTapConfirmed = mapViewModel::identify,
                    onPan = { mapViewModel.dismissBottomSheet() }
                )
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                    ) {
                        IconButton(
                            enabled = mapViewModel.isCreateButtonEnabled.value,
                            onClick = { expanded = !expanded }
                        ) {
                            Icon(imageVector = Icons.Default.Create, contentDescription = "Start")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Point") },
                                onClick = {
                                    mapViewModel.startEditor(GeometryType.Point)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Multipoint") },
                                onClick = {
                                    mapViewModel.startEditor(GeometryType.Multipoint)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Polyline") },
                                onClick = {
                                    mapViewModel.startEditor(GeometryType.Polyline)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Polygon") },
                                onClick = {
                                    mapViewModel.startEditor(GeometryType.Polygon)
                                    expanded = false
                                }
                            )
                        }
                    }
                    val vector = ImageVector
                    IconButton(onClick = { mapViewModel.editorUndo() }) {
                        Icon(vector.vectorResource(R.drawable.undo), contentDescription = "Undo")
                    }
                    IconButton(onClick = { mapViewModel.stopEditor() }) {
                        Icon(vector.vectorResource(R.drawable.save), contentDescription = "Save")
                    }
                    IconButton(onClick = { mapViewModel.clearGraphics() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ){
                        TextButton(
                            enabled = mapViewModel.isSnapSettingsButtonEnabled.value,
                            onClick = { mapViewModel.showBottomSheet() }
                        ) { Text(text = "Snap Settings") }
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
            BottomSheet(isVisible = mapViewModel.isBottomSheetVisible.value) {
                SnapSettings(
                    onSnappingChanged = mapViewModel::snappingEnabledStatus,
                    isSnappingEnabled = mapViewModel.snappingCheckedState.value,
                    snapSourceList = mapViewModel.snapSourceList.collectAsState(),
                    onSnapSourceChanged = mapViewModel::sourceEnabledStatus,
                    isSnapSourceEnabled = mapViewModel.snapSourceCheckedState
                ) { mapViewModel.dismissBottomSheet() }
            }
        }
    )
}
