/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.showrealisticlightandshadows.components

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.SpaceEffect
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.time.Instant

class ShowRealisticLightAndShadowsViewModel(application: Application) : AndroidViewModel(application) {

    val arcGISScene =
        ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // add base surface for elevation data
            val surface = Surface()
            surface.elevationSources.add(
                ArcGISTiledElevationSource(
                    "https://elevation3d.arcgis" +
                            ".com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
                )
            )
            baseSurface = surface
            val point = Point(
                -73.0815,
                -49.3272,
                4059.0,
                SpatialReference.wgs84()
            )
            initialViewpoint = Viewpoint(
                center = point,
                scale = 17000.0,
                camera = Camera(
                    locationPoint = point,
                    heading = 11.0,
                    pitch = 82.0,
                    roll = 0.0
                )
            )
        }


    val lightingOptionsState =
        LightingOptionsState(
            mutableStateOf(Instant.parse("2000-09-22T15:00:00Z")),
            mutableStateOf(LightingMode.LightAndShadows),
            mutableStateOf(Color(220, 220, 220, 255)),
            mutableStateOf(AtmosphereEffect.HorizonOnly),
            mutableStateOf(SpaceEffect.Stars)
        )

    //TODO - delete mutable state when the map does not change or the screen does not need to observe changes
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }
    }
}

/**
 * Represents various lighting options that can be used to configure a composable [SceneView]
 *
 * @property sunTime defines the position of the sun in the scene
 * @property sunLighting configures how light and shadows are displayed in the scene
 * @property ambientLightColor defines the color of the ambient light when the scene uses lighting
 * @property atmosphereEffect configures how the atmosphere in the scene is displayed
 * @property spaceEffect configures how outer space is displayed in the scene
 * @since 200.4.0
 */
data class LightingOptionsState(
    val sunTime: MutableState<Instant>,
    val sunLighting: MutableState<LightingMode>,
    val ambientLightColor: MutableState<Color>,
    val atmosphereEffect: MutableState<AtmosphereEffect>,
    val spaceEffect: MutableState<SpaceEffect>
)
