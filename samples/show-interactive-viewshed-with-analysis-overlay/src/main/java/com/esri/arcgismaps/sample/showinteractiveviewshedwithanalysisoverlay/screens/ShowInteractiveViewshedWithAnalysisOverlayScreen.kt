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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ShowInteractiveViewshedWithAnalysisOverlayViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowInteractiveViewshedWithAnalysisOverlayScreen(sampleName: String) {
    val viewModel: ShowInteractiveViewshedWithAnalysisOverlayViewModel = viewModel()

    // Create a MapViewProxy, used to convert screen points to map points
    val mapViewProxy = MapViewProxy()

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
                        RasterDataCopyrightText()
                        MapView(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            arcGISMap = viewModel.arcGISMap,
                            mapViewProxy = mapViewProxy,
                            mapViewInteractionOptions = MapViewInteractionOptions(isPanEnabled = false),
                            analysisOverlays = listOf(viewModel.analysisOverlay),
                            graphicsOverlays = listOf(viewModel.graphicsOverlay),
                            onSingleTapConfirmed = { event ->
                                viewModel.onTap(event.mapPoint)
                            },
                            onLongPress = { event ->
                                viewModel.onLongPress(event)
                            },
                            onPan = { event ->
                                viewModel.onPan(event, mapViewProxy)
                            }
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                        ) {
                            RasterDataCopyrightText()
                            MapView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                arcGISMap = viewModel.arcGISMap,
                                mapViewProxy = mapViewProxy,
                                mapViewInteractionOptions = MapViewInteractionOptions(isPanEnabled = false),
                                analysisOverlays = listOf(viewModel.analysisOverlay),
                                graphicsOverlays = listOf(viewModel.graphicsOverlay),
                                onSingleTapConfirmed = { event ->
                                    viewModel.onTap(event.mapPoint)
                                },
                                onLongPress = { event ->
                                    viewModel.onLongPress(event)
                                },
                                onPan = { event ->
                                    viewModel.onPan(event, mapViewProxy)
                                }
                            )
                        }
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

@Composable
fun RasterDataCopyrightText() {
    Text(
        text = "Raster data copyright Scottish Government and SEPA (2014)",
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
    )
}