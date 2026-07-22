package com.esri.arcgismaps.sample.sampleslib.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.arcgismaps.Color as ArcGISColor

/**
 * A self-contained ARGB color picker panel using inline sliders.
 */
@Composable
fun ColorPickerPanel(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    supportsOpacity: Boolean = true,
) {
    val r = (color.red * 255f).roundToInt()
    val g = (color.green * 255f).roundToInt()
    val b = (color.blue * 255f).roundToInt()

    Column(modifier = modifier) {

        // Use regular Material sliders for RGB channels while keeping live updates.
        LabeledSlider(
            label = "RED",
            value = r.toFloat(),
            valueRange = 0f..255f,
            valueText = r.toString(),
            onValueChange = { newR -> onColorChange(color.copy(red = newR.roundToInt() / 255f)) }
        )

        Spacer(Modifier.height(12.dp))

        LabeledSlider(
            label = "GREEN",
            value = g.toFloat(),
            valueRange = 0f..255f,
            valueText = g.toString(),
            onValueChange = { newG -> onColorChange(color.copy(green = newG.roundToInt() / 255f)) }
        )

        Spacer(Modifier.height(12.dp))

        LabeledSlider(
            label = "BLUE",
            value = b.toFloat(),
            valueRange = 0f..255f,
            valueText = b.toString(),
            onValueChange = { newB -> onColorChange(color.copy(blue = newB.roundToInt() / 255f)) }
        )

        if (supportsOpacity) {
            Spacer(Modifier.height(12.dp))
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

/**
 * A titled slider whose current value is shown alongside the label, formatted however the
 * caller needs (a plain integer for RGB channels, a percentage for opacity, etc.).
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
fun ColorPickerSwatch(
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
 * Converts a Compose [Color] to the ArcGIS Maps SDK's [ArcGISColor]. Public so any sample
 * using [ColorPickerPanel] to let the user choose a color for an ArcGIS API type (symbols,
 * renderers, BackgroundGrid, etc.) can reuse this instead of writing its own conversion -
 * ArcGIS SDK color properties are typically typed as com.arcgismaps.Color, not
 * androidx.compose.ui.graphics.Color.
 */
fun Color.toArcGISColor(): ArcGISColor {
    val argb = toArgb()
    return ArcGISColor.fromRgba(
        (argb shr 16) and 0xFF, // r
        (argb shr 8) and 0xFF,  // g
        argb and 0xFF,          // b
        (argb shr 24) and 0xFF  // a
    )
}


@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 360)
@Composable
private fun ColorPickerPanelPreview() {
    var color by remember { mutableStateOf(Color(0xFF00826C).copy(alpha = 0.65f)) }

    MaterialTheme {
        ColorPickerPanel(
            color = color,
            onColorChange = { color = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}