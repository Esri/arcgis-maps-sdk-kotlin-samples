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
import androidx.compose.ui.unit.dp
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
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.FreehandTool
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorStyle
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorTool
import com.arcgismaps.mapping.view.geometryeditor.ReticleVertexTool
import com.arcgismaps.mapping.view.geometryeditor.ShapeTool
import com.arcgismaps.mapping.view.geometryeditor.ShapeToolType
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateAndEditGeometriesViewModel(application: Application) : AndroidViewModel(application) {
    // create a map with the imagery basemap style
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            // a viewpoint centered at the island of Inis Meáin (Aran Islands) in Ireland
            initialViewpoint = Viewpoint(
                latitude = 53.08230,
                longitude = -9.5920,
                scale = 5000.0
            )
        }
    )

    // create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // create a MapViewProxy that will be used to identify features in the MapView and set the viewpoint
    val mapViewProxy = MapViewProxy()

    // create a geometryEditorStyle
    private val geometryEditorStyle = GeometryEditorStyle()
    // create a graphic to hold graphics identified on tap
    private var identifiedGraphic = Graphic()
    // create a graphics overlay
    val graphicsOverlay = GraphicsOverlay()
    // create a geometry editor
    val geometryEditor = GeometryEditor()

    /**
     * Enum of GeometryEditorTool types
     */
    enum class ToolType {
        Vertex,
        ReticleVertex,
        Freehand,
        Arrow,
        Ellipse,
        Rectangle,
        Triangle
    }

    private val vertexTool = geometryEditor.tool // Use the default tool, which is vertex
    private val reticleVertexTool = ReticleVertexTool()
    private val freehandTool = FreehandTool()
    private val arrowTool = ShapeTool(ShapeToolType.Arrow)
    private val ellipseTool = ShapeTool(ShapeToolType.Ellipse)
    private val rectangleTool = ShapeTool(ShapeToolType.Rectangle)
    private val triangleTool = ShapeTool(ShapeToolType.Triangle)

    private val _selectedTool = MutableStateFlow<ToolType>(ToolType.Vertex)
    val selectedTool = _selectedTool.asStateFlow()

    private val _currentGeometryType = MutableStateFlow<GeometryType>(GeometryType.Unknown)
    val currentGeometryType = _currentGeometryType.asStateFlow()

    init {
        viewModelScope.launch {
            // load the map
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
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
            _currentGeometryType.value = selectedGeometry
            if (selectedGeometry == GeometryType.Point || selectedGeometry == GeometryType.Multipoint) {
                // default to vertex tool for point or multipoint
                changeTool(ToolType.Vertex)
            }
        }
    }

    /**
     * Stops the GeometryEditor and updates the identified graphic or calls [createGraphic].
     */
    fun stopEditor() {
        // check if there was a previously identified graphic
        if (identifiedGraphic.geometry != null) {
            // update the identified graphic
            identifiedGraphic.geometry = geometryEditor.stop()
            // deselect the identified graphic
            identifiedGraphic.isSelected = false
        } else if (geometryEditor.isStarted.value) {
            // create a graphic from the geometry that was being edited
            createGraphic()
        }
        _currentGeometryType.value = GeometryType.Unknown
    }

    /**
     * Undoes all edits made using the GeometryEditor then calls [stopEditor].
     */
    fun discardEdits() {
        while (geometryEditor.canUndo.value) {
            geometryEditor.undo()
        }
        stopEditor()
    }

    /**
     * Deletes the selected element.
     */
    fun deleteSelectedElement() {
        if (geometryEditor.selectedElement.value != null) {
            geometryEditor.deleteSelectedElement()
        }
    }

    /**
     * Deletes all the geometries on the map.
     */
    fun deleteAllGeometries() {
        graphicsOverlay.graphics.clear()
    }

    /**
     * Undoes the last event on the geometry editor.
     */
    fun undoEdit() {
        if (geometryEditor.canUndo.value) {
            geometryEditor.undo()
        }
    }

    /**
     * Redoes the last event on the geometry editor.
     */
    fun redoEdit() {
        if (geometryEditor.canRedo.value) {
            geometryEditor.redo()
        }
    }

    /**
     * Changes the tool type of the geometry editor to the specified tool.
     */
    fun changeTool(toolType: ToolType) {
        when (toolType) {
            ToolType.Vertex -> {
                geometryEditor.tool = vertexTool
            }

            ToolType.ReticleVertex -> {
                geometryEditor.tool = reticleVertexTool
            }

            ToolType.Freehand -> {
                geometryEditor.tool = freehandTool
            }

            ToolType.Arrow -> {
                geometryEditor.tool = arrowTool
            }

            ToolType.Ellipse -> {
                geometryEditor.tool = ellipseTool
            }

            ToolType.Rectangle -> {
                geometryEditor.tool = rectangleTool
            }

            ToolType.Triangle -> {
                geometryEditor.tool = triangleTool
            }
        }
        _selectedTool.value = toolType
    }

    /**
     * Creates a graphic from the geometry and adds it to the GraphicsOverlay.
     */
    private fun createGraphic() {
        // stop the geometry editor and get its final geometry state
        val geometry = geometryEditor.stop()
            ?: return messageDialogVM.showMessageDialog(
                title = "Error!",
                description = "Error stopping editing session"
            )

        // create a graphic to represent the new geometry
        val graphic = Graphic(geometry)

        // give the graphic an appropriate fill based on the geometry type
        when (geometry) {
            is Point, is Multipoint -> graphic.symbol = geometryEditorStyle.vertexSymbol
            is Polyline -> graphic.symbol = geometryEditorStyle.lineSymbol
            is Polygon -> graphic.symbol = geometryEditorStyle.fillSymbol
            else -> {}
        }
        // add the graphic to the graphics overlay
        graphicsOverlay.graphics.add(graphic)
        // deselect the graphic
        graphic.isSelected = false
    }

    /**
     * Identifies the graphic at the tapped screen coordinate in the provided [singleTapConfirmedEvent]
     * and starts the GeometryEditor using the identified graphic's geometry. Hide the BottomSheet on
     * [singleTapConfirmedEvent].
     */
    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            // attempt to identify a graphic at the location the user tapped
            val graphicsResult = mapViewProxy.identifyGraphicsOverlays(
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 10.0.dp,
                returnPopupsOnly = false
            ).getOrNull()

            if (!geometryEditor.isStarted.value) {
                if (graphicsResult != null) {
                    if (graphicsResult.isNotEmpty()) {
                        // get the tapped graphic
                        identifiedGraphic = graphicsResult.first().graphics.first()
                        // select the graphic
                        identifiedGraphic.isSelected = true
                        // start the geometry editor with the identified graphic
                        identifiedGraphic.geometry?.let {
                            geometryEditor.start(it)
                        }
                    }
                }
                // reset the identified graphic back to null
                identifiedGraphic.geometry = null
            }
        }
    }

}
