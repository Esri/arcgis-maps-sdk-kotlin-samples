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

package com.esri.arcgismaps.sample.configureclusters.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configureclusters.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // get the application context
    val application = LocalContext.current.applicationContext as Application
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                var mapScale by remember { mutableIntStateOf(0) }
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        mapViewModel.identify(singleTapConfirmedEvent)
                    },
                    arcGISMap = mapViewModel.arcGISMap,
                    onMapScaleChanged = { currentMapScale ->
                        if (!currentMapScale.isNaN()) {
                            mapScale = currentMapScale.roundToInt()
                        }
                    },
                )
                // boolean to toggle the state of the bottom sheet layout
                var showControlsBottomSheet by rememberSaveable { mutableStateOf(true) }
                val controlsBottomSheetState = rememberModalBottomSheetState()
                // show the "Show controls" button only when the bottom sheet is not visible
                if (!showControlsBottomSheet) {
                    Button(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        onClick = {
                            showControlsBottomSheet = true
                        },
                    ) {
                        Text(
                            text = "Show controls"
                        )
                    }
                }
                // constrain the bottom sheet to a maximum width of 380dp
                Box(
                    modifier = Modifier
                        .widthIn(0.dp, 380.dp)
                ) {
                    if (showControlsBottomSheet) {
                        ModalBottomSheet(
                            sheetState = controlsBottomSheetState,
                            onDismissRequest = {
                                showControlsBottomSheet = false
                            }) {
                            Column {
                                Text("Cluster labels visibility:")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Show labels")
                                    Switch(
                                        checked = mapViewModel.showClusterLabels,
                                        onCheckedChange = { showClusterLabels ->
                                            mapViewModel.updateClusterLabelState(
                                                showClusterLabels
                                            )
                                        },
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Divider(
                                    color = Color.LightGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .width(2.dp)
                                )
                                ClusterRadiusControls(
                                    mapViewModel.clusterRadiusOptions,
                                    mapViewModel.clusterRadius,
                                    mapViewModel::updateClusterRadius
                                )
                                ClusterMaxScaleControls(
                                    mapViewModel.clusterMaxScaleOptions,
                                    mapViewModel.clusterMaxScale,
                                    mapViewModel::updateClusterMaxScale
                                )
                                Row(modifier = Modifier.padding(bottom = 40.dp)) {
                                    Text("Current map scale: 1:${mapScale}")
                                }
                            }

                        }
                    }

                }
                // display a bottom sheet to show popup details
                BottomSheet(isVisible = mapViewModel.showPopUp, bottomSheetContent = {
                    ClusterInfoContent(
                        popUpTitle = mapViewModel.popUpTitle,
                        popUpInfo = mapViewModel.popUpInfo,
                        onShow = { showControlsBottomSheet = false },
                        onDismiss = { mapViewModel.updateShowPopUpState(false) }
                    )
                })
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClusterRadiusControls(
    clusterRadiusOptions: List<Int>,
    clusterRadius: Int,
    updateClusterRadius: (Int) -> Unit
) {
    Text("Clustering properties:")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Cluster radius")
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .wrapContentSize(Alignment.TopStart)
                .padding(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = clusterRadius.toString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    clusterRadiusOptions.forEachIndexed { index, clusterRadius ->
                        DropdownMenuItem(
                            text = { Text(clusterRadius.toString()) },
                            onClick = {
                                updateClusterRadius(
                                    index
                                )
                                expanded = false
                            })
                        // show a divider between dropdown menu options
                        if (index < clusterRadiusOptions.lastIndex) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClusterMaxScaleControls(
    clusterMaxScaleOptions: List<Int>,
    clusterMaxScale: Int,
    updateClusterMaxScale: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Cluster max scale")
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .wrapContentSize(Alignment.TopStart)
                .padding(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = clusterMaxScale.toString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    clusterMaxScaleOptions.forEachIndexed { index, clusterRadius ->
                        DropdownMenuItem(
                            text = { Text(clusterRadius.toString()) },
                            onClick = {
                                updateClusterMaxScale(
                                    index
                                )
                                expanded = false
                            })
                        // show a divider between dropdown menu options
                        if (index < clusterMaxScaleOptions.lastIndex) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClusterInfoContent(
    popUpTitle: String,
    popUpInfo: Map<String, Any?>,
    onShow: () -> Unit,
    onDismiss: () -> Unit
) {

    onShow()
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 30.dp, vertical = 12.dp)
                    .weight(6f),
                text = popUpTitle,
                style = SampleTypography.displaySmall
            )

            IconButton(
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close button"
                )
            }
        }
        Text(
            text = popUpInfo.map { "${it.key}: ${it.value}" }.joinToString("\n"),
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 16.dp)
        )
        Spacer(modifier = Modifier.size(24.dp))
    }
}
