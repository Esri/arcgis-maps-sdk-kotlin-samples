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

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configureclusters.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()

    val composableScope = rememberCoroutineScope()

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
                    // identify on single tap
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        mapViewModel.identify(singleTapConfirmedEvent)
                    },
                    arcGISMap = mapViewModel.arcGISMap,
                    // update the map scale in the UI on map scale change
                    onMapScaleChanged = { currentMapScale ->
                        if (!currentMapScale.isNaN()) {
                            mapScale = currentMapScale.roundToInt()
                        }
                    },
                )

                val controlsBottomSheetState =
                    rememberModalBottomSheetState(skipPartiallyExpanded = true)
                // show the "Show controls" button only when the bottom sheet is not visible
                if (!controlsBottomSheetState.isVisible) {
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 36.dp, end = 24.dp),
                        onClick = {
                            composableScope.launch {
                                controlsBottomSheetState.show()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Show controls")
                    }
                }
                if (controlsBottomSheetState.isVisible) {
                    ClusterControlsBottomSheet(
                        composableScope = composableScope,
                        controlsBottomSheetState = controlsBottomSheetState,
                        showClusterLabels = mapViewModel.showClusterLabels,
                        updateClusterLabelState = mapViewModel::updateShowClusterLabelState,
                        clusterRadiusOptions = mapViewModel.clusterRadiusOptions,
                        clusterRadius = mapViewModel.clusterRadius,
                        updateClusterRadiusState = mapViewModel::updateClusterRadiusState,
                        clusterMaxScaleOptions = mapViewModel.clusterMaxScaleOptions,
                        clusterMaxScale = mapViewModel.clusterMaxScale,
                        updateClusterMaxScaleState = mapViewModel::updateClusterMaxScaleState,
                        mapScale = mapScale
                    )
                }
            }

            // display a bottom sheet to show popup details
            BottomSheet(
                isVisible = mapViewModel.showPopUpContent,
                bottomSheetContent = {
                    ClusterInfoContent(
                        popUpTitle = mapViewModel.popUpTitle,
                        popUpInfo = mapViewModel.popUpInfo,
                        onDismiss = { mapViewModel.updateShowPopUpContentState(false) }
                    )
                })

        }
    )
}

/**
 * Composable function to display the cluster controls bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClusterControlsBottomSheet(
    composableScope: CoroutineScope,
    controlsBottomSheetState: SheetState,
    showClusterLabels: Boolean,
    updateClusterLabelState: (Boolean) -> Unit,
    clusterRadiusOptions: List<Int>,
    clusterRadius: Int,
    updateClusterRadiusState: (Int) -> Unit,
    clusterMaxScaleOptions: List<Int>,
    clusterMaxScale: Int,
    updateClusterMaxScaleState: (Int) -> Unit,
    mapScale: Int,
) {
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
                .navigationBarsPadding()) {
            Text(
                "Cluster labels visibility:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show labels")
                Switch(
                    checked = showClusterLabels,
                    onCheckedChange = { showClusterLabels ->
                        updateClusterLabelState(
                            showClusterLabels
                        )
                    }
                )
            }
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current map scale:")
                Text("1:$mapScale")
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
            Text(
                "Clustering properties:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.size(8.dp))
            ClusterRadiusControls(
                clusterRadiusOptions,
                clusterRadius,
                updateClusterRadiusState
            )
            Spacer(Modifier.size(8.dp))
            ClusterMaxScaleControls(
                clusterMaxScaleOptions,
                clusterMaxScale,
                updateClusterMaxScaleState
            )
        }
    }
}

/**
 * Composable function to display the cluster radius controls within the cluster controls bottom
 * sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClusterRadiusControls(
    clusterRadiusOptions: List<Int>,
    clusterRadius: Int,
    updateClusterRadius: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Cluster radius",
            modifier = Modifier.padding(8.dp)
        )
        var expanded by rememberSaveable { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(150.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = clusterRadius.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                clusterRadiusOptions.forEachIndexed { index, clusterRadius ->
                    DropdownMenuItem(
                        text = { Text(clusterRadius.toString()) },
                        onClick = {
                            updateClusterRadius(index)
                            expanded = false
                        })
                    // show a divider between dropdown menu options
                    if (index < clusterRadiusOptions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Composable function to display the cluster max scale controls within the cluster controls bottom
 * sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClusterMaxScaleControls(
    clusterMaxScaleOptions: List<Int>,
    clusterMaxScale: Int,
    updateClusterMaxScale: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Cluster max scale",
            modifier = Modifier.padding(8.dp)
        )
        var expanded by rememberSaveable { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(150.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = clusterMaxScale.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                clusterMaxScaleOptions.forEachIndexed { index, clusterRadius ->
                    DropdownMenuItem(
                        text = { Text(clusterRadius.toString()) },
                        onClick = {
                            updateClusterMaxScale(index)
                            expanded = false
                        })
                    // show a divider between dropdown menu options
                    if (index < clusterMaxScaleOptions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Composable function to display the cluster info content from the pop up within a bottom sheet.
 */
@Composable
private fun ClusterInfoContent(
    popUpTitle: String,
    popUpInfo: Map<String, Any?>,
    onDismiss: () -> Unit
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = popUpTitle.ifEmpty { "Cluster Info:" },
                style = SampleTypography.headlineSmall
            )
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close button"
                )
            }
        }
        popUpInfo.forEach {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 30.dp,
                        vertical = 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${it.key}:", style = MaterialTheme.typography.labelMedium)
                Text(text = "${it.value}")
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}
