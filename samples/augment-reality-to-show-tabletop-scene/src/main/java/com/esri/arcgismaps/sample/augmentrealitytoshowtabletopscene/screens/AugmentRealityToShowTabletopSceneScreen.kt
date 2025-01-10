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

package com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.ar.TableTopSceneView
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.components.AugmentRealityToShowTabletopSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplaySceneInTabletopARScreen(sampleName: String) {
    val sceneViewModel: AugmentRealityToShowTabletopSceneViewModel = viewModel()
    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = { paddingValues ->
        sceneViewModel.scene?.let {
            TableTopSceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                arcGISScene = it,
                arcGISSceneAnchor = Point(
                    x = -75.16996728256345,
                    y = 39.95787000283599,
                    spatialReference = SpatialReference.wgs84()
                ),
                translationFactor = 800.0,
                clippingDistance = 800.0,
            )
        }

        sceneViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                )
            }
        }
    })
}