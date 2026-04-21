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

package com.esri.arcgismaps.sample.configuresceneenvironment.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.view.SceneEnvironment
import com.arcgismaps.mapping.view.SunLighting
import com.arcgismaps.mapping.view.VirtualLighting
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Holds UI state for the configure scene environment sample and applies user choices to
 * the ArcGIS [SceneEnvironment].
 */
class ConfigureSceneEnvironmentViewModel(app: Application) : AndroidViewModel(app) {

    // The scene displayed by the SceneView composable.
    val arcGISScene: ArcGISScene = ArcGISScene(
        item = PortalItem("https://www.arcgis.com/home/item.html?id=fcebd77958634ac3874bbc0e6b0677a4")
    )

    // The SceneEnvironment of the scene, used to apply changes to the environment based on user input.
    private val _sceneEnvironment: SceneEnvironment
        get() = arcGISScene.environment

    // Message dialog view model for error handling.
    val messageDialogVM = MessageDialogViewModel()

    // UI state variables that mirror the properties of SceneEnvironment.
    // These are used to update the UI when changes are made.

    var isAtmosphereEnabled by mutableStateOf(_sceneEnvironment.isAtmosphereEnabled)
        private set

    var areStarsEnabled by mutableStateOf(_sceneEnvironment.areStarsEnabled)
        private set

    var backgroundColor by mutableStateOf(_sceneEnvironment.backgroundColor)
        private set

    var lightingType by mutableStateOf(
        if (_sceneEnvironment.lighting is SunLighting) LightingType.SUN else LightingType.VIRTUAL
    )
        private set

    var areDirectShadowsEnabled by mutableStateOf(_sceneEnvironment.lighting.areDirectShadowsEnabled)
        private set

    var timeOfDaySeconds by mutableFloatStateOf(43_200f)
        private set

    // Allowed slider bounds for time-of-day.
    val timeOfDaySecondsMin = 0f // 12:00 AM
    val timeOfDaySecondsMax = 82_800f // 11:00 PM

    // Use the Santa Fe time zone so entered times match local scene time.
    // Santa Fe's time zone is America/Denver.
    private val _sceneTimeZone = ZoneId.of("America/Denver")

    // Anchor the selected time to one date so we can convert local clock time to an Instant.
    private val _sceneDate: LocalDate = LocalDate.now(_sceneTimeZone)

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /** Enables or disables atmospheric rendering. */
    fun updateAtmosphereEnabled(enabled: Boolean) {
        isAtmosphereEnabled = enabled
        _sceneEnvironment.isAtmosphereEnabled = enabled
    }

    /** Enables or disables stars. */
    fun updateStarsEnabled(enabled: Boolean) {
        areStarsEnabled = enabled
        _sceneEnvironment.areStarsEnabled = enabled
    }

    /**
     * Sets the scene background color.
     *
     * A solid background color is most visible when atmosphere and stars are disabled.
     */
    fun updateBackgroundColor(color: Color) {
        backgroundColor = color
        _sceneEnvironment.backgroundColor = color
        // Setting a background color should make atmosphere and stars off so the color is visible
        updateAtmosphereEnabled(false)
        updateStarsEnabled(false)
    }

    /** Switches between sun-based lighting and virtual lighting. */
    fun updateLightingType(newType: LightingType) {
        lightingType = newType
        when (newType) {
            LightingType.SUN -> {
                // Rebuild sun lighting with the currently selected scene time.
                val sunLighting = SunLighting(simulatedDate = instantFromSeconds(timeOfDaySeconds), areDirectShadowsEnabled = areDirectShadowsEnabled)
                _sceneEnvironment.lighting = sunLighting
            }

            LightingType.VIRTUAL -> {
                val virtualLighting = VirtualLighting(areDirectShadowsEnabled = areDirectShadowsEnabled)
                _sceneEnvironment.lighting = virtualLighting
            }
        }
    }

    /** Enables or disables direct shadows on the active lighting model. */
    fun updateDirectShadowsEnabled(enabled: Boolean) {
        areDirectShadowsEnabled = enabled
        _sceneEnvironment.lighting.areDirectShadowsEnabled = enabled
    }

    /** Updates scene local time using seconds since midnight. */
    fun updateTimeOfDaySeconds(seconds: Float) {
        timeOfDaySeconds = seconds
        val newInstant = instantFromSeconds(seconds)
        (_sceneEnvironment.lighting as? SunLighting)?.simulatedDate = newInstant
    }

    // Converts seconds since midnight into an Instant used by SunLighting.
    private fun instantFromSeconds(seconds: Float): Instant {
        return _sceneDate
            .atStartOfDay(_sceneTimeZone)
            .plusSeconds(seconds.toLong())
            .toInstant()
    }

}

/** Lighting options shown in the segmented control. */
enum class LightingType(val displayName: String) {
    SUN("Sun"),
    VIRTUAL("Virtual")
}
