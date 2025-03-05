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
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.FreehandTool
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.ReticleVertexTool
import com.arcgismaps.mapping.view.geometryeditor.ShapeTool
import com.arcgismaps.mapping.view.geometryeditor.ShapeToolType
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val vertexTool = geometryEditor.tool // use the default tool, which is vertex
    private val reticleVertexTool = ReticleVertexTool()
    private val freehandTool = FreehandTool()
    private val arrowTool = ShapeTool(ShapeToolType.Arrow)
    private val ellipseTool = ShapeTool(ShapeToolType.Ellipse)
    private val rectangleTool = ShapeTool(ShapeToolType.Rectangle)
    private val triangleTool = ShapeTool(ShapeToolType.Triangle)

    private val _selectedTool = MutableStateFlow(ToolType.Vertex)
    val selectedTool = _selectedTool.asStateFlow()

    private val _currentGeometryType = MutableStateFlow<GeometryType>(GeometryType.Unknown)
    val currentGeometryType = _currentGeometryType.asStateFlow()

    // fill styles to be used for graphics
    private val pointSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Square,
        color = Color.red,
        size = 10f
    )
    private val multiPointSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Circle,
        color = Color.yellow,
        size = 5f
    )
    private val polylineSymbol =  SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.blue,
        width = 2f
    )
    private val polygonLineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Dash,
        color = Color.black,
        width = 1f
    )
    private val polygonSymbol = SimpleFillSymbol(
        style = SimpleFillSymbolStyle.Solid,
        color = Color.fromRgba(r = 255, g = 0, b = 0, a = 100),
        outline = polygonLineSymbol
    )

    init {
        viewModelScope.launch {
            // load the map
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }.onSuccess {
                // create graphics for the initial geometries and add them to the graphics overlay
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = Geometry.fromJsonOrNull(houseCoordinatesJson),
                        symbol = pointSymbol
                    )
                )
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = Geometry.fromJsonOrNull(outbuildingCoordinatesJson),
                        symbol = multiPointSymbol
                    )
                )
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = Geometry.fromJsonOrNull(road1CoordinatesJson),
                        symbol = polylineSymbol
                    )
                )
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = Geometry.fromJsonOrNull(road2CoordinatesJson),
                        symbol = polylineSymbol
                    )
                )
                graphicsOverlay.graphics.add(
                    Graphic(
                        geometry = Geometry.fromJsonOrNull(boundaryCoordinatesJson),
                        symbol = polygonSymbol
                    )
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
     * Undoes the last event on the geometry editor if possible.
     */
    fun undoEdit() {
        if (geometryEditor.canUndo.value) {
            geometryEditor.undo()
        }
    }

    /**
     * Redoes the last event on the geometry editor if possible.
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
            ToolType.Vertex -> geometryEditor.tool = vertexTool
            ToolType.ReticleVertex -> geometryEditor.tool = reticleVertexTool
            ToolType.Freehand -> geometryEditor.tool = freehandTool
            ToolType.Arrow -> geometryEditor.tool = arrowTool
            ToolType.Ellipse -> geometryEditor.tool = ellipseTool
            ToolType.Rectangle -> geometryEditor.tool = rectangleTool
            ToolType.Triangle -> geometryEditor.tool = triangleTool
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
            is Point -> graphic.symbol = pointSymbol
            is Multipoint -> graphic.symbol = multiPointSymbol
            is Polyline -> graphic.symbol = polylineSymbol
            is Polygon -> graphic.symbol = polygonSymbol
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

    // json formatted strings for initial geometries
    private val houseCoordinatesJson = """{"x": -1067898.59, "y": 6998366.62,
    "spatialReference": {"latestWkid":3857, "wkid":102100}}"""

    private val outbuildingCoordinatesJson = """{"points":[[-1067984.26,6998346.28],[-1067966.80,6998244.84],
            [-1067921.88,6998284.65],[-1067934.36,6998340.74],
            [-1067917.93,6998373.97],[-1067828.30,6998355.28],
            [-1067832.25,6998339.70],[-1067823.10,6998336.93],
            [-1067873.22,6998386.78],[-1067896.72,6998244.49]],
            "spatialReference":{"latestWkid":3857,"wkid":102100}}"""

    private val road1CoordinatesJson = """{"paths":[[[-1068095.40,6998123.52],[-1068086.16,6998134.60],
            [-1068083.20,6998160.44],[-1068104.27,6998205.37],
            [-1068070.63,6998255.22],[-1068014.44,6998291.54],
            [-1067952.33,6998351.85],[-1067927.93,6998386.93],
            [-1067907.97,6998396.78],[-1067889.86,6998406.63],
            [-1067848.08,6998495.26],[-1067832.92,6998521.11]]],
            "spatialReference":{"latestWkid":3857,"wkid":102100}}"""

    private val road2CoordinatesJson = """{"paths":[[[-1067999.28,6998061.97],[-1067994.48,6998086.59],
            [-1067964.53,6998125.37],[-1067952.70,6998215.84],
            [-1067923.13,6998347.54],[-1067903.90,6998391.86],
            [-1067895.40,6998422.02],[-1067891.70,6998460.18],
            [-1067889.49,6998483.56],[-1067880.98,6998527.26]]],
            "spatialReference":{"latestWkid":3857,"wkid":102100}}"""

    private val boundaryCoordinatesJson = """{ "rings": [[[-1067943.67,6998403.86],[-1067938.17,6998427.60],
            [-1067898.77,6998415.86],[-1067888.26,6998398.80],
            [-1067800.85,6998372.93],[-1067799.61,6998342.81],
            [-1067809.38,6998330.00],[-1067817.07,6998307.85],
            [-1067838.07,6998285.34],[-1067849.10,6998250.38],
            [-1067874.02,6998256.00],[-1067879.87,6998235.95],
            [-1067913.41,6998245.03],[-1067934.84,6998291.34],
            [-1067948.41,6998251.90],[-1067961.18,6998186.68],
            [-1068008.59,6998199.49],[-1068052.89,6998225.45],
            [-1068039.37,6998261.11],[-1068064.12,6998265.26],
            [-1068043.32,6998299.88],[-1068036.25,6998327.93],
            [-1068004.43,6998409.28],[-1067943.67,6998403.86]]],
            "spatialReference":{"latestWkid":3857,"wkid":102100}}"""

    /**
     * Define a blue color for polylines.
     */
    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

}
