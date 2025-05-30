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

package com.esri.arcgismaps.sample.displayoverviewmap.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.OverviewMap
import com.esri.arcgismaps.sample.displayoverviewmap.components.DisplayOverviewMapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplayOverviewMapScreen(sampleName: String) {
    val mapViewModel: DisplayOverviewMapViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    onViewpointChangedForCenterAndScale = {
                        mapViewModel.viewpoint.value = it
                    },
                    onVisibleAreaChanged = {
                        mapViewModel.visibleArea.value = it
                    }
                )
                OverviewMap(
                    viewpoint = mapViewModel.viewpoint.value,
                    visibleArea = mapViewModel.visibleArea.value,
                    modifier = Modifier
                        .size(250.dp, 200.dp)
                        .padding(20.dp)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd)
                )
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
