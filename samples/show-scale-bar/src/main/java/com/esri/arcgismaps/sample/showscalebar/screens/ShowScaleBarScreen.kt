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

package com.esri.arcgismaps.sample.showscalebar.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.scalebar.Scalebar
import com.arcgismaps.toolkit.scalebar.ScalebarStyle
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showscalebar.components.ShowScaleBarViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowScaleBarScreen(sampleName: String) {
    val mapViewModel: ShowScaleBarViewModel = viewModel()
    // Keep track of the currently selected scalebar
    var currentScalebarStyle by remember { mutableStateOf(ScalebarStyle.AlternatingBar) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column {
                ScalebarStylesDropDown(
                    currentScalebarStyle = currentScalebarStyle,
                    onScalebarStyleSelected = { currentScalebarStyle = it }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(paddingValues)
                ) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISMap = mapViewModel.arcGISMap,
                        onSpatialReferenceChanged = mapViewModel::updateSpacialReference,
                        onUnitsPerDipChanged = mapViewModel::updateUnitsPerDip,
                        onViewpointChangedForCenterAndScale = mapViewModel::updateViewpoint
                    )

                    Scalebar(
                        modifier = Modifier
                            .padding(25.dp)
                            .align(Alignment.BottomStart),
                        maxWidth = 175.dp,
                        unitsPerDip = mapViewModel.unitsPerDip,
                        viewpoint = mapViewModel.viewpoint,
                        spatialReference = mapViewModel.spatialReference,
                        style = currentScalebarStyle
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
fun ScalebarStylesDropDown(
    currentScalebarStyle: ScalebarStyle,
    onScalebarStyleSelected: (ScalebarStyle) -> Unit
) {
    // List of all the supported scale bar types
    val scalebarStyles = ScalebarStyle.entries.toList()
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                label = { Text("Select Scalebar Style: ") },
                value = currentScalebarStyle.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                scalebarStyles.forEachIndexed { index, scalebarStyle ->
                    DropdownMenuItem(
                        text = { Text(scalebarStyle.name) },
                        onClick = {
                            onScalebarStyleSelected(scalebarStyle)
                            expanded = false
                        })
                    // Show a divider between dropdown menu options
                    if (index < scalebarStyles.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
