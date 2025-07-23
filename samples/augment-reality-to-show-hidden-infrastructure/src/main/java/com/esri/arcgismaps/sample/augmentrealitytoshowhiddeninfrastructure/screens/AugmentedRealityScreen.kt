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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.screens

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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.R
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components.AugmentedRealityViewModel
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components.SharedRepository
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

@Composable
fun AugmentedRealityScreen(sampleName: String) {
    val augmentedRealityViewModel: AugmentedRealityViewModel = viewModel()

    var displayCalibrationView by remember { mutableStateOf(false) }
    var initializationStatus by rememberWorldScaleSceneViewStatus()

    // Initialize the world scale tracking mode based on whether a google API key is provided
    val initialWorldScaleTrackingMode = when {
        SharedRepository.hasNonDefaultAPIKey -> { WorldScaleTrackingMode.Geospatial() }
        else -> { WorldScaleTrackingMode.World() }
    }

    var trackingMode by remember { mutableStateOf(initialWorldScaleTrackingMode) }

    var showDropdownMenu by remember { mutableStateOf(false) }
    var isPipeShadowVisible by remember { mutableStateOf(true) }
    var isLeaderVisible by remember { mutableStateOf(true) }

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
        topBar = {
            SampleTopAppBar(title = sampleName, actions = {
                var actionsExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                DropdownMenu(
                    expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                    DropdownMenuItem(text = { Text("World tracking") }, onClick = {
                        trackingMode = WorldScaleTrackingMode.World()
                        actionsExpanded = false
                    })
                    DropdownMenuItem(text = { Text("Geospatial tracking") }, onClick = {
                        trackingMode = WorldScaleTrackingMode.Geospatial()
                        actionsExpanded = false
                    })
                }
            })
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
            } else if (!acceptedPrivacyInfo) {
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
                        modifier = Modifier.fillMaxSize(),
                        arcGISScene = augmentedRealityViewModel.arcGISScene,
                        graphicsOverlays = listOf(
                            augmentedRealityViewModel.pipeGraphicsOverlay,
                            augmentedRealityViewModel.pipeShadowGraphicsOverlay,
                            augmentedRealityViewModel.leaderGraphicsOverlay
                        ),
                        onCurrentViewpointCameraChanged = { camera ->
                            if (camera.location.x != 0.0 && camera.location.y != 0.0) {
                                augmentedRealityViewModel.onCurrentViewpointCameraChanged(camera.location)
                            }
                        },
                        worldScaleSceneViewProxy = augmentedRealityViewModel.worldScaleSceneViewProxy,
                        worldScaleTrackingMode = trackingMode,
                        onInitializationStatusChanged = { status ->
                            initializationStatus = status
                        }) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (trackingMode is WorldScaleTrackingMode.World) {
                                if (displayCalibrationView) {
                                    CalibrationView(
                                        onDismiss = { displayCalibrationView = false },
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                    )
                                }
                            }
                        }
                    }
                    WorldScaleSceneViewStatusHandler(
                        initializationStatus = initializationStatus,
                        trackingMode = trackingMode,
                        arcGISSceneLoadStatus = augmentedRealityViewModel.arcGISScene.loadStatus.collectAsStateWithLifecycle().value
                    )
                    if (trackingMode is WorldScaleTrackingMode.Geospatial) {
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
                }
            }
        },
        floatingActionButton = {
            if (!displayCalibrationView) {
                FloatingActionButtonOptions(
                    showDropdownMenu = showDropdownMenu,
                    isPipeShadowVisible = isPipeShadowVisible,
                    isLeaderVisible = isLeaderVisible,
                    trackingMode = trackingMode,
                    onToggleDropdownMenu = { showDropdownMenu = !showDropdownMenu },
                    onTogglePipeShadowVisibility = {
                        isPipeShadowVisible = !isPipeShadowVisible
                        augmentedRealityViewModel.pipeShadowGraphicsOverlay.isVisible = isPipeShadowVisible
                    },
                    onToggleLeaderVisibility = {
                        isLeaderVisible = !isLeaderVisible
                        augmentedRealityViewModel.leaderGraphicsOverlay.isVisible = isLeaderVisible
                    },
                    onShowCalibrationView = { displayCalibrationView = true }
                )
            }
        }
    )
}

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

@Composable
private fun WorldScaleSceneViewStatusHandler(
    initializationStatus: WorldScaleSceneViewStatus,
    trackingMode: WorldScaleTrackingMode,
    arcGISSceneLoadStatus: LoadStatus
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (initializationStatus) {
            is WorldScaleSceneViewStatus.Initializing -> {
                TextWithScrim(
                    if (trackingMode is WorldScaleTrackingMode.Geospatial) {
                        "Initializing AR in geospatial mode..."
                    } else {
                        "Initializing AR in world mode..."
                    }
                )
            }

            is WorldScaleSceneViewStatus.Initialized -> {
                when (arcGISSceneLoadStatus) {
                    is LoadStatus.Loading, LoadStatus.NotLoaded -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is LoadStatus.FailedToLoad -> {
                        TextWithScrim("Failed to load world scale AR scene: " + arcGISSceneLoadStatus.error)
                    }

                    else -> {}
                }
            }

            is WorldScaleSceneViewStatus.FailedToInitialize -> {
                TextWithScrim(
                    text = "World scale AR failed to initialize: " + (initializationStatus.error.message
                        ?: initializationStatus.error)
                )
            }
        }
    }
}

@Composable
private fun FloatingActionButtonOptions(
    showDropdownMenu: Boolean,
    isPipeShadowVisible: Boolean,
    isLeaderVisible: Boolean,
    trackingMode: WorldScaleTrackingMode,
    onToggleDropdownMenu: () -> Unit,
    onTogglePipeShadowVisibility: () -> Unit,
    onToggleLeaderVisibility: () -> Unit,
    onShowCalibrationView: () -> Unit
) {
    Column {
        FloatingActionButton(
            modifier = Modifier.padding(bottom = 18.dp),
            onClick = onToggleDropdownMenu
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Toggle visibility of shadows and leaders")
        }
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = onToggleDropdownMenu
        ) {
            DropdownMenuItem(
                text = { Text("Shadows") },
                leadingIcon = {
                    if (isPipeShadowVisible) {
                        Icon(Icons.Default.Done, contentDescription = "Visible")
                    }
                },
                onClick = onTogglePipeShadowVisibility
            )
            DropdownMenuItem(
                text = { Text("Leaders") },
                leadingIcon = {
                    if (isLeaderVisible) {
                        Icon(Icons.Default.Done, contentDescription = "Visible")
                    }
                },
                onClick = onToggleLeaderVisibility
            )
        }
        if (trackingMode is WorldScaleTrackingMode.World) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 16.dp),
                onClick = onShowCalibrationView
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_straighten_24),
                    contentDescription = "Show calibration view"
                )
            }
        }
    }
}

/**
 * Displays the provided [text] on top of a half-transparent gray background.
 */
@Composable
private fun TextWithScrim(text: String) {
    Column(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.5f))
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
    }
}

/**
 * Displays the required privacy information for use of ARCore
 */
@Composable
private fun LegalTextArCore() {
    val textLinkStyle =
        TextLinkStyles(style = SpanStyle(color = Color.Blue))
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
                TextLinkStyles(style = SpanStyle(color = Color.Blue))
            )
        ) {
            append("Learn more")
        }
    })
}
