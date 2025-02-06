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

package com.esri.arcgismaps.sample.filterfeaturesinscene.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.filterfeaturesinscene.R
import com.esri.arcgismaps.sample.filterfeaturesinscene.components.FilterFeaturesInSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun FilterFeaturesInSceneScreen(sampleName: String) {
    val sceneViewModel: FilterFeaturesInSceneViewModel = viewModel()

    var filteringScene by remember { mutableStateOf(false) }

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISScene = sceneViewModel.arcGISScene,
                graphicsOverlays = listOf(sceneViewModel.graphicsOverlay)
            )
            // display a MessageDialog if the sample encounters an error
            sceneViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    }, floatingActionButton = {
        FloatingActionButton(
            modifier = Modifier.padding(bottom = 16.dp),
            onClick = {
                if (!filteringScene) {
                    sceneViewModel.filterScene()
                    filteringScene = true
                } else {
                    sceneViewModel.resetFilter()
                    filteringScene = false
                }
            }) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = if (!filteringScene) {
                    stringResource(R.string.filter_osm_buildings)
                } else {
                    stringResource(R.string.reset_filter)
                }
            )
        }
    }, floatingActionButtonPosition = FabPosition.Center
    )
}
