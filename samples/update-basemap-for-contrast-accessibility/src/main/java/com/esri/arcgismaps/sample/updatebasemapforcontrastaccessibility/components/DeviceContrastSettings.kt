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

package com.esri.arcgismaps.sample.updatebasemapforcontrastaccessibility.components

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Device keys used by Android versions that expose high contrast through accessibility settings.
 */
private val highTextContrastSettings = listOf(
    "high_text_contrast_enabled",
    "accessibility_high_text_contrast_enabled"
)

/**
 * Snapshot of the device appearance settings that influence automatic contrast selection.
 *
 * The sample uses these values to resolve one of four contrast-specific basemaps
 * without changing the surrounding sample app theme.
 */
data class DeviceContrastSettings(
    val isDarkTheme: Boolean,
    val isHighContrastEnabled: Boolean
)

/**
 * Remembers the current device appearance settings and updates
 * when the device theme or accessibility contrast preferences change.
 *
 * On Android 14 and later, contrast changes come from [UiModeManager].
 * On earlier versions, fall back to the secure high-text-contrast settings from Android accessibility.
 */
@Composable
fun rememberDeviceContrastSettings(): DeviceContrastSettings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var settings by remember(context) {
        mutableStateOf(currentDeviceContrastSettings(context))
    }

    LaunchedEffect(context, configuration) {
        settings = currentDeviceContrastSettings(context)
    }

    DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                settings = currentDeviceContrastSettings(context)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                settings = currentDeviceContrastSettings(context)
            }
        }

        val uiModeUri = Settings.Secure.getUriFor("ui_night_mode")
        context.contentResolver.registerContentObserver(
            /* uri = */ uiModeUri,
            /* notifyForDescendants = */ false,
            /* observer = */ observer
        )
        highTextContrastSettings.forEach { key ->
            context.contentResolver.registerContentObserver(
                /* uri = */ Settings.Secure.getUriFor(key),
                /* notifyForDescendants = */ false,
                /* observer = */ observer
            )
        }

        val contrastListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            UiModeManager.ContrastChangeListener {
                settings = currentDeviceContrastSettings(context)
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

    return settings
}

/**
 * Returns the current theme and contrast preferences from the device.
 */
private fun currentDeviceContrastSettings(context: Context): DeviceContrastSettings {
    return DeviceContrastSettings(
        isDarkTheme = isDarkThemeEnabled(context),
        isHighContrastEnabled = isHighContrastEnabled(context)
    )
}

/**
 * Returns `true` when the current configuration resolves to night mode.
 */
private fun isDarkThemeEnabled(context: Context): Boolean {
    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiMode == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Resolves the active high-contrast preference using the API based on Android version.
 */
private fun isHighContrastEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        uiModeManager.contrast > 0f
    } else {
        highTextContrastSettings.any { key ->
            Settings.Secure.getInt(context.contentResolver, key, 0) == 1
        }
    }
}
