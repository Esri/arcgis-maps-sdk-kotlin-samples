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

package com.esri.arcgismaps.sample.addwfslayer.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addwfslayer.components.AddWfsLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the AddWfsLayer sample app.
 */
@Composable
fun AddWfsLayerScreen(sampleName: String) {
    val mapViewModel: AddWfsLayerViewModel = viewModel()
    val isPopulating by mapViewModel.isPopulating.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        arcGISMap = mapViewModel.arcGISMap,
                        onVisibleAreaChanged = { polygon: Polygon ->
                            mapViewModel.onVisibleAreaChanged(polygon)
                        },
                        onNavigationChanged = { isNavigating ->
                            mapViewModel.onNavigatingChanged(isNavigating)
                        }
                    )
                }
                if (isPopulating) {
                    LoadingDialog(loadingMessage = "Populating features...")
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
