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

package com.esri.arcgismaps.sample.augmentrealitytonavigateroute.screens

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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.esri.arcgismaps.sample.augmentrealitytonavigateroute.R
import com.esri.arcgismaps.sample.augmentrealitytonavigateroute.components.AugmentedRealityViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

private const val KEY_PREF_ACCEPTED_PRIVACY_INFO = "ACCEPTED_PRIVACY_INFO"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AugmentedRealityScreen(
    sampleName: String
) {
    val augmentedRealityViewModel: AugmentedRealityViewModel = viewModel()

    // Set up the bottom sheet controls
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    var displayCalibrationView by remember { mutableStateOf(false) }
    var initializationStatus by rememberWorldScaleSceneViewStatus()
    var trackingMode by remember { mutableStateOf<WorldScaleTrackingMode>(WorldScaleTrackingMode.Geospatial()) }

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

    Scaffold(topBar = {
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
    }, content = {
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
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = augmentedRealityViewModel.arcGISScene,
                    graphicsOverlays = listOf(
                        augmentedRealityViewModel.routeAheadGraphicsOverlay,
                        augmentedRealityViewModel.routeBehindGraphicsOverlay
                    ),
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
                if (augmentedRealityViewModel.nextDirectionText != "") {
                    // Add directions text box at the top of the screen
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f))
                            .padding(16.dp)
                    ) {

                        Text(
                            text = augmentedRealityViewModel.nextDirectionText,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                // Show bottom sheet with controls to change number of graphics drawn ahead
                if (showBottomSheet) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentSize(),
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = CenterVertically
                        ) {
                            Text(
                                text = "Number of graphics drawn:",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                            Text(
                                text = augmentedRealityViewModel.currentGraphicsShown.toString(),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        Slider(
                            value = augmentedRealityViewModel.currentGraphicsShown.toFloat(),
                            onValueChange = { valueChanged ->
                                augmentedRealityViewModel.onCurrentGraphicsShownChanged(valueChanged.toInt())
                            },
                            valueRange = 1f..augmentedRealityViewModel.routeAllGraphics.size.toFloat(),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                            steps = augmentedRealityViewModel.routeAllGraphics.size - 2
                        )
                    }
                }

                when (val status = initializationStatus) {
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
                        val sceneLoadStatus =
                            augmentedRealityViewModel.arcGISScene.loadStatus.collectAsStateWithLifecycle().value
                        when (sceneLoadStatus) {
                            is LoadStatus.Loading, LoadStatus.NotLoaded -> {
                                // The scene may take a while to load, so show a progress indicator
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
            }
        }
    }, floatingActionButton = {
        if (!displayCalibrationView) {
            Column {
                if (trackingMode is WorldScaleTrackingMode.World) {

                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 16.dp),
                        onClick = { displayCalibrationView = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_straighten_24), "Show calibration view"
                        )
                    }
                }
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 32.dp), onClick = {
                        showBottomSheet = !showBottomSheet
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Settings, "Change settings"
                    )
                }
            }
        }
    })
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
