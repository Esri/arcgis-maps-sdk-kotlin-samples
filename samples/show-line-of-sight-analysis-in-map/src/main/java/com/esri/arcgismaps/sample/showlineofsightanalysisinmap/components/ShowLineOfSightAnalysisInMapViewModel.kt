/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.HeightOrigin
import com.arcgismaps.analysis.visibility.LineOfSight
import com.arcgismaps.analysis.visibility.LineOfSightFunction
import com.arcgismaps.analysis.visibility.LineOfSightParameters
import com.arcgismaps.analysis.visibility.LineOfSightPosition
import com.arcgismaps.analysis.visibility.ObserverTargetPairs
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ShowLineOfSightAnalysisInMapViewModel(private val app: Application) : AndroidViewModel(app) {

    // Map centered on the Isle of Arran, Scotland
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISHillshadeDark).apply {
        initialViewpoint = Viewpoint(
            latitude = 55.632572,
            longitude = -5.135883,
            scale = 90000.0
        )
    }

    val mapViewProxy = MapViewProxy()

    // Overlays for inputs (target and observers) and results (visible/not visible lines)
    val losPositionsGraphicsOverlay = GraphicsOverlay()
    val resultsGraphicsOverlay = GraphicsOverlay()

    // UI states
    private val _showVisibleTargetsOnly = MutableStateFlow(false)
    val showVisibleTargetsOnly = _showVisibleTargetsOnly.asStateFlow()

    private val _observerSummaries = MutableStateFlow<List<String>>(emptyList())
    val observerSummaries = _observerSummaries.asStateFlow()

    // In case of failures, show an error message dialog
    val messageDialogVM = MessageDialogViewModel()

    // Geometry and symbol configuration
    private val relativeHeightMeters = 5.0

    private val targetPoint = Point(
        x = -577955.365,
        y = 7484288.220,
        z = relativeHeightMeters,
        spatialReference = SpatialReference.webMercator()
    )

    private data class ObserverSeed(val color: Color, val point: Point)

    // Observer locations
    private val observerSeeds = listOf(
        ObserverSeed(
            color = Color.green,
            point = Point(
                -580893.546,
                7489102.890,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        ),
        ObserverSeed(
            color = Color.white,
            point = Point(
                -583446.004,
                7483567.462,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        ),
        ObserverSeed(
            color = orange,
            point = Point(
                -577665.236,
                7490792.908,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        ),
        ObserverSeed(
            color = Color.yellow,
            point = Point(
                -576452.981,
                7487071.388,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        ),
        ObserverSeed(
            color = lightPurple,
            point = Point(
                -576650.067,
                7481479.772,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        ),
        ObserverSeed(
            color = blue,
            point = Point(
                -571683.896,
                7492017.864,
                relativeHeightMeters,
                SpatialReference.webMercator()
            )
        )
    )

    // Symbols for target and observers
    private val targetSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Circle,
        color = blue,
        size = 12f
    )

    // Line symbols for results
    private val visibleLineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.green,
        width = 4f
    )

    private val notVisibleLineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Dash,
        color = Color.fromRgba(128, 128, 128, 255),
        width = 2f
    )

    // Holds the latest line of sight results
    private var latestResults: List<LineOfSight> = emptyList()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = it.message.toString()
                )
            }
        }
    }

    /**
     * Prepare observers/target graphics and run the line of sight analysis.
     */
    fun initializeAnalysis() {
        viewModelScope.launch {
            addObserverAndTargetGraphics()
            runLineOfSightAnalysis()
        }
    }

    /**
     * Update whether only fully visible target lines should be shown.
     */
    fun updateShowVisibleTargetsOnly(visibleOnly: Boolean) {
        _showVisibleTargetsOnly.value = visibleOnly
        applyVisibilityFilter()
    }

    /**
     * Adds the target and observer graphics to the positions graphics overlay.
     */
    private fun addObserverAndTargetGraphics() {
        // Target graphic
        losPositionsGraphicsOverlay.graphics.add(
            Graphic(
                geometry = targetPoint,
                symbol = targetSymbol
            )
        )

        // Observer graphics
        observerSeeds.forEach { seed ->
            val observerSymbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Triangle,
                color = seed.color,
                size = 15f
            )
            losPositionsGraphicsOverlay.graphics.add(
                Graphic(
                    geometry = seed.point,
                    symbol = observerSymbol
                )
            )
        }
    }

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.show_line_of_sight_analysis_in_map_app_name
        )
    }


    /**
     * Creates a ContinuousField from the Isle of Arran elevation raster and evaluates line of sight
     * from multiple observers to a single target. Results are rendered in the results overlay.
     */
    private suspend fun runLineOfSightAnalysis() {
        // Attempt to use a local raster file named "arran.tif" from the app's external files directory.
        val elevationFile = File(provisionPath, "arran.tif")
        val filePaths = mutableListOf<String>()
        if (elevationFile.exists()) {
            filePaths.add(elevationFile.path)
        } else {
            // Fallback: attempt to access the data via a portal item; show guidance if not available
            val portal = Portal.arcGISOnline(Portal.Connection.Anonymous)
            val item = PortalItem(portal, "aa97788593e34a32bcaae33947fdc271")
            item.load().onFailure {
                messageDialogVM.showMessageDialog(
                    title = "Elevation raster not found",
                    description = "Place 'arran.tif' in the app's ExternalFiles directory or ensure access to the portal item.\n\n" +
                            (it.message ?: "")
                )
                return
            }.onSuccess {
                messageDialogVM.showMessageDialog(
                    title = "Elevation raster not found",
                    description = "Please provision 'arran.tif' in external files directory to run the analysis."
                )
                return
            }
        }

        // Create a ContinuousField from the elevation raster
        val continuousField = ContinuousField.createFromFiles(
            filePaths = filePaths,
            band = 0
        ).getOrElse {
            messageDialogVM.showMessageDialog(
                title = "Failed to create ContinuousField",
                description = it.message.toString()
            )
            return
        }

        // Create line of sight positions (target and observers)
        val targetLosPosition = LineOfSightPosition(
            position = targetPoint,
            heightOrigin = HeightOrigin.Relative
        )

        val observerLosPositions = observerSeeds.map { seed ->
            LineOfSightPosition(
                position = seed.point,
                heightOrigin = HeightOrigin.Relative
            )
        }

        // Configure parameters using many-to-many pairs (many observers to the one target)
        val parameters = LineOfSightParameters().apply {
            observerTargetPairs = ObserverTargetPairs(
                observerPositions = observerLosPositions,
                targetPositions = listOf(targetLosPosition)
            )
        }

        // Create and evaluate the line of sight function
        val lineOfSightFunction = LineOfSightFunction(
            elevation = continuousField,
            parameters = parameters
        )

        val results = lineOfSightFunction.evaluate().getOrElse {
            messageDialogVM.showMessageDialog(
                title = "Error evaluating line of sight",
                description = it.message.toString()
            )
            return
        }

        latestResults = results

        // Clear previous results
        resultsGraphicsOverlay.graphics.clear()

        // Add graphics for visible and not visible portions, and attribute each with visibility
        results.forEach { result ->
            val isTargetVisible = result.targetVisibility == 1f

            result.visibleLine?.let { polyline ->
                resultsGraphicsOverlay.graphics.add(
                    Graphic(
                        geometry = polyline,
                        symbol = visibleLineSymbol
                    ).apply { attributes[ATTR_IS_TARGET_VISIBLE] = isTargetVisible }
                )
            }

            result.notVisibleLine?.let { polyline ->
                resultsGraphicsOverlay.graphics.add(
                    Graphic(
                        geometry = polyline,
                        symbol = notVisibleLineSymbol
                    ).apply { attributes[ATTR_IS_TARGET_VISIBLE] = isTargetVisible }
                )
            }
        }

        // Update the info panel summaries
        _observerSummaries.value = buildObserverSummaries(results)

        // Apply the current visibility filter
        applyVisibilityFilter()
    }

    /**
     * When filtering is enabled, only show lines with targetVisibility == Visible.
     */
    private fun applyVisibilityFilter() {
        val visibleOnly = _showVisibleTargetsOnly.value
        resultsGraphicsOverlay.graphics.forEach { graphic ->
            val attrValue = graphic.attributes[ATTR_IS_TARGET_VISIBLE]
            val isTargetVisible = (attrValue as? Boolean) ?: false
            graphic.isVisible = !visibleOnly || isTargetVisible
        }
    }

    /**
     * Build summary text for each observer's result.
     */
    private fun buildObserverSummaries(results: List<LineOfSight>): List<String> {
        return results.mapIndexed { index, result ->
            val error = result.error
            if (error != null) {
                "Observer ${index + 1}: ${error.message}"
            } else {
                val visibleLen = polylineLengthMeters(result.visibleLine)
                val notVisibleLen = polylineLengthMeters(result.notVisibleLine)
                if (notVisibleLen <= 0.0) {
                    "Observer ${index + 1}: Target visible over ${visibleLen.formatMeters()} m"
                } else {
                    "Observer ${index + 1}: Target not visible; obscured after ${visibleLen.formatMeters()} m"
                }
            }
        }
    }

    private fun polylineLengthMeters(line: Polyline?): Double {
        if (line == null) return 0.0
        return GeometryEngine.lengthGeodetic(
            geometry = line as Geometry,
            lengthUnit = LinearUnit.meters,
            curveType = GeodeticCurveType.Geodesic
        )
    }

    private fun Double.formatMeters(): String = String.format("%.1f", this)

    companion object {
        private const val ATTR_IS_TARGET_VISIBLE = "isTargetVisible"

        private val orange: Color
            get() = Color.fromRgba(255, 165, 0, 255)
        private val blue: Color
            get() = Color.fromRgba(0, 0, 255, 255)
        private val lightPurple: Color
            get() = Color.fromRgba(228, 168, 239, 255)
    }
}
