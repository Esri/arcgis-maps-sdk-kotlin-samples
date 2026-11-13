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

package com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.FloatSuppliersUiState
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.MAXIMUM_SLOPE_DEGREES

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun ControlMapAlgebraInputsWithFloatSuppliersSupportingPane(
    uiState: FloatSuppliersUiState,
    displayProgressIndicator: Boolean,
    onMinMaxSlopeChange: (Float, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        val message = when (displayProgressIndicator) {
            false -> "Analysis up to date"
            true -> "Analysis updating..."
        }
        Text(text = message)
        if (displayProgressIndicator) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween //TODO Center?
    ) {
        Text(text = "Slope range: ${uiState.minimumSlope.toInt()} to ${uiState.maximumSlope.toInt()} degrees")
    }
    //TODO: change to fancy custom slider, if time?
    RangeSlider(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.minimumSlope..uiState.maximumSlope,
        onValueChange = { newRange ->
            if (newRange.start < newRange.endInclusive) {
                onMinMaxSlopeChange(newRange.start, newRange.endInclusive)
            }
        },
        valueRange = 0.0f..MAXIMUM_SLOPE_DEGREES.toFloat()
    )
}
