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

package com.esri.arcgismaps.sample.createandsavemap.screens

import kotlin.math.roundToInt
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.createandsavemap.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()
    val composableScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                var mapScale by remember { mutableIntStateOf(0) }
                val map = mapViewModel.arcGISMap
                if (map != null) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize(),
                        arcGISMap = map,
                        // update the map scale in the UI on map scale change
                        onMapScaleChanged = { currentMapScale ->
                            if (!currentMapScale.isNaN()) {
                                mapScale = currentMapScale.roundToInt()
                            }
                        },
                    )
                }


                val controlsBottomSheetState =
                    rememberModalBottomSheetState(skipPartiallyExpanded = true)
                // show the "Show controls" button only when the bottom sheet is not visible
                if (!controlsBottomSheetState.isVisible) {
                    ExtendedFloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 36.dp, end = 24.dp),
                        onClick = {
                            composableScope.launch {
                                controlsBottomSheetState.show()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Do stuff")
                        Spacer(Modifier.padding(8.dp))
                        Text("Edit Map")
                    }
                }
                if (controlsBottomSheetState.isVisible) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = controlsBottomSheetState,
                        onDismissRequest = {
                            composableScope.launch {
                                controlsBottomSheetState.hide()
                            }
                        }) {
                        Column(
                            Modifier
                                .padding(12.dp)
                                .navigationBarsPadding()
                        ) {

                            BasemapDropdown(mapViewModel)
                            Spacer(Modifier.size(8.dp))

                            LayersDropdown(mapViewModel)
                            HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Name: ", Modifier.padding(8.dp))
                                TextField(
                                    value = mapViewModel.mapName,
                                    onValueChange = { newName -> mapViewModel.updateName(newName) },
                                    Modifier.width(270.dp)
                                )
                            }
                            Spacer(Modifier.size(8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Tags: ", Modifier.padding(8.dp))
                                TextField(
                                    value = mapViewModel.mapTags,
                                    onValueChange = { newTags -> mapViewModel.updateTags(newTags) },
                                    Modifier.width(270.dp)
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            FolderDropdown(mapViewModel)
                            Spacer(Modifier.size(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Description: ", Modifier.padding(8.dp))
                                TextField(
                                    value = mapViewModel.mapDescription,
                                    onValueChange = { newText ->
                                        mapViewModel.updateDescription(
                                            newText
                                        )
                                    },
                                    Modifier.width(270.dp)
                                )
                            }
                            Spacer(Modifier.size(8.dp))
                            Button(
                                modifier = Modifier.align(Alignment.End),
                                onClick = {
                                    composableScope.launch {
                                        mapViewModel.save().onSuccess {
                                            controlsBottomSheetState.hide()
                                            snackbarHostState.showSnackbar(
                                                "Map saved to portal.",
                                                withDismissAction = true
                                            )
                                        }.onFailure { err ->
                                            controlsBottomSheetState.hide()
                                            snackbarHostState.showSnackbar(
                                                "Error: ${err.message}",
                                                null,
                                                withDismissAction = true,
                                                SnackbarDuration.Indefinite
                                            )
                                        }
                                    }
                                }) {
                                Text("Save to account")
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                }
            }
        })

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDropdown(mapViewModel: MapViewModel) {

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Folder:",
            modifier = Modifier.padding(8.dp)
        )
        var expanded by rememberSaveable { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(270.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            var label by remember {
                mutableStateOf(
                    mapViewModel.portalFolder?.title ?: "(No folder)"
                )
            }
            TextField(
                value = label,
                onValueChange = { label = it },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                Modifier.padding(vertical = 0.dp)
            ) {
                val folders by mapViewModel.portalFolders.collectAsState(initial = listOf())
                if (folders.isEmpty()) {
                    Text("No folders to display", Modifier.padding(8.dp))
                } else {
                    DropdownMenuItem(
                        text = { Text("(No folder)") },
                        onClick = {
                            mapViewModel.updateFolder(null)
                            label = "(No folder)"
                            expanded = false
                        }
                    )
                    HorizontalDivider()
                    folders.forEachIndexed { index, folder ->
                        DropdownMenuItem(
                            text = { Text(folder.title) },
                            onClick = {
                                mapViewModel.updateFolder(folder)
                                label = folder.title
                                expanded = false
                            })
                        // show a divider between dropdown menu options
                        if (index < folders.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasemapDropdown(
    mapViewModel: MapViewModel
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Basemap Style:",
            modifier = Modifier.padding(8.dp)
        )
        var expanded by rememberSaveable { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(220.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = mapViewModel.basemapStyle,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                Modifier.padding(vertical = 0.dp)
            ) {

                mapViewModel.stylesMap.keys.toList().forEachIndexed { index, basemapName ->
                    DropdownMenuItem(
                        text = { Text(basemapName) },
                        onClick = {
                            mapViewModel.updateBasemapStyle(basemapName)
                            expanded = false
                        })
                    // show a divider between dropdown menu options
                    if (index < mapViewModel.stylesMap.keys.toList().lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersDropdown(
    mapViewModel: MapViewModel
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Operational Layers:",
            modifier = Modifier.padding(8.dp)
        )
        var expanded by rememberSaveable { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(220.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            // Todo can this text be updated in real time?
            /*val numLayersSelected = when(mapViewModel.arcGISMap.operationalLayers.size){
                0 -> "Select..."
                else -> "${mapViewModel.arcGISMap.operationalLayers.size} selected"
            }*/

            TextField(
                value = "Select...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                mapViewModel.availableLayers.forEachIndexed { index, layer ->
                    var checked by mutableStateOf(
                        mapViewModel.arcGISMap?.operationalLayers?.contains(
                            layer
                        ) ?: false
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(layer.name, Modifier.weight(1f))
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                mapViewModel.updateActiveLayers(layer)
                                checked = !checked
                            }
                        )
                    }
                    // show a divider between dropdown menu options
                    if (index < mapViewModel.availableLayers.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
