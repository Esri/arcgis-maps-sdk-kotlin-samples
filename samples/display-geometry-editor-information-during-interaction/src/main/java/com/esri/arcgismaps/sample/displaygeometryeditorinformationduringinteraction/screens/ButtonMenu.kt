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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.displaygeometryeditorinformationduringinteraction.R

/**
 * Composable component to display the menu buttons.
 */
@Composable
fun ButtonMenu(
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val vector = ImageVector


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


    }
}
