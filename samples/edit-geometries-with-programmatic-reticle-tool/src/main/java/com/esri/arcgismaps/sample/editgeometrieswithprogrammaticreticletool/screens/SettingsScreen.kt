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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SettingsScreen(currentGeometryType: String, onGeometryTypeSelected: (String) -> Unit, onDismissRequest: () -> Unit) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Column {
            GeometryTypePicker(currentGeometryType, onGeometryTypeSelected = onGeometryTypeSelected)
        }
    }
}