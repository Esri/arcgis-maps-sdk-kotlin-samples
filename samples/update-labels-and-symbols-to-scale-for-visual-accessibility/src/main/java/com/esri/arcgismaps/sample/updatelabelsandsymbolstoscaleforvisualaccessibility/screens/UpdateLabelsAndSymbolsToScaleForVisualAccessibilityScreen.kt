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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.MapView
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.R
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components.AdaptiveUiState
import com.esri.arcgismaps.sample.updatelabelsandsymbolstoscaleforvisualaccessibility.components.UpdateLabelsAndSymbolsToScaleForVisualAccessibilityViewModel
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
fun UpdateLabelsAndSymbolsToScaleForVisualAccessibilityScreen(
    viewModel: UpdateLabelsAndSymbolsToScaleForVisualAccessibilityViewModel = viewModel()
) {
    val adaptiveUiState = viewModel.adaptiveUiState.collectAsStateWithLifecycle().value
    val fontScale = LocalConfiguration.current.fontScale
    val context = LocalContext.current

    LaunchedEffect(fontScale) {
        viewModel.updateFontScale(fontScale)
    }

    MainScreenScaffold(
        adaptiveUiState = adaptiveUiState,
        onCheckedChange = viewModel::onSelectSystemTextSize,
        onOpenTextSettings = {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))
        },
        mainPaneContent = {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val mapView = remember(context) { MapView(context) }
            val restaurantViewpoint = viewModel.restaurantViewpoint.collectAsStateWithLifecycle().value

            DisposableEffect(lifecycleOwner, mapView) {
                lifecycleOwner.lifecycle.addObserver(mapView)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(mapView)
                    mapView.onDestroy(lifecycleOwner)
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                factory = { mapView },
                update = {
                    it.map = viewModel.arcGISMap
                    it.useSystemTextScale = adaptiveUiState.isSystemTextScaleEnabled
                }
            )

            LaunchedEffect(restaurantViewpoint) {
                restaurantViewpoint?.let { mapView.setViewpoint(it) }
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
