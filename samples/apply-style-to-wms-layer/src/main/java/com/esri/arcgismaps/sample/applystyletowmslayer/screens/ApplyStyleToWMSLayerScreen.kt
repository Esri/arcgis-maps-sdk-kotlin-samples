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

package com.esri.arcgismaps.sample.applystyletowmslayer.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applystyletowmslayer.components.ApplyStyleToWmsLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the Apply style to WMS layer sample.
 */
@Composable
fun ApplyStyleToWmsLayerScreen(sampleName: String) {
    val mapViewModel: ApplyStyleToWmsLayerViewModel = viewModel()

    // Observe WMS styles and selected index from the ViewModel
    val styles: List<String> by mapViewModel.stylesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedStyleIndex: Int by mapViewModel.selectedStyleIndex.collectAsStateWithLifecycle(initialValue = 0)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy
                )

                // Style Picker UI
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Style")

                        val styleTitles = styles.map { styleId ->
                            when (styleId.lowercase()) {
                                "default" -> "Default"
                                "stretch" -> "Contrast Stretch"
                                else -> styleId.ifBlank { "Unknown" }
                            }
                        }

                        DropDownMenuBox(
                            textFieldValue = styleTitles.getOrNull(selectedStyleIndex) ?: "",
                            textFieldLabel = "Choose WMS style",
                            dropDownItemList = styleTitles,
                            onIndexSelected = mapViewModel::updateSelectedStyle
                        )
                    }
                }
            }

            // Display a dialog if the sample encounters an error
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
