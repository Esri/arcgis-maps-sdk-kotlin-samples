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

package com.esri.arcgismaps.sample.showcontrastresponsivegeoview.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.R
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastAppearance
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastMode
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastUiState
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.DeviceContrastSettings
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.GeoViewType
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ShowContrastResponsiveGeoViewViewModel
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.rememberDeviceContrastSettings

/**
 * Main composable screen for the ShowContrastResponsiveGeoView sample.
 * It owns the ViewModel and passes stateless UI data into the scaffold.
 */
@Composable
fun ShowContrastResponsiveGeoViewScreen() {
    val viewModel: ShowContrastResponsiveGeoViewViewModel = viewModel()
    val contrastUiState by viewModel.contrastUiState.collectAsStateWithLifecycle()
    val deviceContrastSettings = rememberDeviceContrastSettings()
    val automaticAppearance = deviceContrastSettings.toAppearance()
    val effectiveAppearance = when (contrastUiState.contrastMode) {
        ContrastMode.Automatic -> automaticAppearance
        ContrastMode.Manual -> contrastUiState.contrastAppearance
    }

    LaunchedEffect(effectiveAppearance) {
        viewModel.syncGeoViewContrast(effectiveAppearance)
    }

    MainScreenScaffold(
        contrastUiState = contrastUiState,
        onContrastModeChanged = viewModel::updateContrastMode,
        onManualContrastChanged = viewModel::syncGeoViewContrast,
        onGeoViewTypeChanged = viewModel::updateGeoViewType,
        onReferenceLayerVisibilityChanged = viewModel::updateReferenceLayerVisibility,
        mainPaneContent = {
            when (contrastUiState.geoViewType) {
                GeoViewType.MapView ->
                    MapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(),
                        arcGISMap = viewModel.arcGISMap
                    )

                GeoViewType.SceneView -> SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    arcGISScene = viewModel.arcGISScene
                )
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
    contrastUiState: ContrastUiState,
    onContrastModeChanged: (ContrastMode) -> Unit = {},
    onManualContrastChanged: (ContrastAppearance) -> Unit = {},
    onGeoViewTypeChanged: (GeoViewType) -> Unit = {},
    onReferenceLayerVisibilityChanged: (Boolean) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_contrast_responsive_geoview_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Contrast options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ShowContrastResponsiveGeoViewSupportingPane(
                        contrastUiState = contrastUiState,
                        onContrastModeChanged = onContrastModeChanged,
                        onManualContrastChanged = onManualContrastChanged,
                        onGeoViewTypeChanged = onGeoViewTypeChanged,
                        onReferenceLayerVisibilityChanged = onReferenceLayerVisibilityChanged
                    )
                }
            )
        }
    )
}

/**
 * Maps the current device settings snapshot to one of the four contrast appearances.
 */
private fun DeviceContrastSettings.toAppearance(): ContrastAppearance {
    return when {
        isHighContrastEnabled && isDarkTheme -> ContrastAppearance.HighContrastDark
        isHighContrastEnabled -> ContrastAppearance.HighContrastLight
        isDarkTheme -> ContrastAppearance.Dark
        else -> ContrastAppearance.Light
    }
}

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            contrastUiState = ContrastUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
