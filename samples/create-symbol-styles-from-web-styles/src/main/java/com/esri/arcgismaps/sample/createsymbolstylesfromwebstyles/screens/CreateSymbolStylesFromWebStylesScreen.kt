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

package com.esri.arcgismaps.sample.createsymbolstylesfromwebstyles.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.createsymbolstylesfromwebstyles.components.CreateSymbolStylesFromWebStylesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun CreateSymbolStylesFromWebStylesScreen(sampleName: String) {

    // Get a reference to the view model
    val mapViewModel: CreateSymbolStylesFromWebStylesViewModel = viewModel()

    // Keep track of legend visibility state
    var showLegend by remember { mutableStateOf(false) }

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(), arcGISMap = mapViewModel.arcGISMap,
                // Hide the legend on any tap of the map view
                onDown = { showLegend = false },
                // Update the map scale in the view model
                onMapScaleChanged = mapViewModel::onMapScaleChanged
            )
            // Show the FAB
            if (!showLegend) {
                FloatingActionButton(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 36.dp, end = 24.dp),
                    // On click, show the legend
                    onClick = { showLegend = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Show legend button")
                    Spacer(Modifier.padding(8.dp))
                }
            } else {
                // Show the legend
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 36.dp, end = 12.dp)
                        .background(Color.White)
                ) {
                    // For each pair of symbol name and icon
                    for ((symbolName, symbolIcon) in symbolsAndIcons) {
                        Row(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
                        ) {
                            // Show the symbol
                            Image(
                                bitmap = symbolIcon.bitmap.asImageBitmap(), contentDescription = "Symbol image"
                            )
                            // Show the symbol name
                            Text(modifier = Modifier.padding(start = 8.dp), text = symbolName)
                        }
                    }
                }
            }
        }

        // Show any errors in a message dialog
        mapViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                )
            }
        }
    })
}
