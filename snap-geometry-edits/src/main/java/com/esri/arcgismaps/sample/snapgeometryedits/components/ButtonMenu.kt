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

package com.esri.arcgismaps.sample.snapgeometryedits.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.esri.arcgismaps.sample.snapgeometryedits.R

/**
 * Composable component to display the menu buttons.
 */
@Composable
fun ButtonMenu(
    mapViewModel: MapViewModel
) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
        ) {
            IconButton(
                enabled = mapViewModel.isCreateButtonEnabled.value,
                onClick = { expanded = !expanded }
            ) {
                Icon(imageVector = Icons.Default.Create, contentDescription = "Start")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Point") },
                    onClick = {
                        mapViewModel.startEditor(GeometryType.Point)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Multipoint") },
                    onClick = {
                        mapViewModel.startEditor(GeometryType.Multipoint)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polyline") },
                    onClick = {
                        mapViewModel.startEditor(GeometryType.Polyline)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polygon") },
                    onClick = {
                        mapViewModel.startEditor(GeometryType.Polygon)
                        expanded = false
                    }
                )
            }
        }
        val vector = ImageVector
        IconButton(
            enabled = mapViewModel.isUndoButtonEnabled.collectAsState().value,
            onClick = { mapViewModel.editorUndo() }
        ) {
            Icon(vector.vectorResource(R.drawable.undo), contentDescription = "Undo")
        }
        IconButton(
            enabled = mapViewModel.isSaveButtonEnabled.value,
            onClick = { mapViewModel.stopEditor() }
        ) {
            Icon(vector.vectorResource(R.drawable.save), contentDescription = "Save")
        }
        IconButton(
            enabled = mapViewModel.isDeleteButtonEnabled.value,
            onClick = { mapViewModel.deleteSelection() }
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                enabled = mapViewModel.isSnapSettingsButtonEnabled.value,
                onClick = { mapViewModel.showBottomSheet() }
            ) { Text(text = "Snap Settings") }
        }
    }
}
