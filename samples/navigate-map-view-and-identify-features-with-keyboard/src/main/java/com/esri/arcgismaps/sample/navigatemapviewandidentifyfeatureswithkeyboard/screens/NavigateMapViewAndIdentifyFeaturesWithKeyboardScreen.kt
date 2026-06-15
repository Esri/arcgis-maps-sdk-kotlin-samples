/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.R
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components.NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun NavigateMapViewAndIdentifyFeaturesWithKeyboardScreen(
    mapViewModel: NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel = viewModel()
) {
    val focusRequester = remember { FocusRequester() }
    val areaOfInterestSize = with(LocalDensity.current) { 200.toDp() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.navigate_map_view_and_identify_features_with_keyboard_app_name)) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        when (keyEvent.key) {
                            Key.Escape -> {
                                mapViewModel.dismissCallout()
                                true
                            }
                            else -> {
                                numberKeyToFeatureIndex(keyEvent.key)?.let { index ->
                                    mapViewModel.showCalloutForFeatureIndex(index)
                                    true
                                } ?: false
                            }
                        }
                    }
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged(mapViewModel::updateMapViewSize),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.labelsOverlay),
                    selectionProperties = mapViewModel.selectionProperties,
                    onDrawStatusChanged = mapViewModel::handleDrawStatusChanged,
                    onNavigationChanged = mapViewModel::handleNavigationChanged,
                    content = {
                        mapViewModel.calloutState?.let { calloutState ->
                            Callout(
                                modifier = Modifier.widthIn(max = 250.dp),
                                location = calloutState.location,
                                shapes = CalloutDefaults.shapes(
                                    calloutContentPadding = PaddingValues(8.dp)
                                ),
                                colorScheme = CalloutDefaults.colors(
                                    backgroundColor = MaterialTheme.colorScheme.background,
                                    borderColor = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column {
                                    Text(
                                        text = calloutState.title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = calloutState.details,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                )

                SampleInstructions(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    isOverflowMessageVisible = mapViewModel.isOverflowMessageVisible
                )

                if (mapViewModel.calloutState == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(areaOfInterestSize)
                            .border(
                                width = 2.dp,
                                color = Color(0xFF1F2328),
                                shape = RoundedCornerShape(4.dp)
                            )
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
private fun SampleInstructions(
    modifier: Modifier = Modifier,
    isOverflowMessageVisible: Boolean
) {
    Surface(
        modifier = modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.navigate_map_view_and_identify_features_with_keyboard_instructions),
                style = MaterialTheme.typography.bodyMedium
            )
            if (isOverflowMessageVisible) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.navigate_map_view_and_identify_features_with_keyboard_overflow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun numberKeyToFeatureIndex(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 0
    Key.Two, Key.NumPad2 -> 1
    Key.Three, Key.NumPad3 -> 2
    Key.Four, Key.NumPad4 -> 3
    Key.Five, Key.NumPad5 -> 4
    Key.Six, Key.NumPad6 -> 5
    Key.Seven, Key.NumPad7 -> 6
    Key.Eight, Key.NumPad8 -> 7
    Key.Nine, Key.NumPad9 -> 8
    else -> null
}
