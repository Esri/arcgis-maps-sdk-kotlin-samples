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

package com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.R
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.ClipAFieldFunctionToMapViewExtentViewModel
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.ClipFunctionUiState
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
fun ClipAFieldFunctionToMapViewExtentScreen(
    viewModel: ClipAFieldFunctionToMapViewExtentViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    MainScreenScaffold(
        uiState = uiState,
        displayProgressIndicator = viewModel.displayProgressIndicator,
        onMinMaxElevationChange = viewModel::onMinMaxElevationChange,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                analysisOverlays = listOf(viewModel.analysisOverlay),
                graphicsOverlays = listOf(viewModel.graphicsOverlay),
                onVisibleAreaChanged = viewModel::onVisibleAreaChanged,
                onAnalysisViewStatusChanged = viewModel::analysisViewStatusListener
            )
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
    uiState: ClipFunctionUiState,
    displayProgressIndicator: Boolean,
    onMinMaxElevationChange: (Float, Float) -> Unit = {_, _ ->},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.clip_a_field_function_to_map_view_extent_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Settings",
                config = ThreePaneConfig(compactSupportingPaneHeightRatio = 0.31f),
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ClipAFieldFunctionToMapViewExtentSupportingPane(
                        uiState = uiState,
                        displayProgressIndicator = displayProgressIndicator,
                        onMinMaxElevationChange = onMinMaxElevationChange
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
            uiState = ClipFunctionUiState.initialClipFunctionUiState,
            displayProgressIndicator = true,
            mainPaneContent = {}
        )
    }
}
