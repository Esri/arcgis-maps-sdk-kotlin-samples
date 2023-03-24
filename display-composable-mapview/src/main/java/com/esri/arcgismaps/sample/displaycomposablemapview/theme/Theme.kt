package com.esri.arcgismaps.sample.displaycomposablemapview.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColorScheme(
    primary = colorPrimary,
    primaryContainer = colorPrimaryDark,
    secondary = colorAccent
)

private val LightColorPalette = lightColorScheme(
    primary = colorPrimary,
    primaryContainer = colorPrimaryDark,
    secondary = colorAccent
)

@Composable
fun KotlinExampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
