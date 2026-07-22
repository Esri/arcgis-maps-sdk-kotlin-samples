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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundUiState
import com.esri.arcgismaps.sample.changemapviewbackground.components.ChangeMapViewBackgroundViewModel
import com.esri.arcgismaps.sample.sampleslib.components.ColorPickerPanel
import com.esri.arcgismaps.sample.sampleslib.components.ColorPickerSwatch

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

    var activePicker by remember { mutableStateOf<ColorPickerTarget?>(null) }

    Text(
        text = "BACKGROUND GRID",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column {
            ColorSwatchRow(
                title = "Color",
                color = adaptiveUiState.color,
                isExpanded = activePicker == ColorPickerTarget.Color,
                onClick = {
                    activePicker =
                        if (activePicker == ColorPickerTarget.Color) null else ColorPickerTarget.Color
                }
            )

            AnimatedVisibility(visible = activePicker == ColorPickerTarget.Color) {
                ColorPickerPanel(
                    color = adaptiveUiState.color,
                    onColorChange = onColorChange,
                    supportsOpacity = false,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                )
            }

            RowDivider()

            ColorSwatchRow(
                title = "Line Color",
                color = adaptiveUiState.lineColor,
                isExpanded = activePicker == ColorPickerTarget.LineColor,
                onClick = {
                    activePicker =
                        if (activePicker == ColorPickerTarget.LineColor) null else ColorPickerTarget.LineColor
                }
            )

            AnimatedVisibility(visible = activePicker == ColorPickerTarget.LineColor) {
                ColorPickerPanel(
                    color = adaptiveUiState.lineColor,
                    onColorChange = onLineColorChange,
                    supportsOpacity = true,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                )
            }

            RowDivider()

            SliderRow(
                title = "Line Width",
                value = adaptiveUiState.lineWidth,
                valueRange = ChangeMapViewBackgroundViewModel.lineWidthRange,
                onValueChange = onLineWidthChange
            )

            RowDivider()

            SliderRow(
                title = "Grid Size",
                value = adaptiveUiState.size,
                valueRange = ChangeMapViewBackgroundViewModel.sizeRange,
                onValueChange = onSizeChange
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

/**
 * A titled row showing the current color as a tappable swatch, plus a chevron
 * that rotates 180 degrees when [isExpanded] is true. Tapping the row invokes
 * [onClick], which the caller uses to expand/collapse the inline color picker.
 */
@Composable
private fun ColorSwatchRow(
    title: String,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "colorSwatchArrowRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorPickerSwatch(color = color)
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowRotation)
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

/**
 * Which color control is currently showing its picker, if any.
 */
private enum class ColorPickerTarget {
    Color,
    LineColor
}