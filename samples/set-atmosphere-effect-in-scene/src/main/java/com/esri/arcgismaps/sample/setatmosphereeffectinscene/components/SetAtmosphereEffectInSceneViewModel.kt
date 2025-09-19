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

package com.esri.arcgismaps.sample.setatmosphereeffectinscene.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the sample demonstrating setting the atmosphere effect on a Scene.
 */
class SetAtmosphereEffectInSceneViewModel(app: Application) : AndroidViewModel(app) {

    // Create a scene with an imagery basemap & set an initial viewpoint.
    var arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        val camera = Camera(
            latitude = 64.416919,
            longitude = -14.483728,
            altitude = 0.0,
            heading = 318.0,
            pitch = 105.0,
            roll = 0.0
        )
        // Add an initial viewpoint using a camera.
        initialViewpoint = Viewpoint(
            boundingGeometry = camera.location,
            camera = camera
        )
        // Creates an elevation source and set it on the scene's base surface.
        baseSurface.elevationSources.add(
            ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
        )
    }

    // Keep track of the SceneView's atmosphere effect state.
    private val _atmosphereEffect = MutableStateFlow<AtmosphereEffect>(AtmosphereEffect.HorizonOnly)
    val atmosphereEffect = _atmosphereEffect.asStateFlow()

    // Message dialog view model for error handling.
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the scene and report load failures if they occur.
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Update the [AtmosphereEffect] applied to the SceneView.
     */
    fun updateAtmosphereEffect(newEffect: AtmosphereEffect) {
        _atmosphereEffect.value = newEffect
    }
}
