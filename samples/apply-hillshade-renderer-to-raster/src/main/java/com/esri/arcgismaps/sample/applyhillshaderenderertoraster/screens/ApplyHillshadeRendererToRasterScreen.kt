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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.applyhillshaderenderertoraster.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.raster.SlopeType
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applyhillshaderenderertoraster.components.ApplyHillshadeRendererToRasterViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlin.math.roundToInt

val slopeTypes = listOf(
    SlopeType.None, SlopeType.Degree, SlopeType.PercentRise, SlopeType.Scaled
)

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyHillshadeRendererToRasterScreen(sampleName: String) {
    val mapViewModel: ApplyHillshadeRendererToRasterViewModel = viewModel()

    // Set up the bottom sheet controls
    val controlsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isBottomSheetVisible) {
        if (isBottomSheetVisible) controlsBottomSheetState.show()
        else controlsBottomSheetState.hide()
    }

    // Collect latest UI states
    val altitude by remember { mapViewModel::currentAltitude }
    val azimuth by remember { mapViewModel::currentAzimuth }
    val slopeType by remember { mapViewModel::currentSlopeType }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    arcGISMap = mapViewModel.arcGISMap,
                    modifier = Modifier.fillMaxSize()
                )
                if (isBottomSheetVisible) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = controlsBottomSheetState,
                        onDismissRequest = { isBottomSheetVisible = false }
                    ) {
                        HillshadeRendererOptions(
                            currentAltitude = altitude,
                            currentAzimuth = azimuth,
                            currentSlopeType = slopeType,
                            updateAltitude = mapViewModel::updateAltitude,
                            updateAzimuth = mapViewModel::updateAzimuth,
                            updateSlopeType = mapViewModel::updateSlopeType
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Hillshade renderer options") }
            }
        }
    )
}

@Composable
fun HillshadeRendererOptions(
    currentAltitude: Double,
    currentAzimuth: Double,
    currentSlopeType: SlopeType,
    updateAltitude: (Double) -> Unit,
    updateAzimuth: (Double) -> Unit,
    updateSlopeType: (SlopeType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = "Hillshade Renderer Settings",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Altitude:", style = MaterialTheme.typography.labelLarge)
            Text(text = currentAltitude.toString(), style = MaterialTheme.typography.labelLarge)
        }
        // Altitude Slider
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            value = currentAltitude.toFloat(),
            onValueChange = { updateAltitude(it.roundToInt().toDouble()) },
            valueRange = 0f..90f
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Azimuth:", style = MaterialTheme.typography.labelLarge)
            Text(text = currentAzimuth.toString(), style = MaterialTheme.typography.labelLarge)
        }
        // Azimuth Slider
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            value = currentAzimuth.toFloat(),
            onValueChange = { updateAzimuth(it.roundToInt().toDouble()) },
            valueRange = 0f..360f
        )
        // SlopeType dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    label = { Text("SlopeType") },
                    value = currentSlopeType.javaClass.simpleName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    slopeTypes.forEachIndexed { index, slopeType ->
                        DropdownMenuItem(
                            text = { Text(slopeType.javaClass.simpleName) },
                            onClick = {
                                updateSlopeType(slopeType)
                                expanded = false
                            })
                        // Show a divider between dropdown menu options
                        if (index < slopeTypes.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HillshadeRendererOptionsPreview() {
    SampleAppTheme {
        Surface {
            HillshadeRendererOptions(
                currentAltitude = 45.0,
                currentAzimuth = 0.0,
                currentSlopeType = SlopeType.None,
                updateAltitude = { },
                updateAzimuth = { },
                updateSlopeType = { }
            )
        }
    }
}
