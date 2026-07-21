package com.esri.arcgismaps.sample.sampleslib.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A self-contained ARGB color picker panel using sliders + hex input:
 * - Each RGB slider's track is a dynamic: it shows what the color would look
 *   like at that channel's min vs. max value, holding the other two channels
 *   at their current values — so the tracks update  live as you drag other
 *   sliders, rather than using a fixed hue.
 */
@Composable
fun ColorPickerPanel(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Color Picker",
    supportsOpacity: Boolean = true,
    showContainer: Boolean = true,
    showHeader: Boolean = true,
    showHexValue: Boolean = true,
    showPreview: Boolean = true,
    onClose: (() -> Unit)? = null,
) {
    val r = (color.red * 255f).roundToInt()
    val g = (color.green * 255f).roundToInt()
    val b = (color.blue * 255f).roundToInt()

    var hexText by remember(color) { mutableStateOf(colorToHex(color)) }

    Column(
        modifier = modifier
            .then(
                if (showContainer) {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF7F7F7))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                } else {
                    Modifier
                }
            )
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Each track's gradient is derived from the current color, varying only
        // the channel that slider controls — so it updates live as other sliders move.
        RgbChannelSlider(
            label = "RED",
            value = r,
            trackColors = listOf(
                color.copy(red = 0f, alpha = 1f),
                color.copy(red = 1f, alpha = 1f)
            ),
            onValueChange = { newR -> onColorChange(color.copy(red = newR / 255f)) }
        )

        Spacer(Modifier.height(12.dp))

        RgbChannelSlider(
            label = "GREEN",
            value = g,
            trackColors = listOf(
                color.copy(green = 0f, alpha = 1f),
                color.copy(green = 1f, alpha = 1f)
            ),
            onValueChange = { newG -> onColorChange(color.copy(green = newG / 255f)) }
        )

        Spacer(Modifier.height(12.dp))

        RgbChannelSlider(
            label = "BLUE",
            value = b,
            trackColors = listOf(
                color.copy(blue = 0f, alpha = 1f),
                color.copy(blue = 1f, alpha = 1f)
            ),
            onValueChange = { newB -> onColorChange(color.copy(blue = newB / 255f)) }
        )

        if (showHexValue) {
            Spacer(Modifier.height(14.dp))

            // Read-only hex value so sliders remain the only interactive controls.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "sRGB Hex Color #",
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = hexText,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.widthIn(min = 72.dp)
                )
            }
        }

        if (supportsOpacity) {
            Spacer(Modifier.height(16.dp))
            Text(
                "OPACITY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            OpacitySlider(
                value = color.alpha,
                baseColor = color.copy(alpha = 1f),
                onValueChange = { newAlpha -> onColorChange(color.copy(alpha = newAlpha)) }
            )
        }

        if (showPreview) {
            Spacer(Modifier.height(14.dp))

            // Preview swatch: bottom-left triangle shows the solid color,
            // top-right triangle shows it at the current opacity.
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                DiagonalSwatch(
                    solidColor = color.copy(alpha = 1f),
                    translucentColor = color,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/*
 * A single R/G/B channel slider: label, gradient track, thumb, and an
 * editable numeric value box (0-255) on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RgbChannelSlider(
    label: String,
    value: Int,
    trackColors: List<Color>,
    onValueChange: (Int) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Text(
        label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        ) {
            //gradient track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(trackColors))
            )
            //transparent material slider on top, driving drag + a custom round thumb
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                thumb = { CircularThumb() },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = textValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.widthIn(min = 26.dp)
        )
    }
}

// opacity slider with a checkerboard track fading into the current color.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpacitySlider(
    value: Float,
    baseColor: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
            ) {
                Checkerboard(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(baseColor.copy(alpha = 0f), baseColor.copy(alpha = 1f))
                            )
                        )
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                thumb = { CircularThumb() },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(76.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Transparent)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${(value * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// A plain round knob, replacing Material's default pill-shaped slider thumb.
@Composable
private fun CircularThumb() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(color = MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
    )
}

@Composable
private fun Checkerboard(modifier: Modifier = Modifier, cell: Float = 8f) {
    Canvas(modifier = modifier) {
        val light = Color.White
        val dark = Color(0xFFBBBBBB)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = row
            while (x < size.width) {
                drawRect(
                    color = if (col % 2 == 0) light else dark,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell)
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

@Composable
private fun DiagonalSwatch(
    solidColor: Color,
    translucentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // Bottom-left triangle: solid color
        val path1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width, size.height)
            close()
        }
        clipPath(path1) { drawRect(color = solidColor) }

        // Top-right triangle: color at current opacity, over a white backdrop
        val path2 = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            close()
        }
        clipPath(path2) {
            drawRect(color = Color.White)
            drawRect(color = translucentColor)
        }
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return String.format("%02X%02X%02X", r, g, b)
}


@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A, widthDp = 360)
@Composable
private fun ColorPickerPanelPreview() {
    var color by remember { mutableStateOf(Color(0xFF00826C).copy(alpha = 0.65f)) }

    MaterialTheme {
        ColorPickerPanel(
            color = color,
            onColorChange = { color = it },
            onClose = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

