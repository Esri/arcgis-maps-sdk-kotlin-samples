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

package com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.ClipFunctionUiState
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.MAXIMUM_ELEVATION_METERS
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.MINIMUM_ELEVATION_METERS

/**
 * Supporting pane content for the sample.
 */
@Composable
internal fun ClipAFieldFunctionToMapViewExtentSupportingPane(
    uiState: ClipFunctionUiState,
    displayProgressIndicator: Boolean,
    onMinMaxElevationChange: (Float, Float) -> Unit
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
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text =
            "Elevation range: ${uiState.minimumElevation.toInt()} to ${uiState.maximumElevation.toInt()} m"
        )
    }
    RangeSlider(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.minimumElevation..uiState.maximumElevation,
        onValueChange = { newRange ->
            if (newRange.start < newRange.endInclusive) {
                onMinMaxElevationChange(newRange.start, newRange.endInclusive)
            }
        },
        valueRange = MINIMUM_ELEVATION_METERS..MAXIMUM_ELEVATION_METERS
    )
    RasterDataCopyrightText()
}

/**
 * Displays copyright text for the sample's source raster.
 */
@Composable
private fun RasterDataCopyrightText() {
    Text(
        text = "Raster data copyright Scottish Government and SEPA (2014)",
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
