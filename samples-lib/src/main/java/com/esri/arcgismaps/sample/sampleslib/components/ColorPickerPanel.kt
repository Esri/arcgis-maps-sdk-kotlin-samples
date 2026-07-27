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

package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.arcgismaps.Color as ArcGISColor

/**
 * A self-contained ARGB color picker panel using inline sliders.
 * This composable expands for slider configurations and collapses for [color] preview state.
 * Supports sRGB color space.
 */
@Composable
fun ColorPickerPanel(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    title: String = "Color Picker",
    onColorChange: (Color) -> Unit,
    supportsOpacity: Boolean = true,
) {
    val r = color.red * 255f
    val g = color.green * 255f
    val b = color.blue * 255f

    var isExpanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "colorSwatchArrowRotation"
    )

    Box(
        modifier = Modifier
            .animateContentSize()
            .clickable(onClick = { isExpanded = !isExpanded })
            .then(modifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    if (!isExpanded) {
                        ColorPickerSwatch(color = color)
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse color picker" else "Expand color picker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }
            if (isExpanded) {
                ColorPickerSwatch(color = color, size = 48.dp)
            }
            if (isExpanded) {
                // Use regular Material sliders for RGB channels while keeping live updates.
                LabeledSlider(
                    label = "RED",
                    value = r,
                    valueRange = 0f..255f,
                    valueText = r.roundToInt().toString(),
                    onValueChange = { newR -> onColorChange(color.copy(red = newR.roundToInt() / 255f)) }
                )

                LabeledSlider(
                    label = "GREEN",
                    value = g,
                    valueRange = 0f..255f,
                    valueText = g.roundToInt().toString(),
                    onValueChange = { newG -> onColorChange(color.copy(green = newG.roundToInt() / 255f)) }
                )

                LabeledSlider(
                    label = "BLUE",
                    value = b,
                    valueRange = 0f..255f,
                    valueText = b.roundToInt().toString(),
                    onValueChange = { newB -> onColorChange(color.copy(blue = newB.roundToInt() / 255f)) }
                )

                if (supportsOpacity) {
                    LabeledSlider(
                        label = "OPACITY",
                        value = color.alpha,
                        valueRange = 0f..1f,
                        valueText = "${(color.alpha * 100).roundToInt()}%",
                        onValueChange = { newAlpha -> onColorChange(color.copy(alpha = newAlpha)) }
                    )
                }
            }
        }
    }
}

/**
 * A slider that shows its current value beside the label.
 */
@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
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
 * Reusable circular swatch that previews a selected color next to picker controls.
 */
@Composable
private fun ColorPickerSwatch(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Converts a Compose Color object into an ArcGIS Maps SDK Color instance.
 *
 * Samples using [ColorPickerPanel] can use this conversion for ArcGIS Color values in
 * symbols, renderers, BackgroundGrid, and similar APIs.
 */
fun Color.toArcGISColor(): ArcGISColor {
    val argb = toArgb()
    return ArcGISColor.fromRgba(
        r = (argb shr 16) and 0xFF, // r
        g = (argb shr 8) and 0xFF,  // g
        b = argb and 0xFF,          // b
        a = (argb shr 24) and 0xFF  // a
    )
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ColorPickerPanelPreview() {
    var currentColor by remember { mutableStateOf(Color(0xFF00826C).copy(alpha = 0.65f)) }
    SamplePreviewSurface {
        Box(Modifier.fillMaxSize()) {
            ColorPickerPanel(
                modifier = Modifier.align(Alignment.Center),
                title = "Compose color picker",
                color = currentColor,
                onColorChange = { newColor -> currentColor = newColor }
            )
        }
    }
}