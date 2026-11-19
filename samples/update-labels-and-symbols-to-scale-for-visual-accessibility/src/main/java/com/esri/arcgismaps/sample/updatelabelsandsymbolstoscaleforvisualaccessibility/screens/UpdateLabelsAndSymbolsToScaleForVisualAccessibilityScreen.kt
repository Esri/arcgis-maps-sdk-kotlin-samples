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

package com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.R
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components.AdaptiveUiState
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components.UpdateLabelsAndSymbolsToScaleForVisualAccessibilityViewModel
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
fun UpdateLabelsAndSymbolsToScaleForVisualAccessibilityScreen(
    viewModel: UpdateLabelsAndSymbolsToScaleForVisualAccessibilityViewModel = viewModel()
) {
    val adaptiveUiState = viewModel.adaptiveUiState.collectAsStateWithLifecycle().value
    val fontScale = LocalConfiguration.current.fontScale
    val context = LocalContext.current

    LaunchedEffect(fontScale) {
        viewModel.updateFontScale(fontScale)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onSelectSystemTextSize(true)
    }
    MainScreenScaffold(
        adaptiveUiState = adaptiveUiState,
        onCheckedChange = viewModel::onSelectSystemTextSize,
        onOpenTextSettings = {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        },
        mainPaneContent = {
            MapView(
                arcGISMap = viewModel.arcGISMap,
                useSystemTextScale = adaptiveUiState.isSystemTextScaleEnabled,
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
    adaptiveUiState: AdaptiveUiState,
    onCheckedChange: (Boolean) -> Unit = {},
    onOpenTextSettings: () -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.update_labels_and_symbols_to_scale_for_visual_accessibility_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                config = ThreePaneConfig(compactSupportingPaneHeightRatio = 0.4f),
                supportingPaneTitle = "Change the OS text size to scale feature labels and symbols",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    UpdateLabelsAndSymbolsToScaleForVisualAccessibilitySupportingPane(
                        adaptiveUiState = adaptiveUiState,
                        onCheckedChange = onCheckedChange,
                        onOpenTextSettings = onOpenTextSettings
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
            adaptiveUiState = AdaptiveUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
