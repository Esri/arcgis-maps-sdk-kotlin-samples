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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.AdaptiveThreePane
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.ShowLineOfSightAnalysisInMapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.R
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.LineOfSightUiState

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowLineOfSightAnalysisInMapScreen() {
    val viewModel: ShowLineOfSightAnalysisInMapViewModel = viewModel()
    val uiState by viewModel.lineOfSightUiState.collectAsStateWithLifecycle()

    MainScreenScaffold(
        uiState = uiState,
        onVisibilityFilterChanged = viewModel::setVisibilityFilter,
        mainPaneContent = {
            MapView(
                modifier = Modifier.fillMaxSize(),
                arcGISMap = viewModel.arcGISMap
            )
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
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                supportingPaneTitle = "Line of Sight Options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { isFloatingPaneVisible, toggleFloatingPane ->
                    LineOfSightSupportingContent(uiState, onVisibilityFilterChanged)
                }
            )
        }
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
