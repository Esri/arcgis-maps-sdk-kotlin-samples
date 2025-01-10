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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.utilitynetworks.UtilityTerminal
import com.arcgismaps.utilitynetworks.UtilityTraceType
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.traceutilitynetwork.components.PointType
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceState
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceUtilityNetworkViewModel

@Composable
fun TraceUtilityNetworkScreen(sampleName: String) {
    // Create the view model used by the sample
    val mapViewModel: TraceUtilityNetworkViewModel = viewModel()

    // Observe relevant states
    val hintText by mapViewModel.hint
        .collectAsStateWithLifecycle(null)
    val selectedTraceType by mapViewModel.selectedTraceType
        .collectAsStateWithLifecycle()
    val selectedPointType by mapViewModel.selectedPointType
        .collectAsStateWithLifecycle(PointType.None)
    val canPerformTrace by mapViewModel.canTrace
        .collectAsStateWithLifecycle(false)
    val traceState by mapViewModel.traceState
        .collectAsStateWithLifecycle(TraceState.NOT_STARTED)
    val terminalConfigurationOptions by mapViewModel.terminalConfigurationOptions
        .collectAsStateWithLifecycle(listOf())

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
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    selectionProperties = SelectionProperties(color = Color.yellow),
                    onSingleTapConfirmed = { tapEvent ->
                        tapEvent.mapPoint?.let {
                            mapViewModel.identifyNearestArcGISFeature(
                                mapPoint = it,
                                screenCoordinate = tapEvent.screenCoordinate
                            )
                        }
                    }
                )

                // Bottom toolbar with trace options
                TraceOptions(
                    hintText = hintText ?: "Trace Options",
                    isTraceButtonEnabled = canPerformTrace,
                    utilityTraceType = selectedTraceType,
                    pointType = selectedPointType,
                    traceState = traceState,
                    onTraceSelected = mapViewModel::traceUtilityNetwork,
                    onPointTypeChanged = mapViewModel::updatePointType,
                    onTraceTypeSelected = mapViewModel::updateTraceType,
                    onResetSelected = mapViewModel::reset
                )
            }

            if (traceState == TraceState.TERMINAL_CONFIGURATION_REQUIRED) {
                TerminalConfigurationDialog(
                    terminalConfigurationOptions = terminalConfigurationOptions,
                    onTerminalConfigurationSelected = mapViewModel::updateTerminalConfigurationOption
                )
            }

            if (traceState == TraceState.RUNNING_TRACE_UTILITY_NETWORK) {
                RunningTraceDialog(
                    traceName = selectedTraceType?.javaClass?.simpleName.toString()
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
fun TerminalConfigurationDialog(
    terminalConfigurationOptions: List<UtilityTerminal>,
    onTerminalConfigurationSelected: (Int) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties()
    ) {
        Surface {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = "Select Terminal"
                )
                terminalConfigurationOptions.fastForEachIndexed { index, utilityTerminal ->
                    OutlinedButton(onClick = { onTerminalConfigurationSelected(index) }) {
                        Text(utilityTerminal.name)
                    }
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
    hintText: String,
    utilityTraceType: UtilityTraceType?,
    pointType: PointType?,
    traceState: String,
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
            selectedTraceType = utilityTraceType,
            onTraceTypeSelected = onTraceTypeSelected
        )

        // Display segmented button for starting point type or barrier point type
        SegmentedButtonTracePointTypes(
            currentPointType = pointType,
            onPointTypeChanged = onPointTypeChanged,
            isPointTypesEnabled = utilityTraceType != null
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
                enabled = true
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
        } else selectedTraceName = selectedTraceType.javaClass.simpleName
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
                DropdownMenuItem(
                    text = { Text(traceType.javaClass.simpleName) },
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
    var selectedIndex = when (currentPointType) {
        PointType.None -> -1
        PointType.Start -> 0
        PointType.Barrier -> 1
        null -> -1
    }

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

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewTraceUtilityNetworkScreen() {
    SampleAppTheme {
        Surface {
            TraceOptions(
                isTraceButtonEnabled = true,
                hintText = "Trace options",
                utilityTraceType = null,
                pointType = null,
                traceState = TraceState.NOT_STARTED,
                onTraceTypeSelected = { },
                onPointTypeChanged = { },
                onResetSelected = { },
                onTraceSelected = { }
            )
        }
    }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewTerminalConfigurationDialog() {
    SampleAppTheme { Surface { TerminalConfigurationDialog(listOf()) {} } }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewRunningTraceDialog() {
    SampleAppTheme { Surface { RunningTraceDialog("Downstream") } }
}
