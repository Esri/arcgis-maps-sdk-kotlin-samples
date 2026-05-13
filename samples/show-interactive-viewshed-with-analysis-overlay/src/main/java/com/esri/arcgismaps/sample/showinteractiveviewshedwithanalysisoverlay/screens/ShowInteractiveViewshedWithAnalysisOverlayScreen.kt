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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.R
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ShowInteractiveViewshedWithAnalysisOverlayViewModel
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ViewshedUiState

/**
 * Main screen layout for the sample app.
 */
@Composable
fun ShowInteractiveViewshedWithAnalysisOverlayScreen() {
    val viewModel: ShowInteractiveViewshedWithAnalysisOverlayViewModel = viewModel()
    val uiState by viewModel.viewshedUiState.collectAsStateWithLifecycle()

    MainScreenScaffold(
        uiState = uiState,
        onObserverElevationChanged = viewModel::setObserverElevation,
        onTargetHeightChanged = viewModel::setTargetHeight,
        onMaxRadiusChanged = viewModel::setMaxRadius,
        onFieldOfViewChanged = viewModel::setFieldOfView,
        onHeadingChanged = viewModel::setHeading,
        onElevationSamplingIntervalChanged = viewModel::setElevationSamplingInterval,
        mainPaneContent = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                RasterDataCopyrightText()
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = viewModel.arcGISMap,
                    mapViewProxy = viewModel.mapViewProxy,
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
                        viewModel.onPan(event, viewModel.mapViewProxy)
                    }
                )
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
        }
    )
}

@Composable
private fun MainScreenScaffold(
    uiState: ViewshedUiState,
    onObserverElevationChanged: (Float) -> Unit = {},
    onTargetHeightChanged: (Float) -> Unit = {},
    onMaxRadiusChanged: (Float) -> Unit = {},
    onFieldOfViewChanged: (Float) -> Unit = {},
    onHeadingChanged: (Float) -> Unit = {},
    onElevationSamplingIntervalChanged: (Double) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_interactive_viewshed_with_analysis_overlay_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                supportingPaneTitle = "Viewshed Parameters",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ViewshedSupportingContent(
                        uiState = uiState,
                        onObserverElevationChanged = onObserverElevationChanged,
                        onTargetHeightChanged = onTargetHeightChanged,
                        onMaxRadiusChanged = onMaxRadiusChanged,
                        onFieldOfViewChanged = onFieldOfViewChanged,
                        onHeadingChanged = onHeadingChanged,
                        onElevationSamplingIntervalChanged = onElevationSamplingIntervalChanged
                    )
                }
            )
        }
    )
}

/**
 * Display copyright text for the raster data we are using.
 */
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

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            uiState = ViewshedUiState(
                observerElevation = 20.0,
                targetHeight = 20.0,
                maxRadius = 8000.0,
                fieldOfView = 150.0,
                heading = 10.0,
                elevationSamplingInterval = 0.0
            ),
            mainPaneContent = {}
        )
    }
}
