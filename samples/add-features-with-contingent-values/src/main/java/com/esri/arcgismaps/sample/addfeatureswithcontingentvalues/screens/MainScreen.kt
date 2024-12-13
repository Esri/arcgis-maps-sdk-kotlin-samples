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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.data.CodedValue
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.R
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.FeatureEditState
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
    val featureEditState by mapViewModel.featureEditState.collectAsStateWithLifecycle()

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
                            if (!state.isVisible) {
                                showBottomSheet = false
                                mapViewModel.clearFeature()
                            }
                        }
                        BottomSheetContents(
                            mapPoint,
                            bottomSheetState,
                            onBottomSheetStateChanged,
                            featureEditState,
                            mapViewModel::onStatusAttributeSelected,
                            mapViewModel::onProtectionAttributeSelected,
                            mapViewModel::onBufferSizeSelected,
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
    featureEditState: FeatureEditState,
    onStatusAttributeSelect: (CodedValue) -> Unit,
    onProtectionAttributeSelect: (CodedValue) -> Unit,
    onBufferSizeSelect: (Int) -> Unit,
    onApplyButtonClicked: (Point) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.add_feature),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.attributes))

        // dropdown boxes for selecting feature status/protection
        AttributeDropdown(
            attributeName = stringResource(R.string.status),
            codedValue = featureEditState.status,
            availableValues = featureEditState.statusAttributes,
            onNewValueSelect = onStatusAttributeSelect
        )
        AttributeDropdown(
            attributeName = stringResource(R.string.protection),
            codedValue = featureEditState.protection,
            availableValues = featureEditState.protectionAttributes,
            onNewValueSelect = onProtectionAttributeSelect)
        Spacer(Modifier.size(8.dp))

        // buffer size displayed and updated in slider
        var bufferSize by remember(key1 = featureEditState) { mutableIntStateOf(featureEditState.buffer) }

        val bufferRange = if (featureEditState.bufferRange != null) {
            val min = featureEditState.bufferRange.minValue as Int
            val max = featureEditState.bufferRange.maxValue as Int
            min..max
        } else {0..0}

        // recenter the slider if contingent values change
        if (!bufferRange.contains(bufferSize)){
            bufferSize = (bufferRange.first + bufferRange.last) /2
        }

        Row {
            Text(stringResource(R.string.exclusion_area_buffer_size))
            Spacer(Modifier.weight(1f))
            Text(text = if (bufferSize > 0) bufferSize.toString() else "")
        }

        Slider(
            enabled = bufferRange.first != bufferRange.last,
            value = bufferSize.toFloat(),
            valueRange = bufferRange.first.toFloat()..bufferRange.last.toFloat(),
            steps = (bufferRange.last - bufferRange.first),
            onValueChange = { bufferSize = it.roundToInt() },
            onValueChangeFinished = { onBufferSizeSelect(bufferSize) },
            track = { sliderState ->
                SliderDefaults.Track(
                    enabled = bufferRange.first != bufferRange.last,
                    sliderState = sliderState,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> }
                )
            }
        )

        HorizontalDivider()
        Text(stringResource(R.string.contingent_note))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                // user may not have interacted with the slider - need to assign a value
                onBufferSizeSelect(bufferSize)
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
            Text(stringResource(R.string.apply))
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeDropdown(
    attributeName: String,
    codedValue: CodedValue?,
    availableValues: List<CodedValue>,
    onNewValueSelect: (CodedValue) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        onExpandedChange = {
            if (availableValues.isNotEmpty()) {
                expanded = !expanded
            }
        },
        expanded = expanded
    ) {

        val textValue = codedValue?.name

        OutlinedTextField(
            enabled = availableValues.isNotEmpty(),
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
            availableValues.forEach { value ->
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
