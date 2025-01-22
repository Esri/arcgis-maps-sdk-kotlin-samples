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

package com.esri.arcgismaps.sample.cutgeometry.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.cutgeometry.R
import com.esri.arcgismaps.sample.cutgeometry.components.CutGeometryViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app.
 */
@Composable
fun CutGeometryScreen(sampleName: String) {
    val mapViewModel: CutGeometryViewModel = viewModel()
    val isResetButtonEnabled by mapViewModel.isResetButtonEnabled.collectAsStateWithLifecycle()
    val isCutButtonEnabled by mapViewModel.isCutButtonEnabled.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewProxy = mapViewModel.mapViewProxy
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier.padding(12.dp),
                        enabled = isResetButtonEnabled,
                        onClick = {
                            mapViewModel.resetGeometry()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reset_button_text)
                        )
                    }
                    Button(
                        modifier = Modifier.padding(12.dp),
                        enabled = isCutButtonEnabled,
                        onClick = {
                            mapViewModel.cutGeometry()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.cut_geometry_button_text)
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
        }
    )
}
