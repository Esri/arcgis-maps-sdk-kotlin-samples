/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.data.CodedValue
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.SliderControlParameters
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.UIState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()

    // flows from view model for displaying state in UI
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val statusAttributes by mapViewModel.statusAttributes.collectAsStateWithLifecycle()
    val protectionAttributes by mapViewModel.protectionAttributes.collectAsStateWithLifecycle()
    val sliderControlParameters by mapViewModel.sliderControlParameters.collectAsStateWithLifecycle()

    // point on map tapped by user
    var mapPoint by remember { mutableStateOf<Point?>(null) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            var showBottomSheet by remember { mutableStateOf(false) }
            val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            Box {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = {
                        it.mapPoint.let { point ->
                            mapPoint = point
                            showBottomSheet = true
                        }
                    }
                )

                if (showBottomSheet) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = bottomSheetState,
                        onDismissRequest = {
                            showBottomSheet = false
                            mapViewModel.clearFeature()
                        }
                    ) {
                        val onBottomSheetStateChanged = { state: SheetState ->
                            if (!bottomSheetState.isVisible) {
                                showBottomSheet = false
                                mapViewModel.clearFeature()
                            }
                        }
                        BottomSheetContents(
                            mapPoint,
                            bottomSheetState,
                            onBottomSheetStateChanged,
                            uiState,
                            statusAttributes,
                            mapViewModel::onStatusAttributeSelect,
                            protectionAttributes,
                            mapViewModel::onProtectionAttributeSelect,
                            sliderControlParameters,
                            mapViewModel::onBufferSizeSelect,
                            mapViewModel::validateContingency
                        )
                    }
                }
            }

            // message dialog view model for displaying error messages
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContents(
    mapPoint: Point?,
    bottomSheetState: SheetState,
    onBottomSheetStateChange: (SheetState) -> Unit,
    uiState: UIState,
    statusAttributes: List<CodedValue>,
    onStatusAttributeSelect: (CodedValue) -> Unit,
    protectionAttributes: List<CodedValue>,
    onProtectionAttributeSelect: (CodedValue) -> Unit,
    sliderControlParameters: SliderControlParameters,
    onBufferSizeSelect: (Int) -> Unit,
    onApplyButtonClicked: (Point) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = "Add Feature",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.size(8.dp))
        Text("Attributes:")

        // dropdown boxes for selecting feature status/protection
        AttributeDropdown("Status", uiState.status, statusAttributes, onStatusAttributeSelect)
        AttributeDropdown("Protection", uiState.protection, protectionAttributes, onProtectionAttributeSelect)
        Spacer(Modifier.size(8.dp))

        // buffer size displayed and updated in slider
        var localBufferSize by remember { mutableIntStateOf(uiState.buffer) }

        // update buffer size if it changes in the view model
        var previousBufferSize by remember { mutableIntStateOf(0) }
        if (uiState.buffer != previousBufferSize) {
            previousBufferSize = uiState.buffer
            localBufferSize = uiState.buffer
        }

        // recenter the slider if contingent values change
        var bufferRange = sliderControlParameters.minRange..sliderControlParameters.maxRange
        if (!bufferRange.contains(localBufferSize)) {
            localBufferSize = (bufferRange.start + bufferRange.endInclusive) / 2
        }

        Row {
            Text("Exclusion area buffer size:")
            Spacer(Modifier.weight(1f))
            Text(text = if (localBufferSize > 0) localBufferSize.toString() else "")
        }
        Slider(
            enabled = sliderControlParameters.isEnabled,
            value = localBufferSize.toFloat(),
            valueRange = sliderControlParameters.minRange.toFloat()..sliderControlParameters.maxRange.toFloat(),
            steps = (bufferRange.endInclusive - bufferRange.start).toInt(),
            onValueChange = { localBufferSize = it.roundToInt() },
            onValueChangeFinished = { onBufferSizeSelect(localBufferSize) },
            track = { sliderState ->
                SliderDefaults.Track(
                    enabled = sliderControlParameters.isEnabled,
                    sliderState = sliderState,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> }
                )
            }
        )
        HorizontalDivider()
        Text("The options will vary depending on which values are selected.")
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                // user may not have interacted with the slider - need to assign a value
                onBufferSizeSelect(localBufferSize)
                mapPoint?.let {
                    onApplyButtonClicked(it)
                }
                coroutineScope.launch {
                    bottomSheetState.hide()
                }.invokeOnCompletion {
                    onBottomSheetStateChange(bottomSheetState)
                }
            }
        ) {
            Text("Apply")
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeDropdown(
    attributeName: String,
    codedValue: CodedValue?,
    attributeOptions: List<CodedValue>,
    onNewValueSelect: (CodedValue) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        onExpandedChange = {
            if (!attributeOptions.isEmpty()) {
                expanded = !expanded
            }
        },
        expanded = expanded
    ) {

        val textValue = codedValue?.name

        OutlinedTextField(
            enabled = !attributeOptions.isEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            value = textValue ?: "",
            onValueChange = {},
            label = { Text("Select $attributeName Attribute") },
            readOnly = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            attributeOptions.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.name) },
                    onClick = {
                        expanded = false
                        onNewValueSelect(value)
                    }
                )
            }
        }
    }
}
