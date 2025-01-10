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

package com.esri.arcgismaps.sample.createandeditgeometries.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorStyle
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateAndEditGeometriesViewModel(application: Application) : AndroidViewModel(application) {
    //TODO - delete mutable state when the map does not change or the screen does not need to observe changes
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // create a MapViewProxy that will be used to identify features in the MapView and set the viewpoint
    val mapViewProxy = MapViewProxy()

    // create a graphic, graphic overlay, and geometry editorenc
    private var identifiedGraphic = Graphic()
    val graphicsOverlay = GraphicsOverlay()
    // create a geometry editor
    val geometryEditor = GeometryEditor()

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

    /**
     * Starts the GeometryEditor using the selected [GeometryType].
     */
    fun startEditor(selectedGeometry: GeometryType) {
        if (!geometryEditor.isStarted.value) {
            geometryEditor.start(selectedGeometry)
        }
    }

    /**
     * Stops the GeometryEditor and updates the identified graphic or calls [createGraphic].
     */
    fun stopEditor() {
        if (identifiedGraphic.geometry != null) {
            identifiedGraphic.geometry = geometryEditor.stop()
            identifiedGraphic.isSelected = false
        } else if (geometryEditor.isStarted.value) {
            createGraphic()
        }
    }

    /**
     * Creates a graphic from the geometry and add it to the GraphicsOverlay.
     */
    private fun createGraphic() {
        val geometry = geometryEditor.stop()
            ?: return messageDialogVM.showMessageDialog(
                "Error!",
                "Error stopping editing session"
            )
        val graphic = Graphic(geometry)

        when (geometry) {
            is Point, is Multipoint -> graphic.symbol = GeometryEditorStyle().vertexSymbol
            is Polyline -> graphic.symbol = GeometryEditorStyle().lineSymbol
            is Polygon -> graphic.symbol = GeometryEditorStyle().fillSymbol
            else -> {}
        }
        graphicsOverlay.graphics.add(graphic)
        graphic.isSelected = false
    }

}
