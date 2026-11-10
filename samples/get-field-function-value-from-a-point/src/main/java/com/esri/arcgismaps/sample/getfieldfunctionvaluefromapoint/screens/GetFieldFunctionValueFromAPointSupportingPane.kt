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

package com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.FieldSampleUiState

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun GetFieldFunctionValueFromAPointSupportingPane(
    uiState: FieldSampleUiState,
    onAircraftAltitudeChange: (Float) -> Unit
) {
    // TODO:
    //  This content is in a scrollable ColumnScope, so Composables can be added without the Column wrapper.
    //  Update below composables to reflect sample design UI state and invoke lambda callbacks for screen.

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Aircraft altitude (MSL): ")
        Text(text = uiState.aircraftAltitude.toInt().toString() + " m")
    }
    Slider(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.aircraftAltitude,
        onValueChange = onAircraftAltitudeChange,
        valueRange = 1500.0f..2595.0f
    )
}
