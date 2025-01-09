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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.data.CodedValue
import com.arcgismaps.geometry.Point
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.R
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.AddFeaturesWithContingentValuesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.addfeatureswithcontingentvalues.components.FeatureEditState
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlin.math.roundToInt

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeaturesWithContingentValuesScreen(sampleName: String) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // create a ViewModel to handle MapView interactions
    val mapViewModel: AddFeaturesWithContingentValuesViewModel = viewModel()

    // flows from view model for displaying state in UI
    val featureEditState by mapViewModel.featureEditState.collectAsStateWithLifecycle()

    // point on map tapped by user
    var selectedPoint by remember { mutableStateOf<Point?>(null) }

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
            mapViewModel.clearFeature()
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {

            Box {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    arcGISMap = mapViewModel.arcGISMap,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = { tapEvent ->
                        selectedPoint = tapEvent.mapPoint
                        showBottomSheet = true
                    }
                )

                if (showBottomSheet) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = bottomSheetState,
                        onDismissRequest = { showBottomSheet = false }
                    ) {
                        BottomSheetContents(
                            featureEditState = featureEditState,
                            onStatusAttributeSelected = mapViewModel::onStatusAttributeSelected,
                            onProtectionAttributeSelected = mapViewModel::onProtectionAttributeSelected,
                            onBufferSizeSelected = mapViewModel::onBufferSizeSelected,
                            onApplyButtonClicked = {
                                selectedPoint?.let { point ->
                                    mapViewModel.validateContingency(point)
                                    showBottomSheet = false
                                }
                            }
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
    featureEditState: FeatureEditState,
    onStatusAttributeSelected: (CodedValue) -> Unit,
    onProtectionAttributeSelected: (CodedValue) -> Unit,
    onBufferSizeSelected: (Int) -> Unit,
    onApplyButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.add_feature),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.attributes),
            style = MaterialTheme.typography.labelLarge
        )

        // dropdown boxes for selecting feature status/protection
        AttributeDropdown(
            attributeName = stringResource(R.string.status),
            codedValue = featureEditState.selectedStatusAttribute,
            availableValues = featureEditState.statusAttributes,
            onNewValueSelected = onStatusAttributeSelected
        )
        AttributeDropdown(
            attributeName = stringResource(R.string.protection),
            codedValue = featureEditState.selectedProtectionAttribute,
            availableValues = featureEditState.protectionAttributes,
            onNewValueSelected = onProtectionAttributeSelected)
        Spacer(Modifier.size(8.dp))

        // buffer size displayed and updated in slider
        var bufferSize by remember(key1 = featureEditState) { mutableIntStateOf(featureEditState.selectedBufferSize) }
        val bufferRange by remember (key1 = featureEditState ) { mutableStateOf(featureEditState.bufferRange) }

        // update the slider if contingent values change
        if (!bufferRange.contains(featureEditState.selectedBufferSize)) {
            onBufferSizeSelected((bufferRange.first + bufferRange.last) / 2)
        } else if (bufferRange.first == bufferRange.last) {
            onBufferSizeSelected(bufferRange.first)
        }

        Row {
            Text(
                text = stringResource(R.string.exclusion_area_buffer_size),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.weight(1f))
            Text(text = if (bufferSize > 0) bufferSize.toString() else "")
        }

        Slider(
            modifier = Modifier.padding(horizontal = 12.dp),
            enabled = bufferRange.first != bufferRange.last,
            value = bufferSize.toFloat(),
            valueRange = bufferRange.first.toFloat()..bufferRange.last.toFloat(),
            steps = (bufferRange.last - bufferRange.first),
            onValueChange = { bufferSize = it.roundToInt() },
            onValueChangeFinished = { onBufferSizeSelected(bufferSize) },
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
        Text(
            text = stringResource(R.string.contingent_note),
            style = MaterialTheme.typography.labelLarge
        )
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onApplyButtonClicked
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
    onNewValueSelected: (CodedValue) -> Unit
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
            label = { Text(attributeName) },
            readOnly = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableValues.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.name) },
                    onClick = {
                        expanded = false
                        onNewValueSelected(value)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SheetPreview() {
    SampleAppTheme {
        Surface {
            BottomSheetContents(
                featureEditState = FeatureEditState(),
                onStatusAttributeSelected = { },
                onProtectionAttributeSelected = { },
                onBufferSizeSelected = { },
                onApplyButtonClicked = { },
            )
        }
    }
}
