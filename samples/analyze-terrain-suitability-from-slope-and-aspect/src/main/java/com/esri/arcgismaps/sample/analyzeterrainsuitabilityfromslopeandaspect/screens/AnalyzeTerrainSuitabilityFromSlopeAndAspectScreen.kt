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

package com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.mapping.view.BackgroundGrid
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.R
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components.SlopeAspectUiState
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components.ScenarioOption
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components.AnalyzeTerrainSuitabilityFromSlopeAndAspectViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.ThreePaneConfig

/**
 * Main composable screen for the sample.
 * It owns the ViewModel and hoists UI state for the scaffold and the MapView.
 */
@Composable
fun AnalyzeTerrainSuitabilityFromSlopeAndAspectScreen(
    viewModel: AnalyzeTerrainSuitabilityFromSlopeAndAspectViewModel = viewModel()
) {
    val adaptiveUiState = viewModel.adaptiveUiState.collectAsStateWithLifecycle().value

    MainScreenScaffold(
        slopeAspectUiState = adaptiveUiState,
        onSelectionChange = viewModel::updateScenarioOption,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                analysisOverlays = listOf(viewModel.analysisOverlay),
                backgroundGrid = BackgroundGrid(color = Color.lightGray),
                onAnalysisViewStatusChanged = viewModel::analysisViewStatusListener
            )
            if (viewModel.displayProgressIndicator) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    )

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

@Composable
private fun MainScreenScaffold(
    slopeAspectUiState: SlopeAspectUiState,
    onSelectionChange: (ScenarioOption) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.analyze_terrain_suitability_from_slope_and_aspect_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                config = ThreePaneConfig(compactSupportingPaneHeightRatio = 0.37f),
                supportingPaneTitle = "Analysis options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    AnalyzeTerrainSuitabilityFromSlopeAndAspectSupportingPane(
                        slopeAspectUiState = slopeAspectUiState,
                        onSelectionChange = onSelectionChange
                    )
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
            slopeAspectUiState = SlopeAspectUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
