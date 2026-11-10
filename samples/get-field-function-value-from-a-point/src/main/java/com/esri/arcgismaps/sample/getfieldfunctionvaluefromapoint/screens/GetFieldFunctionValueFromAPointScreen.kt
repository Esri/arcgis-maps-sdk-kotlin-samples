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

package com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.R
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.DragHapticEvent
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.FieldSampleUiState
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.FieldSampleUiState.Companion.initialFieldSampleUiState
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.GetFieldFunctionValueFromAPointViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDeviceLightDarkPreview
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.components.adaptive.AdaptiveThreePane
import kotlinx.coroutines.flow.Flow

/**
 * Main composable screen for the sample.
 * It owns the ViewModel and hoists UI state for the scaffold and the MapView.
 */
@Composable
fun GetFieldFunctionValueFromAPointScreen(
    viewModel: GetFieldFunctionValueFromAPointViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    // Set up a LaunchedEffect to perform haptic feedback whenever a drag of the aircraft location
    // starts or ends
    InteractiveViewshedHapticFeedback(viewModel.dragHapticEvents)

    MainScreenScaffold(
        uiState = uiState,
        onAircraftAltitudeChange = viewModel::setAircraftAltitude,
        mainPaneContent = {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISMap = viewModel.arcGISMap,
                mapViewProxy = viewModel.mapViewProxy,
                mapViewInteractionOptions =
                    MapViewInteractionOptions(isEnabled = !viewModel.isDragging),
                analysisOverlays = listOf(viewModel.analysisOverlay),
                graphicsOverlays = listOf(viewModel.graphicsOverlay),
                onSingleTapConfirmed = viewModel::onTap,
                onLongPress = viewModel::onLongPress,
                onPan = viewModel::onPan,
                onAnalysisViewStatusChanged = viewModel::analysisViewStatusListener,
                content = {
                    // Show a callout only when the calloutGraphic is set
                    viewModel.calloutGraphic?.let { graphic ->
                        Callout(
                            geoElement = graphic,
                            shapes = CalloutDefaults.shapes(
                                calloutContentPadding = PaddingValues(all = 8.dp)
                            ),
                            colorScheme = CalloutDefaults.colors(
                                backgroundColor = MaterialTheme.colorScheme.background,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            // Callout content:
                            Column {
                                Text(
                                    text = "Elevation (m): %.2f".format(viewModel.sampleElevation),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Clearance above the ground (m): %.2f".format(viewModel.groundClearance),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Ground clearance status: ${viewModel.groundClearanceStatus}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            )
        }
    )

    viewModel.messageDialogVM.apply {
        if (dialogStatus) {
            MessageDialog(
                title = messageTitle,
                description = messageDescription,
                onDismissRequest = ::dismissDialog
            )
        }
    }
}

@Composable
private fun MainScreenScaffold(
    uiState: FieldSampleUiState,
    onAircraftAltitudeChange: (Float) -> Unit = {},
    mainPaneContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.get_field_function_value_from_a_point_app_name)) },
        content = { paddingValues ->
            AdaptiveThreePane(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                supportingPaneTitle = "Settings",
                mainPane = { _, _ -> mainPaneContent() },
                supportingPane = { _, _ ->
                    GetFieldFunctionValueFromAPointSupportingPane(
                        uiState = uiState,
                        onAircraftAltitudeChange = onAircraftAltitudeChange
                    )
                }
            )
        }
    )
}

@Composable
fun InteractiveViewshedHapticFeedback(dragHapticEvents: Flow<DragHapticEvent>) {
    val view = LocalView.current
    LaunchedEffect(dragHapticEvents, view) {
        // Perform haptic feedback whenever a drag of the observer position starts or ends
        dragHapticEvents.collect { event ->
            val hapticFeedbackConstant = when (event) {
                DragHapticEvent.Start -> HapticFeedbackConstants.LONG_PRESS
                DragHapticEvent.End -> HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(hapticFeedbackConstant)
        }
    }
}

@SampleDeviceLightDarkPreview
@Composable
fun MainScreenPreview() {
    SamplePreviewSurface {
        MainScreenScaffold(
            uiState = initialFieldSampleUiState,
            mainPaneContent = {}
        )
    }
}
