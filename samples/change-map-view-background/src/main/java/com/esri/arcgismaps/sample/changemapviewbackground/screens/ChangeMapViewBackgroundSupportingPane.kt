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

package com.esri.arcgismaps.sample.changemapviewbackground.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundUiState
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundViewModel
import com.esri.arcgismaps.sample.sampleslib.components.ColorPickerPanel

/**
 * Supporting pane content for the sample. Lets the user configure the MapView's
 * background grid: its fill color, line color, line width, and grid square size.
 * Styled as a single grouped card under a small section label.
 */
@Composable
internal fun ChangeMapViewBackgroundSupportingPane(
    adaptiveUiState: ChangeMapViewBackgroundUiState,
    onColorChange: (Color) -> Unit,
    onLineColorChange: (Color) -> Unit,
    onLineWidthChange: (Float) -> Unit,
    onSizeChange: (Float) -> Unit
) {
    // This content is in a scrollable ColumnScope, so Composables can be added without
    // the Column wrapper.

    Text(
        text = "BACKGROUND GRID",
        style = MaterialTheme.typography.labelMedium
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 12.dp
    ) {
        Column {
            ColorPickerPanel(
                title = "Color",
                color = adaptiveUiState.color,
                onColorChange = onColorChange,
                supportsOpacity = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            ColorPickerPanel(
                title = "Line Color",
                color = adaptiveUiState.lineColor,
                onColorChange = onLineColorChange,
                supportsOpacity = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider()

            SliderRow(
                title = "Line Width",
                value = adaptiveUiState.lineWidth,
                valueRange = ChangeMapViewBackgroundViewModel.lineWidthRange,
                onValueChange = onLineWidthChange
            )

            HorizontalDivider()

            SliderRow(
                title = "Grid Size",
                value = adaptiveUiState.size,
                valueRange = ChangeMapViewBackgroundViewModel.sizeRange,
                onValueChange = onSizeChange
            )
        }
    }
}

/**
 * A titled row with a slider for adjusting a numeric value. The label and
 * current value sit on one line, with the slider directly beneath.
 */
@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

