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
import androidx.compose.material.icons.filled.Create
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
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryType
import com.esri.arcgismaps.sample.createandeditgeometries.R

/**
 * Composable component to display the menu buttons.
 */
@Composable
fun ButtonMenu(
    isGeometryEditorStarted: Boolean,
    onStartEditingButtonClick: (GeometryType) -> Unit,
    onStopEditingButtonClick: () -> Unit
) {
    val rowModifier = Modifier
        .padding(12.dp)
        .fillMaxWidth()

    Row(
        modifier = rowModifier
    ) {
        val vector = ImageVector
        var expanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
        ) {
            IconButton(
                enabled = !isGeometryEditorStarted,
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
                        onStartEditingButtonClick(GeometryType.Point)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Multipoint") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Multipoint)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polyline") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Polyline)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Polygon") },
                    onClick = {
                        onStartEditingButtonClick(GeometryType.Polygon)
                        expanded = false
                    }
                )
            }
        }
        IconButton(
            enabled = isGeometryEditorStarted,
            onClick = { onStopEditingButtonClick() }
        ) {
            Icon(vector.vectorResource(id = R.drawable.check_32), contentDescription = "Save Edits")
        }
    }
}
