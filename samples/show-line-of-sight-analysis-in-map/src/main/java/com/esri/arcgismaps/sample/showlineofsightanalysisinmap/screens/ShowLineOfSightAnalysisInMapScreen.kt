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

package com.esri.arcgismaps.sample.showlineofsightanalysisinmap.screens

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import com.esri.arcgismaps.sample.sampleslib.components.AdaptiveThreePane
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.ShowLineOfSightAnalysisInMapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.ThreePaneConfig
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.R
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.LineOfSightUiState

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowLineOfSightAnalysisInMapScreen() {
    val viewModel: ShowLineOfSightAnalysisInMapViewModel = viewModel()
    val uiState by viewModel.lineOfSightUiState.collectAsStateWithLifecycle()

    // Create a MapViewProxy, used fro identifyGraphicsOverlays and to convert screen points to map points
    val mapViewProxy = MapViewProxy()

    MainScreenScaffold(
        uiState = uiState,
        onVisibilityFilterChanged = viewModel::setVisibilityFilter,
        mainPaneContent = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                RasterDataCopyrightText()
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = viewModel.arcGISMap,
                    mapViewProxy = mapViewProxy,
                    graphicsOverlays = listOf(
                        viewModel.resultsGraphicsOverlay,
                        viewModel.observersGraphicsOverlay,
                        viewModel.targetGraphicsOverlay
                    ),
                    onSingleTapConfirmed = { event ->
                        viewModel.onTap(event, mapViewProxy)
                    },
                    content = {
                        // Show a callout only when an observer has been selected
                        viewModel.selectedObserverGraphic?.let { graphic ->
                            Callout(
                                geoElement = graphic,
                                modifier = Modifier.sizeIn(maxWidth = 250.dp),
                                shapes = CalloutDefaults.shapes(
                                    calloutContentPadding = PaddingValues(4.dp)
                                ),
                                colorScheme = CalloutDefaults.colors(
                                    backgroundColor = MaterialTheme.colorScheme.background,
                                    borderColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                // Callout content:
                                Column {
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        text = viewModel.calloutContentTitle,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    viewModel.calloutContentDetail?.let { string ->
                                        Text(
                                            text = string,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun MainScreenScaffold(
    uiState: LineOfSightUiState,
    onVisibilityFilterChanged: (Boolean) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_line_of_sight_analysis_in_map_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                Modifier.fillMaxSize().padding(paddingValues),
                supportingPaneTitle = "Line of Sight Options",
                config = ThreePaneConfig(compactSupportingPaneHeightRatio = 0.25f),
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    LineOfSightSupportingContent(uiState, onVisibilityFilterChanged)
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
            uiState = LineOfSightUiState(
                visibilityFilter = true
            ),
            mainPaneContent = {} // empty placeholder — no ArcGIS objects needed
        )
    }
}
