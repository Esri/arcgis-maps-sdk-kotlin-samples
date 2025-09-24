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
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
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

    // Graphics overlays: boundary, buffers, and tapped points
    val boundaryGraphicsOverlay = GraphicsOverlay()
    val bufferGraphicsOverlay = GraphicsOverlay()
    val tappedPointsGraphicsOverlay = GraphicsOverlay()

    val graphicsOverlays: List<GraphicsOverlay>
        get() = listOf(boundaryGraphicsOverlay, bufferGraphicsOverlay, tappedPointsGraphicsOverlay)

    private val _statusText = MutableStateFlow("Tap on the map to add buffers.")
    val statusText = _statusText.asStateFlow()

    private val _isInputDialogVisible = MutableStateFlow(false)
    val isInputDialogVisible = _isInputDialogVisible.asStateFlow()

    // Controls whether buffers should be union
    private val _shouldUnion = MutableStateFlow(false)
    val shouldUnion = _shouldUnion.asStateFlow()

    // Keep last tapped point when requesting radius input
    private var lastTappedPoint: Point? = null

    // List of (point, radiusInMapUnits)
    private val bufferPoints: MutableList<Pair<Point, Double>> = mutableListOf()

    // The boundary polygon that defines the valid area of use.
    private var boundaryPolygon: Polygon? = null

    // Create the spatial reference (State Plane North Central Texas, WKID 32038)
    val statePlaneNorthCentralTexas = SpatialReference(wkid = 32038)

    // Map with projected spatial reference (State Plane North Central Texas).
    val arcGISMap = ArcGISMap(statePlaneNorthCentralTexas).apply {
        val usaLayer = ArcGISMapImageLayer(url = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/USA/MapServer")
        // set a basemap that works well with projected data
        setBasemap(Basemap(baseLayer = usaLayer))
    }

    init {
        viewModelScope.launch {
            try {

                // Build the boundary polygon coordinates in lat/long and project into the state plane spatial reference
                val boundaryLatLon = listOf(
                    Point(x = -103.070, y = 31.720, spatialReference = SpatialReference.wgs84()),
                    Point(x = -103.070, y = 34.580, spatialReference = SpatialReference.wgs84()),
                    Point(x = -94.000, y = 34.580, spatialReference = SpatialReference.wgs84()),
                    Point(x = -94.000, y = 31.720, spatialReference = SpatialReference.wgs84())
                )

                // Project the polygon into the State Plane spatial reference
                val polygonGeoms = boundaryLatLon.map { GeometryEngine.projectOrNull(it, statePlaneNorthCentralTexas) }
                if (polygonGeoms.any { it == null }) {
                    messageDialogVM.showMessageDialog("Error projecting boundary geometry")
                    return@launch
                }

                val builder = PolygonBuilder(spatialReference = statePlaneNorthCentralTexas)
                polygonGeoms.filterNotNull().forEach { builder.addPoint(it) }
                boundaryPolygon = builder.toGeometry()

                // The boundary polygon extent to configure the map's initial viewpoint
                boundaryPolygon?.let { poly ->
                   // Create and add a dashed red boundary graphic
                    val boundaryOutline = SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Dash,
                        color = Color.red,
                        width = 5f
                    )
                    val boundaryGraphic = Graphic(geometry = poly, symbol = boundaryOutline)
                    boundaryGraphicsOverlay.graphics.add(boundaryGraphic)
                }

                // Configure buffer overlay renderer (yellow fill with green outline)
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
                bufferGraphicsOverlay.renderer = SimpleRenderer(symbol = bufferFill)

                // Configure tapped points overlay renderer (small red circles)
                val tapSymbol = SimpleMarkerSymbol(
                    style = SimpleMarkerSymbolStyle.Circle,
                    color = Color.red,
                    size = 10f
                )
                tappedPointsGraphicsOverlay.renderer = SimpleRenderer(symbol = tapSymbol)

                //Set initial viewpoint of the map
                arcGISMap.initialViewpoint = Viewpoint(boundingGeometry = boundaryPolygon as Geometry)
                // Load the map; report any failure
                arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }


            } catch (e: Throwable) {
                messageDialogVM.showMessageDialog(e)
            }
        }
    }

    /**
     * Called by the MapView when the user taps the map. If the tap is within the valid boundary,
     * request a buffer radius input from the UI. Otherwise update status to indicate out-of-bounds.
     */
    fun onMapTapped(mapPoint: Point) {
        viewModelScope.launch {
            val boundary = boundaryPolygon
            if (boundary == null) {
                _statusText.value = "Boundary not initialized"
                return@launch
            }

            val contains = GeometryEngine.contains(boundary, mapPoint)
            if (!contains) {
                _statusText.value = "Tap within the boundary to add buffer."
                // Inform the user via message dialog as well
                messageDialogVM.showMessageDialog("Tap within the boundary to add buffer.")
                return@launch
            }

            // Store the tapped point and request input dialog
            lastTappedPoint = mapPoint
            _isInputDialogVisible.value = true
            _statusText.value = "Enter buffer radius (miles) for the tapped location"
        }
    }

    /**
     * Submit the radius (in miles) provided by the user. Converts miles to the map's linear units
     * (the state plane spatial reference uses US feet) then creates the buffer and updates the overlays.
     */
    fun submitRadiusMiles(radiusMiles: Double) {
        viewModelScope.launch {
            val point = lastTappedPoint
            if (point == null) {
                messageDialogVM.showMessageDialog("Internal error: missing tapped point")
                _isInputDialogVisible.value = false
                return@launch
            }

            if (radiusMiles <= 0.0) {
                messageDialogVM.showMessageDialog("Please enter a value greater than 0")
                return@launch
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
    }

    /**
     * Draws buffers for all stored bufferPoints. If unioned is true, attempt to union buffers
     * into a single geometry before displaying.
     */
    fun drawBuffers(unioned: Boolean) {
        viewModelScope.launch {
            // Clear existing buffer graphics
            bufferGraphicsOverlay.graphics.clear()

            if (bufferPoints.isEmpty()) {
                _statusText.value = "Add a point to draw the buffers."
                return@launch
            }

            // Create buffer geometries for each point
            val polygons = bufferPoints.mapNotNull { (pt, radius) ->
                GeometryEngine.bufferOrNull(pt, radius)
            }

            if (polygons.isEmpty()) {
                messageDialogVM.showMessageDialog("Error creating buffer geometries")
                return@launch
            }

            if (unioned) {
                // Union the polygons into a single geometry (may return a Polygon or Multipart geometry)
                val unionedGeometry = GeometryEngine.unionOrNull(polygons)
                if (unionedGeometry != null) {
                    bufferGraphicsOverlay.graphics.add(Graphic(geometry = unionedGeometry))
                } else {
                    // Fallback: add each polygon separately
                    polygons.forEach { bufferGraphicsOverlay.graphics.add(Graphic(geometry = it)) }
                }
            } else {
                // Add each polygon as its own graphic
                polygons.forEach { bufferGraphicsOverlay.graphics.add(Graphic(geometry = it)) }
            }

            _statusText.value = "Buffers drawn (${if (unioned) "unioned" else "individual"})."
        }
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
        viewModelScope.launch {
            bufferPoints.clear()
            bufferGraphicsOverlay.graphics.clear()
            tappedPointsGraphicsOverlay.graphics.clear()
            _statusText.value = "Tap on the map to add buffers."
        }
    }

    /**
     * Dismiss the radius input dialog.
     */
    fun dismissInputDialog() {
        _isInputDialogVisible.value = false
        _statusText.value = "Tap on the map to add buffers."
    }
}
