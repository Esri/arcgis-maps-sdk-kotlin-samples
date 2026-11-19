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

package com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.screens

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
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.R
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.ControlMapAlgebraInputsWithFloatSuppliersViewModel
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.FloatSuppliersUiState
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
fun ControlMapAlgebraInputsWithFloatSuppliersScreen(
    viewModel: ControlMapAlgebraInputsWithFloatSuppliersViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    MainScreenScaffold(
        uiState = uiState,
        displayProgressIndicator = viewModel.displayProgressIndicator,
        onMinMaxSlopeChange = viewModel::onMinMaxSlopeChange,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                analysisOverlays = listOf(viewModel.analysisOverlay),
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
    uiState: FloatSuppliersUiState,
    displayProgressIndicator: Boolean,
    onMinMaxSlopeChange: (Float, Float) -> Unit = { _, _ ->},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.control_map_algebra_inputs_with_float_suppliers_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Settings",
                config = ThreePaneConfig(compactSupportingPaneHeightRatio = 0.3f),
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ControlMapAlgebraInputsWithFloatSuppliersSupportingPane(
                        uiState = uiState,
                        displayProgressIndicator = displayProgressIndicator,
                        onMinMaxSlopeChange = onMinMaxSlopeChange
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
            uiState = FloatSuppliersUiState.initialFloatSuppliersUiState,
            displayProgressIndicator = true,
            mainPaneContent = {}
        )
    }
}
