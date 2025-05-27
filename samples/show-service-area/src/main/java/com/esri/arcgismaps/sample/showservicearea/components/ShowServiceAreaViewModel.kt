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
import com.arcgismaps.tasks.networkanalysis.ServiceAreaPolygon
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
    // ArcGISMap centered over San Diego
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTerrain).apply {
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
    private val facilitiesOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(
            symbol = PictureMarkerSymbol( // Use a blue star pin for facilities
                url = "https://static.arcgis.com/images/Symbols/Shapes/BluePin1LargeB.png"
            ).apply {
                // Offset to align image properly
                offsetY = 21f
            })
    }
    private val barriersOverlay = GraphicsOverlay().apply {
        // Red diagonal cross fill for barriers
        val barrierSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.DiagonalCross,
            color = Color.red,
            outline = null
        )
        renderer = SimpleRenderer(barrierSymbol)
    }
    private val serviceAreaOverlay = GraphicsOverlay()

    // Expose overlays as a list for MapView
    val graphicsOverlays = listOf(facilitiesOverlay, barriersOverlay, serviceAreaOverlay)

    // Service area task for the San Diego network analysis service
    private val serviceAreaTask = ServiceAreaTask(
        url = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ServiceArea"
    )

    // StateFlow for the currently selected graphic type (facility or barrier)
    private val _selectedGraphicType = MutableStateFlow(GraphicType.Facility)
    val selectedGraphicType: StateFlow<GraphicType> = _selectedGraphicType.asStateFlow()

    // StateFlow for time break values (combined in a data class)
    private val _timeBreaks = MutableStateFlow(TimeBreaks(3, 8))
    val timeBreaks: StateFlow<TimeBreaks> = _timeBreaks.asStateFlow()

    // StateFlow for loading status (used to show loading dialog)
    private val _isSolvingServiceArea = MutableStateFlow(false)
    val isSolvingServiceArea: StateFlow<Boolean> = _isSolvingServiceArea.asStateFlow()

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    /**
     * Called when the user taps the map to add a facility or barrier
     * at the given [mapPoint] coordinates.
     */
    fun onSingleTap(mapPoint: Point) {
        when (_selectedGraphicType.value) {
            GraphicType.Facility -> addFacilityGraphic(mapPoint)
            GraphicType.Barrier -> addBarrierGraphic(mapPoint)
        }
    }

    /**
     * Adds a facility graphic to the facilities overlay at the given [point].
     */
    private fun addFacilityGraphic(point: Point) {
        val graphic = Graphic(geometry = point)
        facilitiesOverlay.graphics.add(graphic)
    }

    /**
     * Adds a barrier graphic (buffered polygon) to the barriers overlay at the given [point].
     */
    private fun addBarrierGraphic(point: Point) {
        val bufferedGeometry = GeometryEngine.bufferOrNull(geometry = point, distance = 500.0)
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
     * Update the selected graphic type (facility or barrier) for adding graphics.
     */
    fun updateSelectedGraphicType(type: GraphicType) {
        _selectedGraphicType.value = type
    }

    /**
     * Updates the time break values for service area calculation.
     */
    fun updateTimeBreaks(first: Int, second: Int) {
        _timeBreaks.value = TimeBreaks(first, second)
        showServiceArea()
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
                // Always create new parameters for each solve
                val serviceAreaParameters = serviceAreaTask.createDefaultParameters().getOrElse {
                    return@launch messageDialogVM.showMessageDialog(it)
                }
                serviceAreaParameters.geometryAtOverlap = ServiceAreaOverlapGeometry.Dissolve
                // Clear previous service area graphics
                serviceAreaOverlay.graphics.clear()
                // Set facilities from facility graphics
                val facilities = facilitiesOverlay.graphics.mapNotNull { graphic ->
                    (graphic.geometry as? Point)?.let { ServiceAreaFacility(it) }
                }
                serviceAreaParameters.setFacilities(facilities)
                // Set polygon barriers from barrier graphics
                val barriers = barriersOverlay.graphics.mapNotNull { graphic ->
                    (graphic.geometry as? Polygon)?.let { PolygonBarrier(it) }
                }
                serviceAreaParameters.setPolygonBarriers(barriers)
                // Set the time breaks (impedance cutoffs)
                serviceAreaParameters.defaultImpedanceCutoffs.clear()
                serviceAreaParameters.defaultImpedanceCutoffs.addAll(
                    listOf(_timeBreaks.value.first.toDouble(), _timeBreaks.value.second.toDouble())
                )
                // Solve the service area
                val result = serviceAreaTask.solveServiceArea(serviceAreaParameters)
                    .getOrElse { return@launch messageDialogVM.showMessageDialog(it) }
                // Display polygons for the first facility (if any)
                val polygons: List<ServiceAreaPolygon> = result.getResultPolygons(0)
                polygons.forEachIndexed { index, polygon ->
                    val fillSymbol = createServiceAreaSymbol(index == 0)
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
     * Creates a fill symbol for the service area polygons.
     * If [isFirst] use, polygon (yellow) else, second (green).
     */
    private fun createServiceAreaSymbol(isFirst: Boolean): Symbol {
        // Colors are semi-transparent
        val lineSymbolColor = if (isFirst) {
            Color.fromRgba(r = 100, g = 100, b = 0, a = 70) // Yellow outline
        } else {
            Color.fromRgba(r = 0, g = 100, b = 0, a = 70) // Green outline
        }
        val fillSymbolColor = if (isFirst) {
            Color.fromRgba(r = 200, g = 200, b = 0, a = 70) // Yellow fill
        } else {
            Color.fromRgba(r = 0, g = 200, b = 0, a = 70) // Green fill
        }

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
     * Enum for the type of graphic to add (facility or barrier).
     */
    enum class GraphicType(val label: String) {
        Facility("Facilities"),
        Barrier("Barriers")
    }

    /**
     * Data class for holding both time break values together.
     */
    data class TimeBreaks(val first: Int, val second: Int)
}
