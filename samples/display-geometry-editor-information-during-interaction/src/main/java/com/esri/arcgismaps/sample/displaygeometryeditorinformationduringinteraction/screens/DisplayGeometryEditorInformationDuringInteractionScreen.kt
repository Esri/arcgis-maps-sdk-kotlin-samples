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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.R
import com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.components.DisplayGeometryEditorInformationDuringInteractionViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplayGeometryEditorInformationDuringInteractionScreen(sampleName: String) {
    val mapViewModel: DisplayGeometryEditorInformationDuringInteractionViewModel = viewModel()
    val transformationInfo by mapViewModel.interactionTransformationMessage.collectAsStateWithLifecycle()
    val editorStarted by mapViewModel.geometryEditor.isStarted.collectAsStateWithLifecycle()
    val canUndo by mapViewModel.geometryEditor.canUndo.collectAsStateWithLifecycle()
    val canRedo by mapViewModel.geometryEditor.canRedo.collectAsStateWithLifecycle()
    val displayText = if (editorStarted) {
        transformationInfo ?: stringResource(R.string.transform_drag_handles)
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = mapViewModel::identify
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    text = displayText,
                    textAlign = TextAlign.Center
                )

                ButtonMenuOptions(
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

/**
 * Composable component to display the menu buttons.
 */
@Composable
fun ButtonMenuOptions(
    isGeometryEditorStarted: Boolean,
    canGeometryEditorUndo: Boolean,
    canGeometryEditorRedo: Boolean,
    onStopEditingButtonClick: () -> Unit,
    onDiscardEditsButtonClick: () -> Unit,
    onUndoButtonClick: () -> Unit,
    onRedoButtonClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(
            enabled = isGeometryEditorStarted,
            onClick = onStopEditingButtonClick
        ) { Icon(imageVector = Icons.Default.Check, contentDescription = "Save Edits") }

        IconButton(
            enabled = isGeometryEditorStarted,
            onClick = onDiscardEditsButtonClick
        ) { Icon(imageVector = Icons.Default.Clear, contentDescription = "Discard Edits") }

        IconButton(
            enabled = canGeometryEditorUndo,
            onClick = onUndoButtonClick
        ) { Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }

        IconButton(
            enabled = canGeometryEditorRedo,
            onClick = onRedoButtonClick
        ) { Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
    }
}
