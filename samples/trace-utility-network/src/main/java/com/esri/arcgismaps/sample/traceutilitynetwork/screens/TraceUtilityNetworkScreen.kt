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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.traceutilitynetwork.screens

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.utilitynetworks.UtilityTraceParameters
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.traceutilitynetwork.components.PointType
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceUtilityNetworkViewModel
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TracingActivity

@Composable
fun TraceUtilityNetworkScreen(sampleName: String) {
    // Create the view model used by the sample
    val mapViewModel: TraceUtilityNetworkViewModel = viewModel()

    // Observe relevant states
    val hintText by mapViewModel.hint
        .collectAsStateWithLifecycle(null)
    val tracingActivity by mapViewModel.tracingActivity
        .collectAsStateWithLifecycle(TracingActivity.None)
    val pendingTraceParameters by mapViewModel.pendingTraceParameters
        .collectAsStateWithLifecycle(null)
    val terminalSelectorIsOpen by mapViewModel.terminalSelectorIsOpen
        .collectAsStateWithLifecycle(false)
    val pendingTraceParams by mapViewModel.pendingTraceParameters
        .collectAsState(null)

    // React to changes in tracingActivity – e.g., if we just switched to TraceRunning, call trace()
    LaunchedEffect(tracingActivity) {
        if (tracingActivity is TracingActivity.TraceRunning) {
            try {
                mapViewModel.trace() // runs the trace
                // If successful, the VM sets TracingActivity to TraceCompleted
            } catch (e: Exception) {
                // If thrown, the VM presumably sets TracingActivity.TraceFailed
            }
        }
    }

    // remove credentials on screen dispose
    DisposableEffect(Unit) {
        onDispose {
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.removeAll()
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.pointsOverlay),
                    onSingleTapConfirmed = { tapEvent ->
                        mapViewModel.identifyFeature(tapEvent)
                    }
                )

                // Bottom toolbar with trace options
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hintText != null) {
                        Text(text = hintText ?: "")
                    }

                    TraceOptions(
                        pendingTraceParams = pendingTraceParams,
                        tracingActivity = tracingActivity,
                        onPointTypeChanged = mapViewModel::setPointType,
                        onTraceTypeSelected = mapViewModel::setTraceParameters,
                        onResetSelected = mapViewModel::reset,
                        onTraceSelected = mapViewModel::trace
                    )
                }
            }

            if (terminalSelectorIsOpen) {
                BasicAlertDialog(
                    onDismissRequest = {},
                    properties = DialogProperties()
                ) {
                    Surface {
                        Column {
                            Text(
                                modifier = Modifier.padding(24.dp),
                                text = "Select Terminal"
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {}) { Text("High") }
                                OutlinedButton(onClick = {}) { Text("Low") }
                            }
                        }
                    }
                }
            }

            if (tracingActivity is TracingActivity.TraceRunning &&
                pendingTraceParameters?.traceType != null
            ) {
                RunningTraceDialog(
                    traceName = pendingTraceParameters?.traceType?.javaClass?.simpleName.toString()
                )
            }
        }
    )
}

@Composable
fun RunningTraceDialog(traceName: String) {
    BasicAlertDialog(onDismissRequest = {}, content = {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Running $traceName trace...")
                CircularProgressIndicator()
            }
        }
    })
}

/**
 * - A trace-type picker
 * - A segmented button for Start vs. Barrier
 * - “Trace” and “Reset” buttons
 */
@Composable
fun TraceOptions(
    pendingTraceParams: UtilityTraceParameters?,
    tracingActivity: TracingActivity,
    onTraceTypeSelected: (UtilityTraceType) -> Unit,
    onPointTypeChanged: (PointType) -> Unit,
    onResetSelected: () -> Unit,
    onTraceSelected: () -> Unit

) {
    // Observe state
    val canTrace = (pendingTraceParams?.startingLocations?.isNotEmpty() == true)
    // Example list of supported trace types
    val traceOptions = listOf(
        UtilityTraceType.Downstream,
        UtilityTraceType.Upstream,
        UtilityTraceType.Subnetwork,
        UtilityTraceType.Connected
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show a menu to pick a new trace type
        ExposedDropdownMenuBoxWithTraceTypes(
            traceOptions = traceOptions,
            onTraceTypeSelected = onTraceTypeSelected
        )

        // Show segmented button for Start vs Barrier
        var selectedIndex by remember { mutableIntStateOf(0) }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf("Add starting location(s)", "Add barrier(s)")
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index, count = options.size
                    ),
                    onClick = {
                        selectedIndex = index
                        // Switch between Start/Barrier
                        onPointTypeChanged(if (index == 0) PointType.Start else PointType.Barrier)
                    },
                    selected = (index == selectedIndex)
                ) { Text(label) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(
                onClick = { onResetSelected() },
                enabled = (tracingActivity !is TracingActivity.TraceRunning)
            ) {
                Text("Reset")
            }
            Button(
                onClick = { onTraceSelected() },
                enabled = canTrace
            ) {
                Text("Trace")
            }
        }
    }
}

/**
 * A simple example of a “Trace Type” dropdown using ExposedDropdownMenuBox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxWithTraceTypes(
    traceOptions: List<UtilityTraceType>,
    onTraceTypeSelected: (UtilityTraceType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTraceName by remember { mutableStateOf("Select a trace type") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedTraceName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Trace Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            traceOptions.forEachIndexed { index, traceType ->
                val displayName = traceTypeDisplayName(traceType)
                DropdownMenuItem(
                    text = { Text(displayName) },
                    onClick = {
                        selectedTraceName = displayName
                        expanded = false
                        onTraceTypeSelected(traceType)
                    }
                )
                if (index < traceOptions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

fun traceTypeDisplayName(type: UtilityTraceType): String =
    when (type) {
        UtilityTraceType.Connected -> "Connected"
        UtilityTraceType.Downstream -> "Downstream"
        UtilityTraceType.Isolation -> "Isolation"
        UtilityTraceType.Loops -> "Loops"
        UtilityTraceType.ShortestPath -> "Shortest Path"
        UtilityTraceType.Subnetwork -> "Subnetwork"
        UtilityTraceType.Upstream -> "Upstream"
        else -> "Unknown"
    }

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewTraceUtilityNetworkScreen() {
    SampleAppTheme {
    }
}
