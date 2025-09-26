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

package com.esri.arcgismaps.sample.createbuffersaroundpoints.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Latitude
import com.arcgismaps.geometry.Longitude
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the "Create buffers around points" sample.
 */
class CreateBuffersAroundPointsViewModel(app: Application) : AndroidViewModel(app) {

    // Message dialog VM for reporting errors to the UI
    val messageDialogVM = MessageDialogViewModel()

    // Create the spatial reference (State Plane North Central Texas, WKID 32038)
    val statePlaneNorthCentralTexas = SpatialReference(wkid = 32038)

    // Map with projected spatial reference (State Plane North Central Texas).
    val arcGISMap by lazy {
        ArcGISMap(spatialReference = statePlaneNorthCentralTexas).apply {
            // Set initial viewpoint of the map
            initialViewpoint = Viewpoint(boundingGeometry = boundaryPolygon)
            // Set the basemap base layer to display the USA map server.
            setBasemap(
                basemap = Basemap(
                    baseLayer = ArcGISMapImageLayer(
                        url = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/USA/MapServer"
                    )
                )
            )
        }
    }
    // The boundary polygon that defines the valid area of use.
    private val boundaryPolygon: Polygon by lazy {
        // Build the boundary polygon coordinates in lat/long and project into the state plane spatial reference
        val boundaryLatLon = listOf(
            Point(Latitude(31.720), Longitude(-103.070)),
            Point(Latitude(34.580), Longitude(-103.070)),
            Point(Latitude(34.580), Longitude(-94.000)),
            Point(Latitude(31.720), Longitude(-94.000))
        )

        // Project the polygon into the State Plane spatial reference
        val projectedBoundaryPoints = boundaryLatLon.mapNotNull { point ->
            GeometryEngine.projectOrNull(
                geometry = point,
                spatialReference = statePlaneNorthCentralTexas
            )
        }
        // Build the boundary polygon geometry using the projected points
        PolygonBuilder(spatialReference = statePlaneNorthCentralTexas).apply {
            projectedBoundaryPoints.forEach { addPoint(it) }
        }.toGeometry()
    }

    // Graphics overlays: boundary, buffers, and tapped points
    private val boundaryGraphicsOverlay by lazy {
        GraphicsOverlay().apply {
            // Create a dashed red boundary graphic
            graphics += Graphic(
                geometry = boundaryPolygon,
                symbol = SimpleLineSymbol(
                    style = SimpleLineSymbolStyle.Dash,
                    color = Color.red,
                    width = 5f
                )
            )
        }
    }

    // Configure buffer overlay renderer (yellow fill with green outline)
    private val bufferGraphicsOverlay by lazy {
        val bufferOutline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.green,
            width = 3f
        )
        val bufferFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(r = 255, g = 255, b = 0, a = 153),
            outline = bufferOutline
        )
        GraphicsOverlay().apply { renderer = SimpleRenderer(symbol = bufferFill) }
    }


    // Configure tapped points overlay renderer (small red circles)
    private val tappedPointsGraphicsOverlay by lazy {
        val tapSymbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.red,
            size = 10f
        )
        GraphicsOverlay().apply { renderer = SimpleRenderer(symbol = tapSymbol) }
    }
   
    val graphicsOverlays: List<GraphicsOverlay>
        get() = listOf(boundaryGraphicsOverlay, bufferGraphicsOverlay, tappedPointsGraphicsOverlay)

    // Keep last tapped point when requesting radius input
    private var lastTappedPoint: Point? = null

    // List of (point, radiusInMapUnits)
    private val bufferPoints: MutableList<Pair<Point, Double>> = mutableListOf()


    private val _statusText = MutableStateFlow("Tap on the map to add buffers.")
    val statusText = _statusText.asStateFlow()

    private val _isInputDialogVisible = MutableStateFlow(false)
    val isInputDialogVisible = _isInputDialogVisible.asStateFlow()

    // Controls whether buffers should be union
    private val _shouldUnion = MutableStateFlow(false)
    val shouldUnion = _shouldUnion.asStateFlow()

    init {
        viewModelScope.launch {
            // Load the map; report any failure
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Called by the MapView on single tap. If the tap is within the valid boundary,
     * request a buffer radius input from the UI. Otherwise update status to indicate out-of-bounds.
     */
    fun onMapTapped(mapPoint: Point) {
        val contains = GeometryEngine.contains(boundaryPolygon, mapPoint)
        if (!contains) {
            _statusText.value = "Tap within the boundary to add buffer."
        }

        // Store the tapped point and request input dialog
        lastTappedPoint = mapPoint
        _isInputDialogVisible.value = true
        _statusText.value = "Enter buffer radius (miles) for the tapped location"
    }

    /**
     * Submit the radius (in miles) & converts miles to the map's linear units. Converts miles to the map's linear units
     * (the state plane spatial reference uses US feet) then creates the buffer and updates the overlays.
     */
    fun submitRadiusMiles(radiusMiles: Double) {
        val point = lastTappedPoint ?: return

        if (radiusMiles <= 0.0 || radiusMiles >= 300) {
            messageDialogVM.showMessageDialog("Please enter a value between 0 & 300.")
        }

        // Convert miles to feet (1 mile = 5280 feet). The state plane uses US feet for this sample.
        val radiusFeet = radiusMiles * 5280.0

        // Add to internal list and draw
        bufferPoints.add(point to radiusFeet)

        // Add tap point graphic
        tappedPointsGraphicsOverlay.graphics.add(Graphic(geometry = point))

        // Redraw buffers using current union setting
        drawBuffers(unioned = _shouldUnion.value)

        _isInputDialogVisible.value = false
        _statusText.value = "Buffer created. Tap to add more points or press Clear."
    }

    /**
     * Draws buffers for all stored bufferPoints. If unioned is true, attempt to union buffers
     * into a single geometry before displaying.
     */
    fun drawBuffers(unioned: Boolean) {
        // Clear existing buffer graphics
        bufferGraphicsOverlay.graphics.clear()

        if (bufferPoints.isEmpty()) {
            _statusText.value = "Add a point to draw the buffers."
            return
        }

        // Create buffer geometries for each point
        val polygons = bufferPoints.mapNotNull { (pt, radius) ->
            GeometryEngine.bufferOrNull(pt, radius)
        }

        if (polygons.isEmpty()) {
            messageDialogVM.showMessageDialog("Error creating buffer geometries")
            return
        }

        if (unioned) {
            // Union the polygons into a single geometry (may return a Polygon or Multipart geometry)
            GeometryEngine.unionOrNull(polygons)?.let { unionedGeometry ->
                bufferGraphicsOverlay.graphics.add(Graphic(geometry = unionedGeometry))
            }
        } else {
            // Add each polygon as its own graphic
            polygons.forEach { bufferGraphicsOverlay.graphics.add(Graphic(geometry = it)) }
        }
        _statusText.value = "Buffers drawn (${if (unioned) "unioned" else "individual"})."

    }

    /**
     * Update the union toggle and redraw buffers using the new value.
     */
    fun updateUnion(shouldUnion: Boolean) {
        _shouldUnion.value = shouldUnion
        drawBuffers(unioned = shouldUnion)
    }

    /**
     * Clears all buffer points and corresponding graphics.
     */
    fun clearAll() {
        bufferPoints.clear()
        bufferGraphicsOverlay.graphics.clear()
        tappedPointsGraphicsOverlay.graphics.clear()
        _statusText.value = "Tap on the map to add buffers."
    }

    /**
     * Dismiss the radius input dialog.
     */
    fun dismissInputDialog() {
        _isInputDialogVisible.value = false
        _statusText.value = "Tap on the map to add buffers."
    }
}
