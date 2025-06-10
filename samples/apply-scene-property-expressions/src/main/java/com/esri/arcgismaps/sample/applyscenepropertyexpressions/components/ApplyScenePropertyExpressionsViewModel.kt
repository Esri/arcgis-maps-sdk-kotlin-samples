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

package com.esri.arcgismaps.sample.applyscenepropertyexpressions.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ApplyScenePropertyExpressionsViewModel(app: Application) : AndroidViewModel(app) {
    // The ArcGISScene shown in the SceneView
    var arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISImageryStandard).apply {
            // Set initial viewpoint with a camera looking at the cone
            val lookAtPoint = Point(x = 83.9, y = 28.4, z = 1000.0, spatialReference = SpatialReference.wgs84())
            initialViewpoint = Viewpoint(
                latitude = 28.4,
                longitude = 83.9,
                scale = 1e5,
                camera = Camera(locationPoint = lookAtPoint, heading = 0.0, pitch = 50.0, roll = 0.0)
            )
        }
    )

    // GraphicsOverlay with heading and pitch expressions
    val graphicsOverlay: GraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Relative
        // Set up renderer with heading and pitch expressions
        renderer = SimpleRenderer().apply {
            sceneProperties.headingExpression = "[HEADING]"
            sceneProperties.pitchExpression = "[PITCH]"
        }
    }

    // The cone graphic, with mutable heading and pitch attributes
    private val coneGraphic: Graphic

    // UI state for heading and pitch sliders
    var heading by mutableStateOf(180.0)
        private set
    var pitch by mutableStateOf(45.0)
        private set

    // State for showing/hiding the settings bottom sheet
    var isSettingsVisible by mutableStateOf(false)

    // SceneViewProxy for advanced operations (not strictly needed here, but provided for completeness)
    val sceneViewProxy = SceneViewProxy()

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Create the cone symbol
        val coneSymbol = SimpleMarkerSceneSymbol(
            style = SimpleMarkerSceneSymbolStyle.Cone,
            color = Color.red,
            height = 100.0,
            width = 100.0,
            depth = 100.0
        )
        // Create the cone graphic at a fixed location
        coneGraphic = Graphic(
            geometry = Point(x = 83.9, y = 28.42, z = 200.0, spatialReference = SpatialReference.wgs84()),
            symbol = coneSymbol
        )
        // Set initial heading and pitch attributes
        coneGraphic.attributes["HEADING"] = heading
        coneGraphic.attributes["PITCH"] = pitch
        graphicsOverlay.graphics.add(coneGraphic)

        // Load the scene and handle errors
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // Called when the user changes the heading slider
    fun updateHeading(newHeading: Double) {
        heading = newHeading
        coneGraphic.attributes["HEADING"] = heading
    }

    // Called when the user changes the pitch slider
    fun updatePitch(newPitch: Double) {
        pitch = newPitch
        coneGraphic.attributes["PITCH"] = pitch
    }

    // Show or hide the settings bottom sheet
//    fun setSettingsVisible(visible: Boolean) {
//        isSettingsVisible = visible
//    }
}
