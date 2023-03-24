package com.esri.arcgismaps.sample.displaycomposablemapview.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColorPalette else LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

private val colorPrimary = Color(0xFF9243cf)
private val colorPrimaryDark = Color(0xFF7a2ab7)
private val colorAccent = Color(0xFF7a2ab7)

private val DarkColorPalette = darkColors(
    primary = colorPrimary, primaryVariant = colorPrimaryDark, secondary = colorAccent
)

private val LightColorPalette = lightColors(
    primary = colorPrimary, primaryVariant = colorPrimaryDark, secondary = colorAccent
)

// Set of Material typography styles to start with
private val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp
    )
)
