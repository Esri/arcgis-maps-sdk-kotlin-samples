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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.esri.arcgismaps.sample.createandeditgeometries.R
import com.esri.arcgismaps.sample.createandeditgeometries.components.CreateAndEditGeometriesViewModel

/**
 * Composable component to display the menu buttons.
 */
@Composable
fun ButtonMenu(
    isGeometryEditorStarted: Boolean,
    canGeometryEditorUndo: Boolean,
    canGeometryEditorRedo: Boolean,
    canDeleteSelectedGeometry: Boolean,
    onStartEditingButtonClick: (GeometryType) -> Unit,
    onStopEditingButtonClick: () -> Unit,
    onDiscardEditsButtonClick: () -> Unit,
    onDeleteSelectedElementButtonClick: () -> Unit,
    onDeleteAllGeometriesButtonClick: () -> Unit,
    onUndoButtonClick: () -> Unit,
    onRedoButtonClick: () -> Unit,
    onToolChange: (CreateAndEditGeometriesViewModel.ToolType) -> Unit,
    selectedTool: CreateAndEditGeometriesViewModel.ToolType,
    onScaleOptionChange: (CreateAndEditGeometriesViewModel.ScaleOption) -> Unit,
    selectedScaleOption: CreateAndEditGeometriesViewModel.ScaleOption,
    currentGeometryType: GeometryType
) {
    val rowModifier = Modifier
        .padding(12.dp)
        .fillMaxWidth()

    Row(
        modifier = rowModifier
    ) {
        val vector = ImageVector
        var drawMenuExpanded by remember { mutableStateOf(false) }
        var deleteMenuExpanded by remember { mutableStateOf(false) }
        var toolMenuExpanded by remember { mutableStateOf(false) }
        var scaleOptionsMenuExpanded by remember { mutableStateOf(false) }
        val canChangeTool =
            (currentGeometryType == GeometryType.Polyline || currentGeometryType == GeometryType.Polygon)
        Box {
            IconButton(
                enabled = !isGeometryEditorStarted,
                onClick = { drawMenuExpanded = !drawMenuExpanded }
            ) {
                Icon(imageVector = Icons.Default.Create, contentDescription = "Start")
            }
            DropdownMenu(
                expanded = drawMenuExpanded,
                onDismissRequest = { drawMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Point") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Point)
                        drawMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Multipoint") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Multipoint)
                        drawMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polyline") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Polyline)
                        drawMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polygon") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Polygon)
                        drawMenuExpanded = false
                    }
                )
            }
        }
        IconButton(
            enabled = canGeometryEditorUndo,
            onClick = { onUndoButtonClick() }
        ) {
            Icon(
                imageVector = vector.vectorResource(R.drawable.undo_24),
                contentDescription = "Undo"
            )
        }
        IconButton(
            enabled = canGeometryEditorRedo,
            onClick = { onRedoButtonClick() }
        ) {
            Icon(
                imageVector = vector.vectorResource(R.drawable.redo_24),
                contentDescription = "Redo"
            )
        }
        Box {
            IconButton(
                onClick = { deleteMenuExpanded = !deleteMenuExpanded }
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Geometry Menu")
            }
            DropdownMenu(
                expanded = deleteMenuExpanded,
                onDismissRequest = { deleteMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete Selected Element") },
                    enabled = canDeleteSelectedGeometry,
                    onClick = {
                        onDeleteSelectedElementButtonClick()
                        deleteMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    enabled = !isGeometryEditorStarted,
                    text = { Text("Delete All Geometries") },
                    onClick = {
                        onDeleteAllGeometriesButtonClick()
                        deleteMenuExpanded = false
                    }
                )
            }
        }
        Box {
            val toolTypeItems = remember { CreateAndEditGeometriesViewModel.ToolType.entries }
            IconButton(
                enabled = canChangeTool,
                onClick = { toolMenuExpanded = !toolMenuExpanded }
            ) {
                Icon(imageVector = Icons.Filled.Build, contentDescription = "Change Tool Type")
            }
            DropdownMenu(
                expanded = toolMenuExpanded,
                onDismissRequest = { toolMenuExpanded = false }
            ) {
                toolTypeItems.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onToolChange(it)
                            // dismiss the dropdown when any item is selected
                            toolMenuExpanded = false
                        },
                        text = {
                            Text(
                                text = it.name,
                                fontWeight =
                                if (selectedTool == it) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    )
                }
            }
        }
        Box {
            val scaleOptionItems = remember { CreateAndEditGeometriesViewModel.ScaleOption.entries }
            IconButton(
                onClick = { scaleOptionsMenuExpanded = !scaleOptionsMenuExpanded }
            ) {
                Icon(imageVector = vector.vectorResource(R.drawable.vertex_move_24), contentDescription = "Change Scale Options")
            }
            DropdownMenu(
                expanded = scaleOptionsMenuExpanded,
                onDismissRequest = { scaleOptionsMenuExpanded = false }
            ) {
                scaleOptionItems.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onScaleOptionChange(it)
                            // dismiss the dropdown when any item is selected
                            scaleOptionsMenuExpanded = false
                        },
                        text = {
                            Text(
                                text = it.name,
                                fontWeight =
                                if (selectedScaleOption == it) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    )
                }
            }
        }
        IconButton(
            enabled = isGeometryEditorStarted,
            onClick = { onStopEditingButtonClick() }
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Save Edits")
        }
        IconButton(
            enabled = isGeometryEditorStarted,
            onClick = { onDiscardEditsButtonClick() }
        ) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "Discard Edits")
        }
    }
}
