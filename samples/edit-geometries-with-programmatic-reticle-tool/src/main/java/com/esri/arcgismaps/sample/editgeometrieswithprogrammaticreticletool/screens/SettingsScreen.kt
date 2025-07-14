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

package com.esri.arcgismaps.sample.editgeometrieswithprogrammaticreticletool.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog

@Composable
private fun GeometryTypePicker(currentGeometryType: String, onGeometryTypeSelected: (String) -> Unit) {
    val geometryTypes = listOf(
        "Point",
        "Multipoint",
        "Polyline",
        "Polygon"
    )
    DropDownMenuBox(
        textFieldLabel = "Starting Geometry Type",
        textFieldValue = currentGeometryType,
        dropDownItemList = geometryTypes
    ) { i ->
        onGeometryTypeSelected(geometryTypes[i])
    }
}

@Composable
fun SettingsScreen(
    isEditorStarted: Boolean,
    allowVertexCreation: Boolean,
    canDeleteSelectedElement: Boolean,
    currentGeometryType: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onGeometryTypeSelected: (String) -> Unit,
    onVertexCreationToggled: (Boolean) -> Unit,
    onUndoButtonPressed: () -> Unit,
    onRedoButtonPressed: () -> Unit,
    onDeleteButtonPressed: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Column {
            Text(text = "Settings", modifier = Modifier.fillMaxWidth().padding(2.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 20.sp)
            Row(modifier =  Modifier.fillMaxWidth()) {
                Text(text = "Allow Vertex Creation", modifier = Modifier.padding(horizontal = 3.dp).align(Alignment.CenterVertically), textAlign = TextAlign.Center)
                Column(Modifier.fillMaxWidth()) {
                    Switch(
                        checked = allowVertexCreation,
                        modifier = Modifier.padding(horizontal = 2.dp).align(Alignment.CenterHorizontally),
                        onCheckedChange = onVertexCreationToggled
                    )
                }
            }
            if (isEditorStarted) {
                // Show geometry editor action buttons.
                Button(onClick = onUndoButtonPressed, enabled = canUndo) {
                    Text(text = "Undo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Button(onClick = onRedoButtonPressed, enabled = canRedo) {
                    Text(text = "Redo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Button(onClick = onDeleteButtonPressed, enabled = canDeleteSelectedElement) {
                    Text(text = "Delete", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else {
                // Show geometry type selector.
                GeometryTypePicker(
                    currentGeometryType,
                    onGeometryTypeSelected = onGeometryTypeSelected
                )
            }
        }
    }
}
