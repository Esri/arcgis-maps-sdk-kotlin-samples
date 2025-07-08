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

package com.esri.arcgismaps.sample.augmentrealitytocollectdata.screens

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.ar.WorldScaleTrackingMode
import com.arcgismaps.toolkit.ar.rememberWorldScaleSceneViewStatus
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.AugmentRealityToCollectDataViewModel
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.TreeHealth
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlin.enums.EnumEntries

/**
 * Main screen layout for the sample app
 */
@Composable
fun AugmentRealityToCollectDataScreen(sampleName: String) {
    val augmentedRealityViewModel: AugmentRealityToCollectDataViewModel = viewModel()
    var isDialogOptionsVisible by remember { mutableStateOf(false) }

    var initializationStatus by rememberWorldScaleSceneViewStatus()

    val context = LocalContext.current

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isDialogOptionsVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isDialogOptionsVisible = true }
                ) { Icon(Icons.Filled.Add, contentDescription = "Add tree") }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                WorldScaleSceneView(
                    arcGISScene = augmentedRealityViewModel.arcGISScene,
                    modifier = Modifier.fillMaxSize(),
                    onInitializationStatusChanged = { status ->
                        initializationStatus = status
                    },
                    worldScaleTrackingMode = WorldScaleTrackingMode.Geospatial(),
                    worldScaleSceneViewProxy = augmentedRealityViewModel.worldScaleSceneViewProxy,
                    graphicsOverlays = listOf(augmentedRealityViewModel.graphicsOverlay),
                    onSingleTapConfirmed = augmentedRealityViewModel::addMarker,
                )
            }

            if (isDialogOptionsVisible) {
                DialogOptions(
                    options = TreeHealth.entries,
                    onOptionSelected = { selectedOption ->
                        augmentedRealityViewModel.addTree(context ,selectedOption)},
                    onDismissRequest = { isDialogOptionsVisible = false }
                )
            }

            when (val status = initializationStatus) {
                is WorldScaleSceneViewStatus.Initializing -> {
                    TextWithScrim("Initializing AR...")
                }

                is WorldScaleSceneViewStatus.Initialized -> {
                    val sceneLoadStatus =
                        augmentedRealityViewModel.arcGISScene.loadStatus.collectAsStateWithLifecycle().value
                    when (sceneLoadStatus) {
                        is LoadStatus.Loading, LoadStatus.NotLoaded -> {
                            // The scene may take a while to load, so show a progress indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is LoadStatus.FailedToLoad -> {
                            TextWithScrim("Failed to load world scale AR scene: " + sceneLoadStatus.error)
                        }

                        else -> {}
                    }
                }

                is WorldScaleSceneViewStatus.FailedToInitialize -> {
                    TextWithScrim(
                        text = "World scale AR failed to initialize: " + (status.error.message ?: status.error)
                    )
                }
            }

            augmentedRealityViewModel.messageDialogVM.apply {
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
 * Displays the provided [text] on top of a half-transparent gray background.
 */
@Composable
private fun TextWithScrim(text: String) {
    Column(
        modifier = Modifier
            .background(androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

@Composable
fun DialogOptions(
    options: EnumEntries<TreeHealth>,
    onOptionSelected: (TreeHealth) -> Unit,
    onDismissRequest: () -> Unit
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Text("Add Tree ", style = MaterialTheme.typography.titleMedium)
        Text("How healthy is this tree?", style = MaterialTheme.typography.titleSmall)
        options.forEach { option ->
            Button(
                onClick = {
                    onOptionSelected(option)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(option.name)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onDismissRequest) { Text("Dismiss") }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DialogOptionsPreview() {
    SamplePreviewSurface {
        DialogOptions(
            options = TreeHealth.entries,
            onOptionSelected = { _ -> },
            onDismissRequest = {}
        )
    }
}
