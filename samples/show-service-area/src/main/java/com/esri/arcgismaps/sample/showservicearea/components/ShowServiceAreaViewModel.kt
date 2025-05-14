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

package com.esri.arcgismaps.sample.showservicearea.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.tasks.networkanalysis.PolygonBarrier
import com.arcgismaps.tasks.networkanalysis.ServiceAreaFacility
import com.arcgismaps.tasks.networkanalysis.ServiceAreaOverlapGeometry
import com.arcgismaps.tasks.networkanalysis.ServiceAreaParameters
import com.arcgismaps.tasks.networkanalysis.ServiceAreaPolygon
import com.arcgismaps.tasks.networkanalysis.ServiceAreaResult
import com.arcgismaps.tasks.networkanalysis.ServiceAreaTask
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Show Service Area sample.
 * Handles all ArcGIS Maps SDK logic, state, and exposes flows for Compose UI.
 */
class ShowServiceAreaViewModel(app: Application) : AndroidViewModel(app) {
    // ArcGISMap centered over San Diego, matching the Swift sample viewpoint
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTerrain).apply {
        initialViewpoint = Viewpoint(
            center = Point(
                x = -13041154.0,
                y = 3858170.0,
                spatialReference = SpatialReference.webMercator()
            ),
            scale = 60000.0
        )
    }

    // MapViewProxy for identify operations and map interaction
    val mapViewProxy = MapViewProxy()

    // Graphics overlays for facilities, barriers, and service areas
    val facilitiesOverlay = GraphicsOverlay().apply {
        // Use a blue star pin for facilities, matching Swift sample
        val facilitySymbol = PictureMarkerSymbol("https://static.arcgis.com/images/Symbols/Shapes/BluePin1LargeB.png")
        // Offset to align image properly
        facilitySymbol.apply {
            offsetY = 21f
            renderer = SimpleRenderer(this)
        }
    }
    val barriersOverlay = GraphicsOverlay().apply {
        // Red diagonal cross fill for barriers
        val barrierSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.DiagonalCross,
            color = Color.red,
            outline = null
        )
        renderer = SimpleRenderer(barrierSymbol)
    }
    val serviceAreaOverlay = GraphicsOverlay()

    // Expose overlays as a list for MapView
    val graphicsOverlays: List<GraphicsOverlay> = listOf(facilitiesOverlay, barriersOverlay, serviceAreaOverlay)

    // Service area task for the San Diego network analysis service
    private val serviceAreaTask = ServiceAreaTask("https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ServiceArea")

    // Service area parameters (created on first use)
    private var serviceAreaParameters: ServiceAreaParameters? = null

    // StateFlow for the currently selected graphic type (facility or barrier)
    private val _selectedGraphicType = MutableStateFlow(GraphicType.Facility)
    val selectedGraphicType: StateFlow<GraphicType> = _selectedGraphicType.asStateFlow()

    // StateFlows for time break values (default 3 and 8, as in Swift sample)
    private val _firstTimeBreak = MutableStateFlow(3)
    val firstTimeBreak: StateFlow<Int> = _firstTimeBreak.asStateFlow()
    private val _secondTimeBreak = MutableStateFlow(8)
    val secondTimeBreak: StateFlow<Int> = _secondTimeBreak.asStateFlow()

    // StateFlow for loading status (used to show loading dialog)
    private val _isSolvingServiceArea = MutableStateFlow(false)
    val isSolvingServiceArea: StateFlow<Boolean> = _isSolvingServiceArea.asStateFlow()

    // StateFlow for showing/hiding the bottom sheet
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    /**
     * Called when the user taps the map to add a facility or barrier.
     * @param mapPoint The tapped location in map coordinates.
     */
    fun handleMapTap(mapPoint: Point) {
        when (_selectedGraphicType.value) {
            GraphicType.Facility -> addFacilityGraphic(mapPoint)
            GraphicType.Barrier -> addBarrierGraphic(mapPoint)
        }
    }

    /**
     * Adds a facility graphic to the facilities overlay at the given point.
     * @param point The map point where the facility is added.
     */
    private fun addFacilityGraphic(point: Point) {
        val graphic = Graphic(geometry = point)
        facilitiesOverlay.graphics.add(graphic)
    }

    /**
     * Adds a barrier graphic (buffered polygon) to the barriers overlay at the given point.
     * @param point The map point where the barrier is added.
     */
    private fun addBarrierGraphic(point: Point) {
        // Create a 500 meter buffer polygon around the point (matching Swift sample)
        val bufferedGeometry = GeometryEngine.bufferOrNull(point, 500.0)
        val graphic = Graphic(geometry = bufferedGeometry)
        barriersOverlay.graphics.add(graphic)
    }

    /**
     * Removes all graphics from all overlays (reset the sample).
     */
    fun removeAllGraphics() {
        facilitiesOverlay.graphics.clear()
        barriersOverlay.graphics.clear()
        serviceAreaOverlay.graphics.clear()
    }

    /**
     * Sets the selected graphic type (facility or barrier) for adding graphics.
     */
    fun setSelectedGraphicType(type: GraphicType) {
        _selectedGraphicType.value = type
    }

    /**
     * Updates the time break values for service area calculation.
     */
    fun updateTimeBreaks(first: Int, second: Int) {
        _firstTimeBreak.value = first
        _secondTimeBreak.value = second
    }

    /**
     * Shows or hides the bottom sheet.
     */
    fun setBottomSheetVisible(visible: Boolean) {
        _isBottomSheetVisible.value = visible
    }

    /**
     * Calculates and displays the service area polygons for the current facilities and barriers.
     * Uses the time breaks specified by the user.
     */
    fun showServiceArea() {
        // Only allow one solve at a time
        if (_isSolvingServiceArea.value) return
        _isSolvingServiceArea.value = true
        viewModelScope.launch {
            try {
                // Create parameters if not already created
                if (serviceAreaParameters == null) {
                    serviceAreaParameters = serviceAreaTask.createDefaultParameters().getOrElse {
                        _isSolvingServiceArea.value = false
                        messageDialogVM.showMessageDialog(it)
                        return@launch
                    }
                    // Dissolve overlapping polygons (matches Swift sample)
                    serviceAreaParameters?.geometryAtOverlap = ServiceAreaOverlapGeometry.Dissolve
                }
                // Clear previous service area graphics
                serviceAreaOverlay.graphics.clear()
                // Set facilities from facility graphics
                val facilities = facilitiesOverlay.graphics.mapNotNull { graphic ->
                    (graphic.geometry as? Point)?.let { ServiceAreaFacility(it) }
                }
                serviceAreaParameters?.setFacilities(facilities)
                // Set polygon barriers from barrier graphics
                val barriers = barriersOverlay.graphics.mapNotNull { graphic ->
                    (graphic.geometry as? Polygon)?.let { PolygonBarrier(it) }
                }
                serviceAreaParameters?.setPolygonBarriers(barriers)
                // Set the time breaks (impedance cutoffs)
                serviceAreaParameters?.defaultImpedanceCutoffs?.clear()
                serviceAreaParameters?.defaultImpedanceCutoffs?.addAll(
                    listOf(_firstTimeBreak.value.toDouble(), _secondTimeBreak.value.toDouble())
                )
                // Solve the service area
                val result: ServiceAreaResult = serviceAreaTask.solveServiceArea(serviceAreaParameters!!)
                    .getOrElse {
                        _isSolvingServiceArea.value = false
                        messageDialogVM.showMessageDialog(it)
                        return@launch
                    }
                // Display polygons for the first facility (if any)
                val polygons: List<ServiceAreaPolygon> = result.getResultPolygons(0)
                polygons.forEachIndexed { index, polygon ->
                    val fillSymbol = makeServiceAreaSymbol(isFirst = index == 0)
                    val graphic = Graphic(
                        geometry = polygon.geometry,
                        symbol = fillSymbol
                    )
                    serviceAreaOverlay.graphics.add(graphic)
                }
            } finally {
                _isSolvingServiceArea.value = false
            }
        }
    }

    /**
     * Creates a fill symbol for the service area polygons, matching the Swift sample colors.
     * @param isFirst Whether this is the first polygon (yellow) or second (green).
     */
    private fun makeServiceAreaSymbol(isFirst: Boolean): Symbol {
        // Colors are semi-transparent, matching Swift sample
        val lineSymbolColor = if (isFirst) Color.fromRgba(102, 102, 0, 77) else Color.fromRgba(0, 102, 0, 77)
        val fillSymbolColor = if (isFirst) Color.fromRgba(204, 204, 0, 77) else Color.fromRgba(0, 204, 0, 77)
        val outline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = lineSymbolColor,
            width = 2f
        )
        return SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = fillSymbolColor,
            outline = outline
        )
    }

    /**
     * Companion object to provide Color.blue for ArcGIS SDK usage.
     */
    companion object {
        val Color.Companion.blue: Color
            get() = Color.fromRgba(0, 0, 255, 255)
    }

    /**
     * Enum for the type of graphic to add (facility or barrier).
     */
    enum class GraphicType(val label: String) {
        Facility("Facilities"),
        Barrier("Barriers")
    }
}
