/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.setreferencescale.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.setreferencescale.components.LayerToggleState
import com.esri.arcgismaps.sample.setreferencescale.components.SetReferenceScaleViewModel

/**
 * Main screen containing the MapView and bottom-sheet controls for reference scale and layers.
 */
@Composable
fun SetReferenceScaleScreen(sampleName: String) {
    val mapViewModel: SetReferenceScaleViewModel = viewModel()

    // UI state for the bottom sheet visibility and layers dialog.
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var isLayersDialogVisible by remember { mutableStateOf(false) }

    // Collect flows from the viewmodel.
    val selectedReferenceScale by mapViewModel.selectedReferenceScale.collectAsStateWithLifecycle()
    val mapScale by mapViewModel.mapScale.collectAsStateWithLifecycle()
    val layers by mapViewModel.layers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // MapView fills the screen. When map is interacted with, dismiss the bottom sheet.
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onMapScaleChanged = { mapViewModel.onMapScaleChanged(it) },
                    onVisibleAreaChanged = { isBottomSheetVisible = false }
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Map Settings",
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                SettingsControls(
                    referenceScaleOptions = mapViewModel.referenceScaleOptions,
                    selectedReferenceScale = selectedReferenceScale,
                    onReferenceScaleSelected = mapViewModel::onReferenceScaleSelected,
                    currentMapScale = mapScale,
                    onShowLayers = { isLayersDialogVisible = true },
                    onSetToReferenceScale = { mapViewModel.setMapScaleToReference() }
                )
            }

            // Layers dialog: show a togglable list of operational layers.
            if (isLayersDialogVisible) {
                SampleDialog(onDismissRequest = { isLayersDialogVisible = false }) {
                    LayersDialogContent(
                        layers = layers,
                        onLayerScaleSymbolToggled = mapViewModel::onLayerScaleSymbolToggled,
                        onDone = { isLayersDialogVisible = false }
                    )
                }
            }

            // Display message dialogs from the ViewModel.
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

/**
 * Encapsulated settings controls arranged so labels are on the left and controls on the right.
 */
@Composable
fun SettingsControls(
    referenceScaleOptions: List<Double>,
    selectedReferenceScale: Double,
    onReferenceScaleSelected: (Int) -> Unit,
    onShowLayers: () -> Unit,
    currentMapScale: Double,
    onSetToReferenceScale: () -> Unit
) {
    // A Left column of hints and right of controls
    Card(Modifier.padding(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Map's reference scale:")
                DropDownMenuBox(
                    modifier = Modifier.padding(start = 12.dp),
                    textFieldValue = "1:${selectedReferenceScale.toInt()}",
                    textFieldLabel = "Reference Scale",
                    dropDownItemList = referenceScaleOptions.map { "1:${it.toInt()}" },
                    onIndexSelected = onReferenceScaleSelected
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Toggle layer visibility:")
                Button(onClick = onShowLayers) {
                    Text(text = "Layers")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Map Scale: 1:${if (currentMapScale.isNaN()) "..." else currentMapScale.toInt()}")
                Button(
                    onClick = onSetToReferenceScale,
                    enabled = !currentMapScale.isNaN() && (currentMapScale != selectedReferenceScale)
                ) {
                    Text(text = "Set to reference scale")
                }
            }
        }
    }
}

/**
 * Dialog composable that displays a list of operational [layers] and a toggle for
 * each layer's scalesSymbols property.
 */
@Composable
fun LayersDialogContent(
    layers: List<LayerToggleState>,
    onLayerScaleSymbolToggled: (LayerToggleState, Boolean) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Layers Symbol Scaling",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        layers.forEach { layerState ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(layerState.name)
                Switch(
                    checked = layerState.scaleSymbols,
                    onCheckedChange = { isChecked -> onLayerScaleSymbolToggled(layerState, isChecked) }
                )
            }
        }
        OutlinedButton(onClick = onDone) { Text("Done") }
    }
}

// Previews
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewSettingsControls() {
    SamplePreviewSurface {
        SettingsControls(
            referenceScaleOptions = listOf(50_000.0),
            selectedReferenceScale = 250_000.0,
            onReferenceScaleSelected = {},
            onShowLayers = {},
            currentMapScale = 123_456.0,
            onSetToReferenceScale = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewLayersDialog() {
    SamplePreviewSurface {
        LayersDialogContent(
            layers = listOf(
                LayerToggleState(
                    name = "Land",
                    scaleSymbols = true
                ),
                LayerToggleState(
                    name = "Roads",
                    scaleSymbols = false
                )
            ), onLayerScaleSymbolToggled = { _, _ -> }, onDone = {})
    }
}
