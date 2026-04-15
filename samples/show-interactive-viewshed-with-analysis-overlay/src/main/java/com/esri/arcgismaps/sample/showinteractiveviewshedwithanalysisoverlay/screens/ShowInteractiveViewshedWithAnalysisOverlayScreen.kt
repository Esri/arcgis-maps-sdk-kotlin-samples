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

package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ShowInteractiveViewshedWithAnalysisOverlayViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowInteractiveViewshedWithAnalysisOverlayScreen(sampleName: String) {
    val viewModel: ShowInteractiveViewshedWithAnalysisOverlayViewModel = viewModel()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            BoxWithConstraints {
                if (maxWidth < maxHeight) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                    ) {
                        MapView(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            arcGISMap = viewModel.arcGISMap,
                            analysisOverlays = listOf(viewModel.analysisOverlay),
                            graphicsOverlays = listOf(viewModel.graphicsOverlay)
                        )
                        // display list of options to modify viewshed parameters
                        ViewshedParametersScreen(
                            viewModel.viewshedParameters,
                            onObserverElevationChanged = viewModel::setObserverElevation,
                            onTargetHeightChanged = viewModel::setTargetHeight,
                            onMaxRadiusChanged = viewModel::setMaxRadius,
                            onFieldOfViewChanged = viewModel::setFieldOfView,
                            onHeadingChanged = viewModel::setHeading,
                            onElevationSamplingIntervalChanged = viewModel::setElevationSamplingInterval
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                    ) {
                        MapView(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            arcGISMap = viewModel.arcGISMap,
                            analysisOverlays = listOf(viewModel.analysisOverlay),
                            graphicsOverlays = listOf(viewModel.graphicsOverlay)
                        )
                        // display list of options to modify viewshed parameters
                        ViewshedParametersScreen(
                            viewModel.viewshedParameters,
                            onObserverElevationChanged = viewModel::setObserverElevation,
                            onTargetHeightChanged = viewModel::setTargetHeight,
                            onMaxRadiusChanged = viewModel::setMaxRadius,
                            onFieldOfViewChanged = viewModel::setFieldOfView,
                            onHeadingChanged = viewModel::setHeading,
                            onElevationSamplingIntervalChanged = viewModel::setElevationSamplingInterval
                        )
                    }
                }
            }

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
