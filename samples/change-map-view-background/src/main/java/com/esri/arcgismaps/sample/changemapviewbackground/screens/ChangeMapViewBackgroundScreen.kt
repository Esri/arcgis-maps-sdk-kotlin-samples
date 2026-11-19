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

package com.esri.arcgismaps.sample.changemapviewbackground.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.changemapviewbackground.R
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundUiState
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundViewModel
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
fun ChangeMapViewBackgroundScreen(
    viewModel: ChangeMapViewBackgroundViewModel = viewModel()
) {
    val changeMapViewBackgroundUiState =
        viewModel.changeMapViewBackgroundUiState.collectAsStateWithLifecycle().value

    MainScreenScaffold(
        adaptiveUiState = changeMapViewBackgroundUiState,
        onColorChange = viewModel::updateColor,
        onLineColorChange = viewModel::updateLineColor,
        onLineWidthChange = viewModel::updateLineWidth,
        onSizeChange = viewModel::updateSize,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                backgroundGrid = viewModel.backgroundGrid
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
    adaptiveUiState: ChangeMapViewBackgroundUiState,
    onColorChange: (Color) -> Unit = {},
    onLineColorChange: (Color) -> Unit = {},
    onLineWidthChange: (Float) -> Unit = {},
    onSizeChange: (Float) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.change_map_view_background_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Background Settings",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ChangeMapViewBackgroundSupportingPane(
                        adaptiveUiState = adaptiveUiState,
                        onColorChange = onColorChange,
                        onLineColorChange = onLineColorChange,
                        onLineWidthChange = onLineWidthChange,
                        onSizeChange = onSizeChange
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
            adaptiveUiState = ChangeMapViewBackgroundUiState.defaultState,
            mainPaneContent = {}
        )
    }
}
