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

package com.esri.arcgismaps.sample.applyscenepropertyexpressions.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.applyscenepropertyexpressions.components.ApplyScenePropertyExpressionsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyScenePropertyExpressionsScreen(sampleName: String) {
    val sceneViewModel: ApplyScenePropertyExpressionsViewModel = viewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = sceneViewModel.arcGISScene,
                    graphicsOverlays = listOf(sceneViewModel.graphicsOverlay),
                )
                // Settings bottom sheet
                if (isSheetOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { isSheetOpen = false },
                        sheetState = sheetState
                    ) {
                        ScenePropertyExpressionsSettings(
                            heading = sceneViewModel.heading,
                            pitch = sceneViewModel.pitch,
                            onHeadingChanged = sceneViewModel::updateHeading,
                            onPitchChanged = sceneViewModel::updatePitch,
                            onDone = { isSheetOpen = false }
                        )
                    }
                }
            }
            // Error dialog
            sceneViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSheetOpen) {
                // Floating action button to open settings
                FloatingActionButton(
                    modifier = Modifier.padding(16.dp),
                    onClick = { isSheetOpen = true }
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    )
}

@Composable
fun ScenePropertyExpressionsSettings(
    heading: Double,
    pitch: Double,
    onHeadingChanged: (Double) -> Unit,
    onPitchChanged: (Double) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Expression Settings", style = MaterialTheme.typography.titleLarge)
        // Heading slider
        Text("Heading: ${heading.toInt()}°", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = heading.toFloat(),
            onValueChange = { onHeadingChanged(it.toDouble()) },
            valueRange = 0f..360f,
        )
        // Pitch slider
        Text("Pitch: ${pitch.toInt()}°", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = pitch.toFloat(),
            onValueChange = { onPitchChanged(it.toDouble()) },
            valueRange = 0f..180f,
        )
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onDone) { Text("Done") }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewScenePropertyExpressionsSettings() {
    SamplePreviewSurface {
        ScenePropertyExpressionsSettings(
            heading = 180.0,
            pitch = 45.0,
            onHeadingChanged = {},
            onPitchChanged = {},
            onDone = {}
        )
    }
}
