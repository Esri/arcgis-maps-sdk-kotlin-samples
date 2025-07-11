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

package com.esri.arcgismaps.sample.editgeometrieswithprogrammaticreticletool.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editgeometrieswithprogrammaticreticletool.components.EditGeometriesWithProgrammaticReticleToolViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun EditGeometriesWithProgrammaticReticleToolScreen(sampleName: String) {
    val mapViewModel: EditGeometriesWithProgrammaticReticleToolViewModel = viewModel()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            val buttonText by mapViewModel.multiButtonText.collectAsState()
            var isSettingsVisible  by remember { mutableStateOf(false) }
            val startingGeometryType by mapViewModel.startingGeometryType.collectAsState()

            if (isSettingsVisible) {
                SettingsScreen(
                    currentGeometryType = startingGeometryType,
                    onGeometryTypeSelected = { newGeometryType -> mapViewModel.setStartingGeometryType(newGeometryType) },
                    onDismissRequest = { isSettingsVisible = false }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = { isSettingsVisible = true }) {
                    Text(text = "Settings")
                }
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = mapViewModel.graphicsOverlays,
                    geometryEditor = mapViewModel.geometryEditor,
                    onSingleTapConfirmed = mapViewModel::onMapViewTap,
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = mapViewModel::onButtonClick) {
                    Text(text = buttonText)
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
