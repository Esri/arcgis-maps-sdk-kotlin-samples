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

package com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.R
import com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.components.DisplayGeometryEditorInformationDuringInteractionViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplayGeometryEditorInformationDuringInteractionScreen(sampleName: String) {
    val mapViewModel: DisplayGeometryEditorInformationDuringInteractionViewModel = viewModel()
    val transformationInfo by mapViewModel.interactionTransformationFlow.collectAsStateWithLifecycle()
    val editorStarted by mapViewModel.geometryEditor.isStarted.collectAsStateWithLifecycle()
    val canUndo by mapViewModel.geometryEditor.canUndo.collectAsStateWithLifecycle()
    val canRedo by mapViewModel.geometryEditor.canRedo.collectAsStateWithLifecycle()
    val displayText  = if (editorStarted) {
        transformationInfo?.let { text ->
            text
        } ?: stringResource(R.string.transform_drag_handles)
    } else {
        stringResource(R.string.tap_on_geometry_to_interact)
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = mapViewModel::identify
                )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = displayText,
                    textAlign = TextAlign.Center
                )

                ButtonMenu(
                    isGeometryEditorStarted = editorStarted,
                    canGeometryEditorUndo = canUndo,
                    canGeometryEditorRedo = canRedo,
                    onStopEditingButtonClick = mapViewModel::stopEditor,
                    onDiscardEditsButtonClick = mapViewModel::discardEdits,
                    onUndoButtonClick = mapViewModel::undoEdit,
                    onRedoButtonClick = mapViewModel::redoEdit
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
fun SampleOptions() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DropDownMenuBox(
            textFieldValue = "<selected-option>",
            textFieldLabel = "Select an option",
            dropDownItemList = emptyList(),
            onIndexSelected = { }
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun BottomSheetPreview() {
    SamplePreviewSurface {
        BottomSheet(
            isVisible = true,
            sheetTitle = "Bottom sheet options",
        ) {
            SampleOptions()
        }
    }
}
