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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.FloatSuppliersUiState
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.MAXIMUM_SLOPE_DEGREES

// A list of colors used to approximate the color gradient of the Viridis color scheme
private val viridis = listOf(
    Color(0xFF440154),
    Color(0xFF3B528B),
    Color(0xFF21918C),
    Color(0xFF5EC962),
    Color(0xFFFDE725)
)

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
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "Slope range: ${uiState.minimumSlope.toInt()} to ${uiState.maximumSlope.toInt()} degrees")
    }
    SlopeRangeSlider(uiState, onMinMaxSlopeChange)
}

/**
 * A range slider for selecting the minimum and maximum slope values. Uses a customized color
 * scheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlopeRangeSlider(
    uiState: FloatSuppliersUiState,
    onMinMaxSlopeChange: (Float, Float) -> Unit
) {
    val colors = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        viridis.reversed()
    } else {
        viridis
    }

    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val maxSlope = MAXIMUM_SLOPE_DEGREES.toFloat()

    RangeSlider(
        value = uiState.minimumSlope..uiState.maximumSlope,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = { newRange ->
            if (newRange.start < newRange.endInclusive) {
                onMinMaxSlopeChange(newRange.start, newRange.endInclusive)
            }
        },
        valueRange = 0.0f..maxSlope,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = {
            // Make the start thumb the same color as the point on the track where it is placed
            Box(
                Modifier
                    .size(20.dp)
                    .background(
                        color = colorAtFraction(
                            colors = viridis,
                            fraction = uiState.minimumSlope / maxSlope
                        ),
                        shape = CircleShape
                    )
                    .border(width = 1.dp, color = Color.Black, shape = CircleShape)
            )
        },
        endThumb = {
            // Make the end thumb the same color as the point on the track where it is placed
            Box(
                Modifier
                    .size(20.dp)
                    .background(
                        color = colorAtFraction(
                            colors = viridis,
                            fraction = uiState.maximumSlope / maxSlope
                        ),
                        shape = CircleShape
                    )
                    .border(width = 1.dp, color = Color.Black, shape = CircleShape)
            )
        },
        track = {
            // The track of the slider, using a horizontal gradient that matches the color ramp used
            // when the slope data is rendered
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(colors))
            )
        }
    )
}

/**
 * Returns a Color that is a linear interpolation between the given list of [colors], based on the
 * given [fraction].
 */
private fun colorAtFraction(colors: List<Color>, fraction: Float): Color {
    val position = fraction.coerceIn(0f, 1f) * (colors.size - 1)
    val index = position.toInt().coerceAtMost(colors.lastIndex - 1)
    val t = position - index

    val start = colors[index]
    val end = colors[index + 1]
    return lerp(start, end, t)
}
