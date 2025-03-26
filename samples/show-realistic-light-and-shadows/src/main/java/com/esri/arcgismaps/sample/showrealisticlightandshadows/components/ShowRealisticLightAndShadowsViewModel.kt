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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.LightingMode
import com.arcgismaps.mapping.view.SpaceEffect
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ShowRealisticLightAndShadowsViewModel(application: Application) :
    AndroidViewModel(application) {

    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        // add base surface for elevation data
        val surface = Surface()
        // create an elevation source from Terrain3D REST service
        surface.elevationSources.add(
            ArcGISTiledElevationSource(
                "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
            )
        )
        baseSurface = surface
        // create a scene layer from buildings REST service
        operationalLayers.add(
            ArcGISSceneLayer(
                "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/DevA_BuildingShells/SceneServer"
            )
        )
        // create a point to centre on
        val point = Point(
            x = -122.69033, y = 45.54605, z = 500.0, spatialReference = SpatialReference.wgs84()
        )
        initialViewpoint = Viewpoint(
            center = point, scale = 17000.0, camera = Camera(
                locationPoint = point, heading = 162.58544, pitch = 72.0, roll = 0.0
            )
        )
    }

    // noon in the timezone our center point is located with an arbitrary date
    private val noon: ZonedDateTime = LocalDateTime.parse("2000-09-22T12:00:00").atZone(
        ZoneId.of("US/Pacific"))

    // create a LightingOptionsState with default values that will be used by the scene view
    val lightingOptionsState = LightingOptionsState(
        mutableStateOf(
            noon.toInstant() // use noon as the default time
        ),
        mutableStateOf(LightingMode.LightAndShadows),
        mutableStateOf(Color(red = 220, green = 220, blue = 220, alpha = 255)),
        mutableStateOf(AtmosphereEffect.HorizonOnly),
        mutableStateOf(SpaceEffect.Stars)
    )

    // create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    /**
     * Update the sun time of the lighting options state used by the scene view.
     * Uses offset in seconds since noon, which is a range from -43200 (12 am) to 43140 seconds (11:59 pm).
     */
    fun setSunTime(sunTime: Int) {
        lightingOptionsState.sunTime.value = noon.plusSeconds(sunTime.toLong()).toInstant()
    }
}

/**
 * Represents various lighting options that can be used to configure a composable [SceneView].
 */
data class LightingOptionsState(
    val sunTime: MutableState<Instant>,
    val sunLighting: MutableState<LightingMode>,
    val ambientLightColor: MutableState<Color>,
    val atmosphereEffect: MutableState<AtmosphereEffect>,
    val spaceEffect: MutableState<SpaceEffect>
)
