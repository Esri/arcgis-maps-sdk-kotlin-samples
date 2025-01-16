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

package com.esri.arcgismaps.sample.getelevationatpointonsurface.components

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class GetElevationAtPointOnSurfaceViewModel(application: Application) : AndroidViewModel(application) {

    val sceneViewProxy = SceneViewProxy()

    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Add an elevation source to the scene's base surface.
        baseSurface.elevationSources.add(
            ArcGISTiledElevationSource(
                "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
            )
        )
    }

    // Graphics overlay used to add feature graphics to the map
    val graphicsOverlay = GraphicsOverlay()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onSuccess {
                // Set the initial viewpoint of the scene view, once loaded
                sceneViewProxy.setViewpointCamera(
                    Camera(
                        latitude = 28.42, longitude = 83.9, altitude = 10000.0, heading = 10.0, pitch = 80.0, roll = 0.0
                    )
                )
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map", error.message.toString()
                )
            }
        }
    }

    /**
     * Get the elevation at the point on the surface where the user taps.
     */
    fun getElevation(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            sceneViewProxy.screenToLocation(singleTapConfirmedEvent.screenCoordinate).onSuccess { scenePoint ->
                arcGISScene.baseSurface.getElevation(scenePoint).onSuccess { elevation ->
                    // Create a point symbol to mark where elevation is being measured
                    val circleSymbol = SimpleMarkerSymbol(
                        style = SimpleMarkerSymbolStyle.Circle, color = com.arcgismaps.Color.red, size = 10f
                    )
                    // Add the graphic to the graphics overlay
                    graphicsOverlay.graphics.add(Graphic(geometry = scenePoint, symbol = circleSymbol))
                    // Show a toast message with the elevation
                    Toast.makeText(
                        getApplication(), "Elevation at point is ${elevation.roundToInt()} meters", Toast.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    messageDialogVM.showMessageDialog(
                        "Failed to get elevation", error.message.toString()
                    )
                }
            }
        }
    }
}
