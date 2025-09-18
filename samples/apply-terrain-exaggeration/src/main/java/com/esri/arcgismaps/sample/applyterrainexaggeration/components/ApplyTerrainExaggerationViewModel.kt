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

package com.esri.arcgismaps.sample.applyterrainexaggeration.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that prepares a Scene with an elevation source and exposes
 * an elevation exaggeration value that the UI can update.
 */
class ApplyTerrainExaggerationViewModel(app: Application) : AndroidViewModel(app) {

    // Elevation source used by the scene's base surface.
    private val elevationSource: ArcGISTiledElevationSource = ArcGISTiledElevationSource(
        uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    )

    // Expose the elevation exaggeration as a MutableStateFlow for the UI to observe.
    private val _elevationExaggeration = MutableStateFlow(1f)
    val elevationExaggeration = _elevationExaggeration.asStateFlow()

    // Set the camera location to center on Levering, WA.
    private val camera = Camera(
        lookAtPoint = Point(
            x = -119.9489,
            y = 46.75792,
            spatialReference = SpatialReference.wgs84()
        ),
        distance = 15000.0,
        heading = 40.0,
        pitch = 60.0,
        roll = 0.0
    )

    // Create a scene with a topographic basemap and a surface that holds the elevation source.
    val arcGISScene: ArcGISScene = ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        baseSurface = Surface().apply {
            elevationSources.add(elevationSource)
            // Initial exaggeration value.
            elevationExaggeration = _elevationExaggeration.value
        }
        // Set the initial viewpoint using the camera.
        initialViewpoint = Viewpoint(camera = camera, boundingGeometry = camera.location)
    }

    // Dialog helper for showing errors.
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the scene and observe changes to the exaggeration flow to update the surface.
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }

        viewModelScope.launch {
            _elevationExaggeration.collect { value ->
                // Update the surface exaggeration whenever the flow changes.
                arcGISScene.baseSurface.elevationExaggeration = value
            }
        }
    }

    /**
     * Update exaggeration using an increment or decrement factor.
     * Value is expected between 1 and 10, clamped for the sample.
     */
    fun updateElevationExaggerationByFactor(factor: Float) {
        val newValue = _elevationExaggeration.value + factor
        val clamped = newValue.coerceIn(1f, 10f)
        _elevationExaggeration.value = clamped
    }
}
