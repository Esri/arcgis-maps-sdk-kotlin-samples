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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.findroute.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun FindRouteScreen(sampleName: String) {
    val mapViewModel: FindRouteViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    val isRouteSolved = mapViewModel.directions.isNotEmpty()

    BottomSheetScaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        scaffoldState = sheetState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            DirectionManeuversSheet(
                directions = mapViewModel.directions,
                routeDirectionsInfo = mapViewModel.routeDirectionsInfo,
                onDirectionManeuverSelected = {
                    mapViewModel.selectDirectionManeuver(it)
                },
                onDismissSelected = {
                    scope.launch { sheetState.bottomSheetState.hide() }
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    OutlinedButton(
                        onClick = { scope.launch { mapViewModel.solveRoute() } }
                    ) {
                        Text("Solve route directions")
                    }

                    FilledTonalIconButton(
                        enabled = isRouteSolved,
                        onClick = {
                            scope.launch {
                                if (isRouteSolved) sheetState.bottomSheetState.expand()
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(30.dp).padding(4.dp),
                            painter = painterResource(R.drawable.ic_navigate),
                            contentDescription = "Directions icon",
                            tint = if (!isRouteSolved) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
    val height = LocalConfiguration.current.screenHeightDp / 2
    Column(Modifier.height(height.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = "Directions: $routeDirectionsInfo",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            IconButton(onClick = { onDismissSelected() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss sheet",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(12.dp)
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
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (index < directions.size - 1)
                        HorizontalDivider()
                }
            }
        }
    }
}
