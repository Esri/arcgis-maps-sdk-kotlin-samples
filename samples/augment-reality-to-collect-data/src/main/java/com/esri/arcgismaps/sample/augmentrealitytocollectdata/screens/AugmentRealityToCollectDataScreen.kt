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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewStatus
import com.arcgismaps.toolkit.ar.WorldScaleTrackingMode
import com.arcgismaps.toolkit.ar.rememberWorldScaleSceneViewStatus
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.BuildConfig
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.R
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.AugmentRealityToCollectDataViewModel
import com.esri.arcgismaps.sample.augmentrealitytocollectdata.components.TreeHealth
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

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

    var displayCalibrationView by remember { mutableStateOf(false) }

    val sharedPreferences = LocalContext.current.getSharedPreferences("", Context.MODE_PRIVATE)
    var acceptedPrivacyInfo by rememberSaveable {
        mutableStateOf(
            sharedPreferences.getBoolean(
                KEY_PREF_ACCEPTED_PRIVACY_INFO,
                false
            )
        )
    }
    var showPrivacyInfo by rememberSaveable { mutableStateOf(!acceptedPrivacyInfo) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            Column {
                if (!augmentedRealityViewModel.isDialogOptionsVisible) {
                    FloatingActionButton(
                        modifier = Modifier.padding(bottom = 20.dp, end = 12.dp),
                        onClick = { augmentedRealityViewModel.showDialog(context) }
                    ) { Icon(Icons.Filled.Add, contentDescription = "Add tree") }
                }
                if (worldScaleTrackingMode is WorldScaleTrackingMode.World) {
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 20.dp, end = 12.dp),
                        onClick = { displayCalibrationView = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_straighten_24), "Show calibration view"
                        )
                    }
                }
            }
        },
        content = {
            if (showPrivacyInfo) {
                PrivacyInfoDialog(
                    hasCurrentlyAccepted = acceptedPrivacyInfo,
                    onUserResponse = { accepted ->
                        acceptedPrivacyInfo = accepted
                        sharedPreferences.edit { putBoolean(KEY_PREF_ACCEPTED_PRIVACY_INFO, accepted) }
                        showPrivacyInfo = false
                    }
                )
            }
            if (!acceptedPrivacyInfo) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Privacy Info not accepted")
                    Button(onClick = { showPrivacyInfo = true }) {
                        Text(text = "Show Privacy Info")
                    }
                }
            } else {
                Box(
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
                        onCurrentViewpointCameraChanged = { camera ->
                            augmentedRealityViewModel.onCurrentViewpointCameraChanged(camera.location)
                        },
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (worldScaleTrackingMode is WorldScaleTrackingMode.World) {
                                if (displayCalibrationView) {
                                    CalibrationView(
                                        onDismiss = { displayCalibrationView = false },
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                    )
                                }
                            }
                        }
                    }

                    if (augmentedRealityViewModel.isDialogOptionsVisible) {
                        TreeHealthDialog(
                            onOptionSelected = { selectedOption ->
                                augmentedRealityViewModel.addTree(context ,selectedOption)},
                            onDismissRequest = augmentedRealityViewModel::hideDialog
                        )
                    }

                    if (worldScaleTrackingMode is WorldScaleTrackingMode.Geospatial) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.5f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (augmentedRealityViewModel.isVpsAvailable) {
                                    "VPS available"
                                } else {
                                    "VPS unavailable"
                                },
                                color = Color.White
                            )
                        }
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

/**
 * An alert dialog that asks the user to accept or deny
 * [ARCore's privacy requirements](https://developers.google.com/ar/develop/privacy-requirements).
 */
@Composable
private fun PrivacyInfoDialog(
    hasCurrentlyAccepted: Boolean,
    onUserResponse: (accepted: Boolean) -> Unit
) {
    Dialog(onDismissRequest = {
        onUserResponse(hasCurrentlyAccepted)
    }) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                LegalTextArCore()
                Spacer(Modifier.height(16.dp))
                LegalTextGeospatial()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        onUserResponse(false)
                    }) {
                        Text(text = "Decline")
                    }

                    TextButton(onClick = {
                        onUserResponse(true)
                    }) {
                        Text(text = "Accept")
                    }
                }
            }
        }
    }
}

/**
 * Displays the required privacy information for use of ARCore
 */
@Composable
private fun LegalTextArCore() {
    val textLinkStyle =
        TextLinkStyles(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue))
    Text(text = buildAnnotatedString {
        append("This application runs on ")
        withLink(
            LinkAnnotation.Url(
                "https://play.google.com/store/apps/details?id=com.google.ar.core",
                textLinkStyle
            )
        ) {
            append("Google Play Services for AR")
        }
        append("  (ARCore), which is provided by Google and governed by the ")
        withLink(
            LinkAnnotation.Url(
                "https://policies.google.com/privacy",
                textLinkStyle
            )
        ) {
            append("Google Privacy Policy.")
        }
    })
}

/**
 * Displays the required privacy information for use of the Geospatial API
 */
@Composable
private fun LegalTextGeospatial() {
    Text(text = buildAnnotatedString {
        append("To power this session, Google will process sensor data (e.g., camera and location).")
        appendLine()
        withLink(
            LinkAnnotation.Url(
                "https://support.google.com/ar?p=how-google-play-services-for-ar-handles-your-data",
                TextLinkStyles(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Blue))
            )
        ) {
            append("Learn more")
        }
    })
}
