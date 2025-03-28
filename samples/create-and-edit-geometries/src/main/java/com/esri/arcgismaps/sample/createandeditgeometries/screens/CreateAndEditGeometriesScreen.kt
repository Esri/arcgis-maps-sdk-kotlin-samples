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

package com.esri.arcgismaps.sample.createandeditgeometries.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.createandeditgeometries.components.CreateAndEditGeometriesViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun CreateAndEditGeometriesScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: CreateAndEditGeometriesViewModel = viewModel()
    val selectedElement = mapViewModel.geometryEditor.selectedElement.collectAsStateWithLifecycle().value
    val canDeleteSelectedElement =
        when (selectedElement) {
            null -> false
            else -> selectedElement.canDelete
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
                    onSingleTapConfirmed = mapViewModel::identify,
                )
                ButtonMenu(
                    isGeometryEditorStarted = mapViewModel.geometryEditor.isStarted.collectAsStateWithLifecycle().value,
                    canGeometryEditorUndo = mapViewModel.geometryEditor.canUndo.collectAsStateWithLifecycle().value,
                    canGeometryEditorRedo = mapViewModel.geometryEditor.canRedo.collectAsStateWithLifecycle().value,
                    canDeleteSelectedElement = canDeleteSelectedElement,
                    onStartEditingButtonClick = mapViewModel::startEditor,
                    onStopEditingButtonClick = mapViewModel::stopEditor,
                    onDiscardEditsButtonClick = mapViewModel::discardEdits,
                    onDeleteSelectedElementButtonClick = mapViewModel::deleteSelectedElement,
                    onDeleteAllGeometriesButtonClick = mapViewModel::deleteAllGeometries,
                    onUndoButtonClick = mapViewModel::undoEdit,
                    onRedoButtonClick = mapViewModel::redoEdit,
                    onToolChange = mapViewModel::changeTool,
                    selectedTool = mapViewModel.selectedTool.collectAsStateWithLifecycle().value,
                    onScaleOptionChange = mapViewModel::changeScaleOption,
                    selectedScaleOption = mapViewModel.currentScaleOption.collectAsStateWithLifecycle().value,
                    currentGeometryType = mapViewModel.currentGeometryType.collectAsStateWithLifecycle().value
                )
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
        }
    )
}
