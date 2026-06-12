/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.hydrography.EncAreaSymbolizationType
import com.arcgismaps.hydrography.EncColorScheme
import com.arcgismaps.hydrography.EncPointSymbolizationType
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.components.ConfigureElectronicNavigationalChartsScreenViewModel
import com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.components.areaSymbolizationTypes
import com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.components.colorSchemes
import com.esri.arcgismaps.sample.configureelectronicnavigationalcharts.components.pointSymbolizationTypes
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun ConfigureElectronicNavigationalChartsScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: ConfigureElectronicNavigationalChartsScreenViewModel = viewModel()
    val selectedEncFeature by mapViewModel.selectedEncFeature.collectAsStateWithLifecycle()
    var isSettingsDialogVisible by remember { mutableStateOf(false) }
    var tapLocation by remember { mutableStateOf<Point?>(null) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onSingleTapConfirmed = mapViewModel::identify
                ) {
                    selectedEncFeature?.let { encFeature ->
                        tapLocation?.let { location ->
                            Callout(location = location) {
                                Column {
                                    Text(encFeature.acronym)
                                    Text(encFeature.description)
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    modifier = Modifier.padding(12.dp),
                    onClick = { isSettingsDialogVisible = true }
                ) { Text("Display Settings") }

                DisplaySettingsContent(
                    isSettingsDialogVisible = isSettingsDialogVisible,
                    currentColorScheme = mapViewModel.currentColorScheme,
                    currentAreaSymbolizationType = mapViewModel.currentAreaSymbolizationType,
                    currentPointSymbolizationType = mapViewModel.currentPointSymbolizationType,
                    onColorSchemeSelected = mapViewModel::updateColorScheme,
                    onAreaSymbolizationSelected = mapViewModel::updateAreaSymbolizationType,
                    onPointSymbolizationSelected = mapViewModel::updatePointSymbolizationType,
                    onDismiss = { isSettingsDialogVisible = false }
                )
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}

@Composable
fun DisplaySettingsContent(
    isSettingsDialogVisible: Boolean,
    currentColorScheme: EncColorScheme,
    currentAreaSymbolizationType: EncAreaSymbolizationType,
    currentPointSymbolizationType: EncPointSymbolizationType,
    onColorSchemeSelected: (EncColorScheme) -> Unit,
    onAreaSymbolizationSelected: (EncAreaSymbolizationType) -> Unit,
    onPointSymbolizationSelected: (EncPointSymbolizationType) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(isSettingsDialogVisible) {
        SampleDialog(onDismissRequest = onDismiss) {
            Text("Display Settings", style = MaterialTheme.typography.titleMedium)
            DropDownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                textFieldLabel = "Color Scheme",
                textFieldValue = currentColorScheme.javaClass.simpleName,
                dropDownItemList = colorSchemes.map { it.javaClass.simpleName },
                onIndexSelected = { index -> onColorSchemeSelected(colorSchemes[index]) }
            )
            DropDownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                textFieldLabel = "Area Symbolization Type",
                textFieldValue = currentAreaSymbolizationType.javaClass.simpleName,
                dropDownItemList = areaSymbolizationTypes.map { it.javaClass.simpleName },
                onIndexSelected = { index -> onAreaSymbolizationSelected(areaSymbolizationTypes[index]) }
            )
            DropDownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                textFieldLabel = "Point Symbolization Type",
                textFieldValue = currentPointSymbolizationType.javaClass.simpleName,
                dropDownItemList = pointSymbolizationTypes.map { it.javaClass.simpleName },
                onIndexSelected = { index -> onPointSymbolizationSelected(pointSymbolizationTypes[index]) }
            )
        }
    }
}
