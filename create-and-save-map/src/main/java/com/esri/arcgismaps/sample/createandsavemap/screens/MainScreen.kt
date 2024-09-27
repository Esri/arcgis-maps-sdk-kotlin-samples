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

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.createandsavemap.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

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
    val controlsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var isBottomSheetVisible by remember { mutableStateOf(false) }

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
                // map view not shown until the portal is loaded and a basemap has been set
                MapView(
                    arcGISMap = mapViewModel.arcGISMap,
                    Modifier.fillMaxSize(),
                )

                // show the "Edit map" button only when the bottom sheet is not visible
                if (!isBottomSheetVisible) {
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 36.dp, end = 24.dp),
                        onClick = {
                            isBottomSheetVisible = true
                        })
                    {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit map button")
                        Spacer(Modifier.padding(8.dp))
                    }
                }

                if (isBottomSheetVisible) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = controlsBottomSheetState,
                        onDismissRequest = {
                            isBottomSheetVisible = false
                        }
                    ) {
                        Column(
                            Modifier
                                .padding(12.dp)
                                .navigationBarsPadding()
                        ) {
                            CreateMapBottomSheet(
                                mapViewModel = mapViewModel
                            )

                            Button(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(12.dp),
                                onClick = {
                                    composableScope.launch {
                                        // dismiss bottom sheet immediately
                                        isBottomSheetVisible = false

                                        // report success in a snack bar, failure in a popup
                                        mapViewModel.save().onSuccess {
                                            snackbarHostState.showSnackbar(
                                                "Map saved to portal.",
                                                withDismissAction = true
                                            )
                                        }.onFailure { err ->
                                            mapViewModel.messageDialogVM.showMessageDialog(
                                                "Error",
                                                err.message.toString()
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

            // message dialog can draw over all other content in the main screen
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

/**
 * Composable function to populate bottom sheet with sample options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMapBottomSheet(
    mapViewModel: MapViewModel
) {
    Text(
        "Choose map settings:",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(Modifier.size(8.dp))


    BasemapDropdown(
        mapViewModel.selectedBasemapStyle,
        mapViewModel.stylesMap,
        mapViewModel::updateBasemapStyle
    )
    Spacer(Modifier.size(8.dp))

    LayersDropdown(
        mapViewModel.availableLayers,
        mapViewModel.arcGISMap,
        mapViewModel::updateActiveLayers
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 8.dp))

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = mapViewModel.mapName,
        onValueChange = { newName -> mapViewModel.updateName(newName) },
        label = { Text(text = "Enter map name:") },
        singleLine = true,
        supportingText = {}
    )

    Spacer(Modifier.size(8.dp))

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = mapViewModel.mapTags,
        onValueChange = { newTags -> mapViewModel.updateTags(newTags) },
        label = { Text(text = "Tags:") },
        singleLine = true,
        supportingText = {}
    )

    Spacer(Modifier.size(8.dp))

    FolderDropdown(
        mapViewModel.portalFolder,
        mapViewModel.portalFolders,
        mapViewModel::updateFolder
    )

    Spacer(Modifier.size(8.dp))

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = mapViewModel.mapDescription,
        onValueChange = { newDescription -> mapViewModel.updateDescription(newDescription) },
        label = { Text(text = "Description:") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        supportingText = {}
    )

    Spacer(Modifier.size(8.dp))
}

/**
 * Composable function to display portal folder dropdown in the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDropdown(
    currentFolder: PortalFolder?,
    portalFolders: StateFlow<List<PortalFolder>>,
    updateFolder: (PortalFolder?) -> Unit
) {

    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        var label by remember {
            mutableStateOf(
                currentFolder?.title ?: "(No folder)"
            )
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = label,
            onValueChange = { newDescription -> label = newDescription },
            label = { Text(text = "Folder:") },
            supportingText = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            Modifier.padding(vertical = 0.dp)
        ) {
            val folders by portalFolders.collectAsState(initial = listOf())
            if (folders.isEmpty()) {
                Text("No folders to display", Modifier.padding(8.dp))
            } else {
                DropdownMenuItem(
                    text = { Text("(No folder)") },
                    onClick = {
                        updateFolder(null)
                        label = "(No folder)"
                        expanded = false
                    }
                )
                HorizontalDivider()
                folders.forEachIndexed { index, folder ->
                    DropdownMenuItem(
                        text = { Text(folder.title) },
                        onClick = {
                            updateFolder(folder)
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

/**
 * Composable function to display basemap dropdown in the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasemapDropdown(
    basemapStyle: String,
    stylesNameMap: Map<String, BasemapStyle>,
    updateBasemapStyle: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }


    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = basemapStyle,
            onValueChange = {},
            label = { Text(text = "Basemap Style:") },
            supportingText = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            Modifier.padding(vertical = 0.dp)
        ) {

            stylesNameMap.keys.toList().forEachIndexed { index, basemapName ->
                DropdownMenuItem(
                    text = { Text(basemapName) },
                    onClick = {
                        updateBasemapStyle(basemapName)
                        expanded = false
                    })
                // show a divider between dropdown menu options
                if (index < stylesNameMap.keys.toList().lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Composable function to display active layers dropdown in the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersDropdown(
    availableLayers: List<Layer>,
    arcGISMap: ArcGISMap,
    updateActiveLayers: (Layer) -> Unit

) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = "Select...",
            onValueChange = {},
            label = { Text(text = "Operational Layers:") },
            supportingText = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableLayers.forEachIndexed { index, layer ->
                var checked by
                mutableStateOf(
                    arcGISMap.operationalLayers.contains(layer)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(layer.name, Modifier.weight(1f))
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            updateActiveLayers(layer)
                            checked = !checked
                        }
                    )
                }
                // show a divider between dropdown menu options
                if (index < availableLayers.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
