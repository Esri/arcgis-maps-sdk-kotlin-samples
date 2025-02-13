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

package com.esri.arcgismaps.sample.showscalebar.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.UnitSystem
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.scalebar.Scalebar
import com.arcgismaps.toolkit.scalebar.ScalebarStyle
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.showscalebar.components.ShowScaleBarViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Main screen layout for the sample app
 */
@Composable
fun ShowScaleBarScreen(sampleName: String) {
    val mapViewModel: ShowScaleBarViewModel = viewModel()
    // Keep track of the currently selected scalebar
    var currentScalebarStyle by remember { mutableStateOf(ScalebarStyle.AlternatingBar) }
    var isScalebarDialogOptionsVisible by remember { mutableStateOf(false) }
    var currentAutoHideDelay by remember { mutableStateOf(Duration.ZERO) }
    var isGeodeticCalculationsEnabled by remember { mutableStateOf(true) }
    var currentScalebarUnitSystem by remember { mutableStateOf<UnitSystem>(UnitSystem.Metric) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isScalebarDialogOptionsVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isScalebarDialogOptionsVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show Scalebar Options") }
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    isAttributionBarVisible = false,
                    onSpatialReferenceChanged = mapViewModel::updateSpacialReference,
                    onUnitsPerDipChanged = mapViewModel::updateUnitsPerDip,
                    onViewpointChangedForCenterAndScale = mapViewModel::updateViewpoint
                )
                Scalebar(
                    modifier = Modifier
                        .padding(25.dp)
                        .align(Alignment.BottomStart),
                    maxWidth = 300.dp,
                    unitsPerDip = mapViewModel.unitsPerDip,
                    viewpoint = mapViewModel.viewpoint,
                    spatialReference = mapViewModel.spatialReference,
                    style = currentScalebarStyle,
                    units = currentScalebarUnitSystem,
                    autoHideDelay = currentAutoHideDelay,
                    useGeodeticCalculations = isGeodeticCalculationsEnabled
                )

                if (isScalebarDialogOptionsVisible) {
                    ScalebarDialogOptions(
                        currentScalebarStyle = currentScalebarStyle,
                        currentScalebarUnitSystem = currentScalebarUnitSystem,
                        currentAutoHideDelay = currentAutoHideDelay,
                        isGeodeticCalculationsEnabled = isGeodeticCalculationsEnabled,
                        onScalebarUnitSystemSelected = { currentScalebarUnitSystem = it },
                        onScalebarStyleSelected = { currentScalebarStyle = it },
                        onDismissRequest = { isScalebarDialogOptionsVisible = false },
                        onAutoHideDelaySelected = { currentAutoHideDelay = it },
                        onGeodeticCalculationsToggled = {
                            isGeodeticCalculationsEnabled = !isGeodeticCalculationsEnabled
                        }
                    )
                }
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
fun ScalebarDialogOptions(
    currentScalebarStyle: ScalebarStyle,
    currentScalebarUnitSystem: UnitSystem,
    currentAutoHideDelay: Duration,
    onDismissRequest: () -> Unit,
    isGeodeticCalculationsEnabled: Boolean,
    onScalebarStyleSelected: (ScalebarStyle) -> Unit,
    onScalebarUnitSystemSelected: (UnitSystem) -> Unit,
    onAutoHideDelaySelected: (Duration) -> Unit,
    onGeodeticCalculationsToggled: (Boolean) -> Unit
) {
    // List of all the supported scale bar properties
    val scalebarStyles = ScalebarStyle.entries.toList()
    val scalebarUnitSystems = listOf(UnitSystem.Metric, UnitSystem.Imperial)
    val autoHideDelays = listOf(
        Duration.ZERO,
        1.toDuration(DurationUnit.SECONDS),
        3.toDuration(DurationUnit.SECONDS),
        5.toDuration(DurationUnit.SECONDS)
    )

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Scale bar options: ", style = MaterialTheme.typography.headlineSmall)
            DropDownMenuBox(
                textFieldLabel = "Select scale bar style: ",
                textFieldValue = currentScalebarStyle.name,
                dropDownItemList = scalebarStyles.map { it.name },
                onIndexSelected = { index -> onScalebarStyleSelected(scalebarStyles[index]) }
            )
            DropDownMenuBox(
                textFieldLabel = "Select scale bar unit system: ",
                textFieldValue = currentScalebarUnitSystem.javaClass.simpleName,
                dropDownItemList = scalebarUnitSystems.map { it.javaClass.simpleName },
                onIndexSelected = { index -> onScalebarUnitSystemSelected(scalebarUnitSystems[index]) }
            )
            DropDownMenuBox(
                textFieldLabel = "Select scale bar auto-hide delay: ",
                textFieldValue = currentAutoHideDelay.inWholeSeconds.toString(),
                dropDownItemList = autoHideDelays.map { it.inWholeSeconds.toString() },
                onIndexSelected = { index -> onAutoHideDelaySelected(autoHideDelays[index]) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Geodetic Calculations",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = isGeodeticCalculationsEnabled,
                    onCheckedChange = onGeodeticCalculationsToggled
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewScalebarDialogOptions() {
    SampleAppTheme {
        Surface {
            ScalebarDialogOptions(
                currentScalebarStyle = ScalebarStyle.AlternatingBar,
                currentScalebarUnitSystem = UnitSystem.Metric,
                currentAutoHideDelay = Duration.ZERO,
                isGeodeticCalculationsEnabled = true,
                onDismissRequest = { },
                onScalebarStyleSelected = { },
                onScalebarUnitSystemSelected = { },
                onAutoHideDelaySelected = { },
                onGeodeticCalculationsToggled = { }
            )
        }
    }
}
