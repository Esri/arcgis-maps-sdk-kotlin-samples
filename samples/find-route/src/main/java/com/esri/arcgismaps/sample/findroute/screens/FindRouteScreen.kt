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

package com.esri.arcgismaps.sample.findroute.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.tasks.networkanalysis.DirectionManeuver
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.findroute.R
import com.esri.arcgismaps.sample.findroute.components.FindRouteViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun FindRouteScreen(sampleName: String) {
    val mapViewModel: FindRouteViewModel = viewModel()

    var isRouteTaskRunning by remember { mutableStateOf(false) }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val isDirectionsAvailable = mapViewModel.directions.isNotEmpty()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.map,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // solve route button
                    OutlinedButton(onClick = {
                        // show loading dialog
                        isRouteTaskRunning = true
                        // run the route task to solve route
                        mapViewModel.solveRoute(
                            onSolveRouteCompleted = {
                                isRouteTaskRunning = false
                                isBottomSheetVisible = true
                            }
                        )
                    }) { Text("Solve route directions") }
                    // directions icon button
                    FilledTonalIconButton(
                        enabled = isDirectionsAvailable,
                        onClick = { if (isDirectionsAvailable) isBottomSheetVisible = true })
                    {
                        Icon(
                            modifier = Modifier.size(30.dp),
                            painter = painterResource(R.drawable.ic_navigate),
                            contentDescription = "Directions icon",
                            tint = if (!isDirectionsAvailable) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Bottom sheet to display list of direction maneuvers
            BottomSheet(isBottomSheetVisible) {
                DirectionManeuversSheet(
                    directions = mapViewModel.directions,
                    routeDirectionsInfo = mapViewModel.routeDirectionsInfo,
                    onDirectionManeuverSelected = {
                        isBottomSheetVisible = false
                        mapViewModel.selectDirectionManeuver(it)
                    },
                    onDismissSelected = {
                        isBottomSheetVisible = false
                    }
                )
            }

            // display a MessageDialog if the sample encounters an error
            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }

            if (isRouteTaskRunning) {
                LoadingDialog("Solving route ...")
            }
        }
    )
}

@Composable
fun DirectionManeuversSheet(
    directions: List<DirectionManeuver>,
    routeDirectionsInfo: String,
    onDirectionManeuverSelected: (DirectionManeuver) -> Unit,
    onDismissSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            // Use 1/2 screen height to display sheet
            .height((LocalConfiguration.current.screenHeightDp * 0.5).dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = "Directions: $routeDirectionsInfo",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            FilledTonalIconButton(onClick = { onDismissSelected() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss sheet"
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            itemsIndexed(directions) { index: Int, directionManeuver: DirectionManeuver ->
                Column(Modifier.clickable { onDirectionManeuverSelected(directionManeuver) }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        text = directionManeuver.directionText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (index < directions.size - 1)
                        HorizontalDivider()
                }
            }
        }
    }
}
