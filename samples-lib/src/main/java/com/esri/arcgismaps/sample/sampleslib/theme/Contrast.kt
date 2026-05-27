package com.esri.arcgismaps.sample.sampleslib.theme

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun rememberSystemContrastLevel(context: Context): ContrastLevel {
    var contrastLevel by remember(context) {
        mutableStateOf(resolveContrastLevel(context))
    }

    DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                contrastLevel = resolveContrastLevel(context)
            }

            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                contrastLevel = resolveContrastLevel(context)
            }
        }

        highTextContrastSettings.forEach { key ->
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(key),
                false,
                observer
            )
        }

        val contrastListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            UiModeManager.ContrastChangeListener { contrast ->
                contrastLevel = contrast.toContrastLevel()
            }.also { listener ->
                uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
            }
        } else {
            null
        }

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val uiModeManager = context.getSystemService(UiModeManager::class.java)
                contrastListener?.let(uiModeManager::removeContrastChangeListener)
            }
        }
    }

    return contrastLevel
}


private val highTextContrastSettings = listOf(
    "high_text_contrast_enabled",
    "accessibility_high_text_contrast_enabled"
)

internal enum class ContrastLevel {
    Standard,
    Medium,
    High
}


private fun resolveContrastLevel(context: Context): ContrastLevel {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        uiModeManager.contrast.toContrastLevel()
    } else {
        if (isHighTextContrastEnabled(context)) ContrastLevel.High else ContrastLevel.Standard
    }
}

private fun isHighTextContrastEnabled(context: Context): Boolean {
    return highTextContrastSettings.any { key ->
        Settings.Secure.getInt(context.contentResolver, key, 0) == 1
    }
}

private fun Float.toContrastLevel(): ContrastLevel {
    return when {
        this >= 0.67f -> ContrastLevel.High
        this >= 0.33f -> ContrastLevel.Medium
        else -> ContrastLevel.Standard
    }
}
