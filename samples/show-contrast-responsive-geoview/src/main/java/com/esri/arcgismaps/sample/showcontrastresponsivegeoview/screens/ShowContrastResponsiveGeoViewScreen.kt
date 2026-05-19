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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.R
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ContrastUiState
import com.esri.arcgismaps.sample.showcontrastresponsivegeoview.components.ShowContrastResponsiveGeoViewViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowContrastResponsiveGeoViewScreen() {
    val mapViewModel: ShowContrastResponsiveGeoViewViewModel = viewModel()
    val contrastUiState by mapViewModel.contrastUiState.collectAsStateWithLifecycle()

    MainScreenScaffold(
        contrastUiState = contrastUiState,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = mapViewModel.arcGISMap
            )
        })
}

@Composable
private fun MainScreenScaffold(
    contrastUiState: ContrastUiState,
    mainPaneContent: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.show_contrast_responsive_geoview_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Viewshed Options",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    ContrastSupportingContent(contrastUiState = contrastUiState)
                }
            )
        }
    )
}

@Composable
private fun ContrastSupportingContent(contrastUiState: ContrastUiState) {
    Column {
        // display contrast options
        DropDownMenuBox(
            textFieldValue = if (contrastUiState.isResponsiveAutomatic) "Automatic" else "Manual",
            textFieldLabel = "Contrast mode",
            dropDownItemList = listOf("Automatic", "Manual"),
            onIndexSelected = { }
        )

        // if manual contrast is selected, show the manual contrast value
        if (!contrastUiState.isResponsiveAutomatic) {
            Text(text = "Manual contrast value: ${contrastUiState.manualContrastValue}")
        }
    }
}

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            contrastUiState = ContrastUiState(
                isResponsiveAutomatic = false,
                manualContrastValue = 1f
            ),
            mainPaneContent = {},
        )
    }
}
