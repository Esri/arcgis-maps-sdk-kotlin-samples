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
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.traceutilitynetwork.components.PointType
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceUtilityNetworkViewModel
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceState

@Composable
fun TraceUtilityNetworkScreen(sampleName: String) {
    // Create the view model used by the sample
    val mapViewModel: TraceUtilityNetworkViewModel = viewModel()

    // Observe relevant states
    val hintText by mapViewModel.hint
        .collectAsStateWithLifecycle(null)
    val traceState by mapViewModel.traceState
        .collectAsStateWithLifecycle(TraceState.None)
    val pendingTraceParameters by mapViewModel.pendingTraceParameters
        .collectAsStateWithLifecycle(null)
    val terminalSelectorIsOpen by mapViewModel.terminalSelectorIsOpen
        .collectAsStateWithLifecycle(false)
    val canPerformTrace by mapViewModel.canTrace
        .collectAsStateWithLifecycle(false)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
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
                TraceOptions(
                    traceState = traceState,
                    hintText = hintText ?: "Trace Options",
                    isTraceButtonEnabled = canPerformTrace,
                    selectedTraceType = pendingTraceParameters?.traceType,
                    currentPointType = (traceState as? TraceState.SettingPoints)?.pointType,
                    onPointTypeChanged = mapViewModel::setPointType,
                    onTraceTypeSelected = mapViewModel::setTraceParameters,
                    onResetSelected = mapViewModel::reset,
                    onTraceSelected = mapViewModel::trace
                )
            }

            if (terminalSelectorIsOpen) {
                TerminalConfigurationDialog()
            }

            if (traceState is TraceState.TraceRunning && pendingTraceParameters?.traceType != null) {
                RunningTraceDialog(
                    traceName = pendingTraceParameters?.traceType?.javaClass?.simpleName.toString()
                )
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
fun TerminalConfigurationDialog() {
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


/**
 * - A trace-type picker
 * - A segmented button for Start vs. Barrier
 * - “Trace” and “Reset” buttons
 */
@Composable
fun TraceOptions(
    isTraceButtonEnabled: Boolean,
    traceState: TraceState,
    hintText: String,
    selectedTraceType: UtilityTraceType?,
    currentPointType: PointType?,
    onTraceTypeSelected: (UtilityTraceType) -> Unit,
    onPointTypeChanged: (PointType) -> Unit,
    onResetSelected: () -> Unit,
    onTraceSelected: () -> Unit

) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = hintText, style = MaterialTheme.typography.labelLarge)

        // Show a dropdown menu to pick a new trace type
        ExposedDropdownMenuBoxWithTraceTypes(
            selectedTraceType = selectedTraceType,
            onTraceTypeSelected = onTraceTypeSelected
        )

        // Display segmented button for starting point type or barrier point type
        SegmentedButtonTracePointTypes(
            currentPointType = currentPointType,
            onPointTypeChanged = onPointTypeChanged,
            isPointTypesEnabled = selectedTraceType != null
        )

        // Display a row with reset and trace controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(
                onClick = { onResetSelected() },
                enabled = (traceState !is TraceState.TraceRunning)
            ) {
                Text("Reset")
            }
            Button(
                onClick = { onTraceSelected() },
                enabled = isTraceButtonEnabled
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
    selectedTraceType: UtilityTraceType?,
    onTraceTypeSelected: (UtilityTraceType) -> Unit
) {
    val traceOptions = listOf(
        UtilityTraceType.Downstream,
        UtilityTraceType.Upstream,
        UtilityTraceType.Subnetwork,
        UtilityTraceType.Connected
    )

    var selectedTraceName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(selectedTraceType) {
        if (selectedTraceType == null) {
            focusManager.clearFocus()
            selectedTraceName = "Select a trace type"
        } else selectedTraceName = traceTypeDisplayName(selectedTraceType)
    }

    ExposedDropdownMenuBox(
        modifier = Modifier.focusRequester(focusRequester),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            label = { Text("Trace Type") },
            value = selectedTraceName,
            readOnly = true,
            onValueChange = { },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
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

@Composable
fun SegmentedButtonTracePointTypes(
    currentPointType: PointType?,
    onPointTypeChanged: (PointType) -> Unit,
    isPointTypesEnabled: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Show segmented button for Start vs Barrier
    var selectedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentPointType) {
        if (currentPointType == null) {
            focusManager.clearFocus()
            selectedIndex = -1
        } else {
            selectedIndex = if (currentPointType == PointType.Start) 0 else 1
        }
    }

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    ) {
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
                enabled = isPointTypesEnabled,
                selected = (index == selectedIndex)
            ) { Text(label) }
        }
    }
}


@Composable
fun RunningTraceDialog(traceName: String) {
    LoadingDialog(loadingMessage = "Running $traceName trace...")
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
        Surface {
            TraceOptions(
                isTraceButtonEnabled = true,
                traceState = TraceState.None,
                selectedTraceType = null,
                currentPointType = null,
                hintText = "Trace options",
                onTraceTypeSelected = { },
                onPointTypeChanged = { },
                onResetSelected = { },
                onTraceSelected = { },
            )
        }
    }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewTerminalConfigurationDialog() {
    SampleAppTheme { Surface { TerminalConfigurationDialog() } }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewRunningTraceDialog() {
    SampleAppTheme { Surface { RunningTraceDialog("Downstream") } }
}
