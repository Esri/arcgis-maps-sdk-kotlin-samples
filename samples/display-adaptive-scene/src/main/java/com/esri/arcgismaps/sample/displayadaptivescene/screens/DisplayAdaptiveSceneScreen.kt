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

package com.esri.arcgismaps.sample.displayadaptivescene.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.displayadaptivescene.AdaptiveThreePaneTemplate
import com.esri.arcgismaps.sample.displayadaptivescene.components.AdaptiveSceneUiState
import com.esri.arcgismaps.sample.displayadaptivescene.components.DisplayAdaptiveSceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplayAdaptiveSceneScreen(sampleName: String) {
    val viewModel: DisplayAdaptiveSceneViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            DisplayAdaptiveSceneContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                arcGISScene = viewModel.arcGISScene,
                sceneViewProxy = viewModel.sceneViewProxy,
                uiState = uiState,
                onAtmosphereChanged = viewModel::setShowAtmosphere,
                onHeadingChanged = viewModel::setHeading,
                onPitchChanged = viewModel::setPitch,
                onDistanceChanged = viewModel::setDistance,
                onCurrentViewpointCameraChanged = viewModel::onCurrentViewpointCameraChanged,
                onResetCamera = viewModel::resetCamera,
                onCameraPresetSelected = viewModel::applyCameraPreset
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
    )
}

@Composable
private fun DisplayAdaptiveSceneContent(
    modifier: Modifier,
    arcGISScene: ArcGISScene,
    sceneViewProxy: SceneViewProxy,
    uiState: AdaptiveSceneUiState,
    onAtmosphereChanged: (Boolean) -> Unit,
    onHeadingChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onDistanceChanged: (Float) -> Unit,
    onCurrentViewpointCameraChanged: (Camera) -> Unit,
    onResetCamera: () -> Unit,
    onCameraPresetSelected: (Int) -> Unit,
) {
    val defaultFocusPoint = remember {
        Point(
            x = -117.1958,
            y = 34.0563,
            spatialReference = SpatialReference.wgs84()
        )
    }
    val currentCameraState = uiState.currentCameraState
    val containerSize = LocalWindowInfo.current.containerSize

    // Restore the current viewpoint when the available window changes.
    LaunchedEffect(containerSize.width, containerSize.height) {
        currentCameraState?.let {
            Camera(
                locationPoint = Point(
                    x = it.x,
                    y = it.y,
                    z = it.z,
                    spatialReference = defaultFocusPoint.spatialReference
                ),
                heading = it.heading.toDouble(),
                pitch = it.pitch.toDouble(),
                roll = it.roll.toDouble()
            )
        }?.let(sceneViewProxy::setViewpointCamera)
    }

    // Animate to user-requested targets from the current SceneView camera.
    LaunchedEffect(uiState.cameraCommandId) {
        if (uiState.cameraCommandId == 0L) return@LaunchedEffect
        val lookAtPoint = currentCameraState?.let {
            Point(
                x = it.x,
                y = it.y,
                z = it.z,
                spatialReference = defaultFocusPoint.spatialReference
            )
        } ?: defaultFocusPoint

        sceneViewProxy.setViewpointCameraAnimated(
            camera = Camera(
                lookAtPoint = lookAtPoint,
                distance = uiState.cameraDistance.toDouble(),
                heading = uiState.cameraHeading.toDouble(),
                pitch = uiState.cameraPitch.toDouble(),
                roll = 0.0
            ),
            duration = 450.milliseconds
        )
    }

    val layoutDirection = LocalLayoutDirection.current

    AdaptiveThreePaneTemplate(
        modifier = modifier,
        mainPane = { isSupportingPaneVisible, _, openSupportingPane ->
            SceneView(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                arcGISScene = arcGISScene,
                sceneViewProxy = sceneViewProxy,
                onCurrentViewpointCameraChanged = onCurrentViewpointCameraChanged,
                atmosphereEffect = if (uiState.showAtmosphere) {
                    AtmosphereEffect.Realistic
                } else {
                    AtmosphereEffect.None
                }
            )

            if (!isSupportingPaneVisible) {
                val fabAlignment = if (layoutDirection == LayoutDirection.Ltr) {
                    Alignment.TopEnd
                } else {
                    Alignment.TopStart
                }
                FloatingActionButton(
                    modifier = Modifier
                        .align(fabAlignment)
                        .padding(12.dp)
                        .semantics { contentDescription = "Open scene controls" },
                    onClick = openSupportingPane
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                }
            }
        },
        supportingPane = { closeSupportingPane, isFloatingPaneVisible, toggleFloatingPane ->
            SupportingControlsPane(
                uiState = uiState,
                onAtmosphereChanged = onAtmosphereChanged,
                onHeadingChanged = onHeadingChanged,
                onPitchChanged = onPitchChanged,
                onDistanceChanged = onDistanceChanged,
                onResetCamera = onResetCamera,
                onClosePane = closeSupportingPane,
                isFloatingPaneVisible = isFloatingPaneVisible,
                onToggleFloatingWidget = toggleFloatingPane
            )
        },
        floatingPane = { dismissFloatingPane ->
            FloatingSceneWidget(
                modifier = Modifier,
                uiState = uiState,
                onDismiss = dismissFloatingPane,
                onResetCamera = onResetCamera,
                onCameraPresetSelected = onCameraPresetSelected
            )
        }
    )
}

@Composable
private fun SupportingControlsPane(
    uiState: AdaptiveSceneUiState,
    onAtmosphereChanged: (Boolean) -> Unit,
    onHeadingChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onDistanceChanged: (Float) -> Unit,
    onResetCamera: () -> Unit,
    onClosePane: () -> Unit,
    isFloatingPaneVisible: Boolean,
    onToggleFloatingWidget: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(6.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClosePane) {
                    Icon(Icons.Default.Close, contentDescription = "Hide controls")
                }
            }

            Text(text = "Scene controls", style = MaterialTheme.typography.titleLarge)


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Atmosphere effect", style = MaterialTheme.typography.titleMedium)
                Switch(checked = uiState.showAtmosphere, onCheckedChange = onAtmosphereChanged)
            }

            LabeledSlider(
                label = "Heading",
                value = uiState.cameraHeading,
                valueSuffix = "°",
                valueRange = 0f..360f,
                onValueChange = onHeadingChanged
            )

            LabeledSlider(
                label = "Pitch",
                value = uiState.cameraPitch,
                valueSuffix = "°",
                valueRange = 20f..89f,
                onValueChange = onPitchChanged
            )

            LabeledSlider(
                label = "Distance",
                value = uiState.cameraDistance,
                valueSuffix = " m",
                valueRange = 1_500f..18_000f,
                onValueChange = onDistanceChanged
            )

            FilledTonalButton(onClick = onResetCamera) {
                Text("Reset camera")
            }

            OutlinedButton(onClick = onToggleFloatingWidget) {
                Text(
                    if (isFloatingPaneVisible) {
                        "Hide floating widget"
                    } else {
                        "Show floating widget"
                    }
                )
            }
        }
    }
}


@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueSuffix: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label: ${value.roundToInt()}$valueSuffix",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingSceneWidget(
    modifier: Modifier,
    uiState: AdaptiveSceneUiState,
    onDismiss: () -> Unit,
    onResetCamera: () -> Unit,
    onCameraPresetSelected: (Int) -> Unit,
) {
    Card(
        modifier = modifier
            .blur(radius = 0.5.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .semantics { contentDescription = "Floating scene widget" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DragIndicator, contentDescription = null)
                    Text(
                        text = "Floating camera widget",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss floating widget")
                }
            }

            Text(
                text = "Target H ${uiState.cameraHeading.roundToInt()}°, " +
                        "P ${uiState.cameraPitch.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Applied H ${uiState.currentCameraState?.heading?.roundToInt() ?: uiState.cameraHeading.roundToInt()}°, " +
                        "P ${uiState.currentCameraState?.pitch?.roundToInt() ?: uiState.cameraPitch.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium
            )
            SingleChoiceSegmentedButtonRow {
                val labels = listOf("Wide", "City", "Close")
                labels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = uiState.selectedPresetIndex == index,
                        onClick = { onCameraPresetSelected(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, labels.size)
                    ) {
                        Text(label)
                    }
                }
            }
            Button(onClick = onResetCamera) {
                Text("Reset view")
            }
        }
    }
}
