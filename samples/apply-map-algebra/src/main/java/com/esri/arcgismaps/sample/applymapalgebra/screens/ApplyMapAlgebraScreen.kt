/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.applymapalgebra.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applymapalgebra.components.ApplyMapAlgebraViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun ApplyMapAlgebraScreen(sampleName: String) {
    val viewModel: ApplyMapAlgebraViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {

                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = viewModel.arcGISMap
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (!viewModel.hasMapAlgebraResults || viewModel.isPerformingAnalysis) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.categorizeElevation() },
                            enabled = !viewModel.isPerformingAnalysis
                        ) {
                            if (viewModel.isPerformingAnalysis) {
                                Text("Categorizing...")
                            } else {
                                Text("Categorize")
                            }
                        }
                    } else {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.availableLayerNames.forEachIndexed { index, layerName ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = viewModel.availableLayerNames.size
                                    ),
                                    selected = layerName == viewModel.selectedRasterLayerName,
                                    onClick = { viewModel.selectRasterLayer(layerName) }
                                ) {
                                    Text(
                                        text = layerName,
                                        fontWeight = if (layerName == viewModel.selectedRasterLayerName) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show a message dialog if the viewmodel reported an error
            viewModel.messageDialogVM.apply {
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
