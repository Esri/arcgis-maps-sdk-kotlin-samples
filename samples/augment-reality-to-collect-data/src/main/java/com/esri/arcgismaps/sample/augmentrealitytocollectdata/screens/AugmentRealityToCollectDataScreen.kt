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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.ar.WorldScaleTrackingMode
import com.arcgismaps.toolkit.ar.rememberWorldScaleSceneViewStatus
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.BuildConfig
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.AugmentRealityToCollectDataViewModel
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.TreeHealth
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun AugmentRealityToCollectDataScreen(sampleName: String) {
    val augmentedRealityViewModel: AugmentRealityToCollectDataViewModel = viewModel()

    var initializationStatus by rememberWorldScaleSceneViewStatus()

    val context = LocalContext.current

    val hasNonDefaultAPIKey = BuildConfig.GOOGLE_API_KEY != "DEFAULT_GOOGLE_API_KEY"
    // Initialize the world scale tracking mode based on whether a google API key is provided
    val worldScaleTrackingMode = remember {
        when {
            hasNonDefaultAPIKey -> { WorldScaleTrackingMode.Geospatial() }
            else -> { WorldScaleTrackingMode.World() }
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!augmentedRealityViewModel.isDialogOptionsVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { augmentedRealityViewModel.showDialog(context) }
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
                    worldScaleTrackingMode = worldScaleTrackingMode,
                    worldScaleSceneViewProxy = augmentedRealityViewModel.worldScaleSceneViewProxy,
                    graphicsOverlays = listOf(augmentedRealityViewModel.graphicsOverlay),
                    onSingleTapConfirmed = augmentedRealityViewModel::addMarker,
                )
            }

            if (augmentedRealityViewModel.isDialogOptionsVisible) {
                TreeHealthDialog(
                    onOptionSelected = { selectedOption ->
                        augmentedRealityViewModel.addTree(context ,selectedOption)},
                    onDismissRequest = augmentedRealityViewModel::hideDialog
                )
            }

            when (val status = initializationStatus) {
                is WorldScaleSceneViewStatus.Initializing -> {
                    // Display a message indicating the initialization status
                    TextWithScrim(
                        if (worldScaleTrackingMode is WorldScaleTrackingMode.Geospatial) {
                            "Initializing AR in geospatial mode..."
                        } else {
                            "Initializing AR in world mode..."
                        }
                    )
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

                        is LoadStatus.Loaded -> {} // Display the main content of the AR scene once it has successfully loaded.
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

/**
 * Displays a dialog for selecting the health status of a tree.
 */
@Composable
fun TreeHealthDialog(
    onOptionSelected: (TreeHealth) -> Unit,
    onDismissRequest: () -> Unit
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Text("Add Tree ", style = MaterialTheme.typography.titleLarge)
        Text("How healthy is this tree?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        TreeHealth.entries.forEach { option ->
            Button(
                onClick = {
                    onOptionSelected(option)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismissRequest) { Text("Dismiss") }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun DialogOptionsPreview() {
    SamplePreviewSurface {
        TreeHealthDialog(
            onOptionSelected = { _ -> },
            onDismissRequest = {}
        )
    }
}
