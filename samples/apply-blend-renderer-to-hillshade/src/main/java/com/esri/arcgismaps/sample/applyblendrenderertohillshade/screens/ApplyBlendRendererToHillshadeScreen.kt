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

package com.esri.arcgismaps.sample.applyblendrenderertohillshade.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applyblendrenderertohillshade.components.ApplyBlendRendererToHillshadeViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app. The UI exposes a floating settings button
 * which opens a dialog where the user can adjust the altitude, azimuth, slope type
 * and color ramp preset. Changes are applied live to the ViewModel which updates
 * the renderer on the map.
 */
@Composable
fun ApplyBlendRendererToHillshadeScreen(sampleName: String) {
    val viewModel: ApplyBlendRendererToHillshadeViewModel = viewModel()

    // Dialog visibility state
    var isDialogOptionsVisible by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isDialogOptionsVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isDialogOptionsVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            // MapView shows the map from the ViewModel
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = viewModel.arcGISMap
            )

            // Settings dialog
            if (isDialogOptionsVisible) {
                DialogOptions(
                    altitude = viewModel.altitude,
                    onAltitudeChange = viewModel::updateAltitude,
                    azimuth = viewModel.azimuth,
                    onAzimuthChange = viewModel::updateAzimuth,
                    slopeTypeOptions = ApplyBlendRendererToHillshadeViewModel.slopeTypeOptions,
                    selectedSlopeType = viewModel.slopeType,
                    onSlopeTypeSelected = viewModel::updateSlopeType,
                    colorRampPresets = viewModel.colorRampPresets,
                    selectedColorRampIndex = viewModel.selectedColorRampPresetIndex,
                    onColorRampSelected = viewModel::updateColorRampPresetIndex,
                    onDismissRequest = { isDialogOptionsVisible = false }
                )
            }

            // Message dialog for errors surfaced by the ViewModel
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
    }
}

@Composable
private fun DialogOptions(
    altitude: Double,
    onAltitudeChange: (Double) -> Unit,
    azimuth: Double,
    onAzimuthChange: (Double) -> Unit,
    slopeTypeOptions: List<Pair<String, com.arcgismaps.raster.SlopeType?>>,
    selectedSlopeType: com.arcgismaps.raster.SlopeType?,
    onSlopeTypeSelected: (com.arcgismaps.raster.SlopeType?) -> Unit,
    colorRampPresets: List<String>,
    selectedColorRampIndex: Int,
    onColorRampSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Text("Renderer Settings", style = MaterialTheme.typography.titleMedium)

        // Slope type dropdown
        val slopeLabels = slopeTypeOptions.map { it.first }
        val selectedSlopeIndex = slopeTypeOptions.indexOfFirst { it.second == selectedSlopeType }.coerceAtLeast(0)
        DropDownMenuBox(
            textFieldValue = slopeLabels.getOrNull(selectedSlopeIndex) ?: slopeLabels[0],
            textFieldLabel = "Slope Type",
            dropDownItemList = slopeLabels,
            onIndexSelected = { index -> onSlopeTypeSelected(slopeTypeOptions[index].second) }
        )

        // Color ramp dropdown
        DropDownMenuBox(
            textFieldValue = colorRampPresets.getOrNull(selectedColorRampIndex) ?: colorRampPresets[0],
            textFieldLabel = "Color Ramp Preset",
            dropDownItemList = colorRampPresets,
            onIndexSelected = onColorRampSelected
        )

        // Altitude slider
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Altitude: ${altitude.toInt()}°")
            Slider(
                value = altitude.toFloat(),
                onValueChange = { onAltitudeChange(it.toDouble()) },
                valueRange = 0f..360f
            )
        }

        // Azimuth slider
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Azimuth: ${azimuth.toInt()}°")
            Slider(
                value = azimuth.toFloat(),
                onValueChange = { onAzimuthChange(it.toDouble()) },
                valueRange = 0f..360f
            )
        }

        // Dismiss button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDismissRequest, modifier = Modifier.wrapContentWidth()) {
                Text("Dismiss")
            }
            Button(onClick = onDismissRequest, modifier = Modifier.padding(start = 8.dp)) {
                Text("Done")
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewDialogOptions() {
    SamplePreviewSurface {
        DialogOptions(
            altitude = 45.0,
            onAltitudeChange = {},
            azimuth = 0.0,
            onAzimuthChange = {},
            slopeTypeOptions = ApplyBlendRendererToHillshadeViewModel.slopeTypeOptions,
            selectedSlopeType = com.arcgismaps.raster.SlopeType.Degree,
            onSlopeTypeSelected = {},
            colorRampPresets = listOf("None", "DEM Light", "Screen Display", "Elevation"),
            selectedColorRampIndex = 0,
            onColorRampSelected = {},
            onDismissRequest = {}
        )
    }
}
