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

package com.esri.arcgismaps.sample.applyrgbrenderer.screens

import androidx.compose.animation.animateContentSize
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
import com.esri.arcgismaps.sample.applyrgbrenderer.R
import com.esri.arcgismaps.sample.applyrgbrenderer.components.RgbRendererUiState
import com.esri.arcgismaps.sample.applyrgbrenderer.components.ApplyRgbRendererViewModel
import com.esri.arcgismaps.sample.applyrgbrenderer.components.StretchType
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane

/**
 * Main composable screen for the sample.
 * It owns the ViewModel and hoists UI state for the scaffold and the MapView.
 */
@Composable
fun ApplyRgbRendererScreen(
    viewModel: ApplyRgbRendererViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenScaffold(
        uiState = uiState,
        onStretchTypeChange = viewModel::updateStretchType,
        onMinMaxMinValueChange = viewModel::onMinMaxMinValueChange,
        onMinMaxMaxValueChange = viewModel::onMinMaxMaxValueChange,
        onPercentClipMinValueChange = viewModel::onPercentClipMinValueChange,
        onPercentClipMaxValueChange = viewModel::onPercentClipMaxValueChange,
        onStdDevFactorChange = viewModel::onStdDevFactorChange,
        onResetAllChanges = viewModel::resetAllChanges,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy
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
    uiState: RgbRendererUiState,
    onStretchTypeChange: (StretchType) -> Unit = {},
    onMinMaxMinValueChange: (Double) -> Unit = {},
    onMinMaxMaxValueChange: (Double) -> Unit = {},
    onPercentClipMinValueChange: (Double) -> Unit = {},
    onPercentClipMaxValueChange: (Double) -> Unit = {},
    onStdDevFactorChange: (Double) -> Unit = {},
    onResetAllChanges: () -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.apply_rgb_renderer_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "RGB Renderer Settings",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ApplyRgbRendererSupportingPane(
                        uiState = uiState,
                        onStretchTypeChange = onStretchTypeChange,
                        onMinMaxMinValueChange = onMinMaxMinValueChange,
                        onMinMaxMaxValueChange = onMinMaxMaxValueChange,
                        onPercentClipMinValueChange = onPercentClipMinValueChange,
                        onPercentClipMaxValueChange = onPercentClipMaxValueChange,
                        onStdDevFactorChange = onStdDevFactorChange,
                        onResetAllChanges = onResetAllChanges
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
            uiState = RgbRendererUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
