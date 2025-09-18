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

package com.esri.arcgismaps.sample.applyterrainexaggeration.screens

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.applyterrainexaggeration.components.ApplyTerrainExaggerationViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Main screen that displays a SceneView and a bottom sheet with controls
 * to adjust terrain vertical exaggeration.
 */
@Composable
fun ApplyTerrainExaggerationScreen(sampleName: String) {
    val sceneViewModel: ApplyTerrainExaggerationViewModel = viewModel()

    // Observe the exaggeration value from the ViewModel
    val exaggeration by sceneViewModel.elevationExaggeration.collectAsStateWithLifecycle()

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Show options")
                }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISScene = sceneViewModel.arcGISScene,
                    onViewpointChangedForBoundingGeometry = { isBottomSheetVisible = false }
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Terrain options",
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                TerrainOptions(
                    currentExaggeration = exaggeration,
                    onIncrement = { sceneViewModel.updateElevationExaggerationByFactor(1f) },
                    onDecrement = { sceneViewModel.updateElevationExaggerationByFactor(-1f) }
                )
            }
            // Display error dialogs from the ViewModel
            sceneViewModel.messageDialogVM.apply {
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
fun TerrainOptions(
    currentExaggeration: Float,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = "Elevation exaggeration: ${currentExaggeration.toInt()}x")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onDecrement() }, enabled = currentExaggeration > 1f) {
                Text(text = "-")
            }

            Text(text = "${currentExaggeration.toInt()}x")

            Button(onClick = { onIncrement() }, enabled = currentExaggeration < 10f) {
                Text(text = "+")
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewTerrainOptions() {
    SampleAppTheme {
        Surface {
            TerrainOptions(
                currentExaggeration = 2f,
                onIncrement = {},
                onDecrement = {}
            )
        }
    }
}
