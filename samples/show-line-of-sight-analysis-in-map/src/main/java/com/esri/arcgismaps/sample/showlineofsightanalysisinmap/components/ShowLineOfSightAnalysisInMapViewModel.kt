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
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
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
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.R
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.LineOfSightUiState.Companion.initialLineOfSightUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ShowLineOfSightAnalysisInMapViewModel(app: Application) : AndroidViewModel(app) {
    // Initialize and keep track of UI state
    private val _lineOfSightUiState = MutableStateFlow(initialLineOfSightUiState)
    val lineOfSightUiState = _lineOfSightUiState.asStateFlow()

    // Create a MapViewProxy, used for identifyGraphicsOverlays
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the overlays it uses
    private val targetPosition =
        Point(x = -577955.365, y = 7484288.220, z = 5.0, SpatialReference.webMercator())
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISHillshadeDark).apply {
            initialViewpoint = Viewpoint(center = targetPosition, scale = 150000.0)
        }
    )
    var targetGraphicsOverlay by mutableStateOf(GraphicsOverlay())
    var observersGraphicsOverlay by mutableStateOf(GraphicsOverlay())
    var resultsGraphicsOverlay by mutableStateOf(GraphicsOverlay())

    // Keep track of which observer is selected & the content of the Callout (if any)
    var selectedObserverGraphic: Graphic? by mutableStateOf(null)
    var calloutContentTitle: String by mutableStateOf("")
    var calloutContentDetail: String? by mutableStateOf(null)

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(
            R.string.show_line_of_sight_analysis_in_map_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + app.getString(R.string.elevation_data_filename)

    // Line of sight results by observer (for access when tapping on the observer graphics)
    private val lineOfSightResults = mutableMapOf<Observer, LineOfSight>()

    // Create symbols for the visible and not visible line segments
    private val visibleLineSymbol = SimpleLineSymbol(color = Color.green, width = 4f)
    private val notVisibleLineSymbol =
        SimpleLineSymbol(style = SimpleLineSymbolStyle.LongDash, color = Color.gray, width = 2f)

    // Create the observers
    private val observers = listOf(
        Observer(
            name = "Green Observer",
            color = Color.green,
            x = -580893.546,
            y = 7489102.890,
        ),
        Observer(
            name = "White Observer",
            color = Color.white,
            x = -583446.004,
            y = 7483567.462,
        ),
        Observer(
            name = "Cyan Observer",
            color = Color.cyan,
            x = -577665.236,
            y = 7490792.908,
        ),
        Observer(
            name = "Yellow Observer",
            color = Color.yellow,
            x = -576452.981,
            y = 7487071.388,
        ),
        Observer(
            name = "Magenta Observer",
            color = Color.magenta,
            x = -576650.067,
            y = 7481479.772,
        ),
        Observer(
            name = "Blue Observer",
            color = Color.blue,
            x = -571683.896,
            y = 7492017.864,
        ),
    )

    // Used to surface errors to the Compose UI
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Create a graphic to mark the target position and add it to a graphics overlay
            val beaconDrawable = ContextCompat.getDrawable(app, R.drawable.beacon) as BitmapDrawable
            val beaconSymbol = PictureMarkerSymbol.createWithImage(beaconDrawable)
            beaconSymbol.apply {
                width = 24f
                height = 24f
            }
            val targetGraphic = Graphic(geometry = targetPosition, symbol = beaconSymbol)
            targetGraphicsOverlay.graphics.add(targetGraphic)

            // Create a graphic for each observer and add them to a graphics overlay
            for ((index, observer) in observers.withIndex()) {
                val graphic = Graphic(geometry = observer.position, symbol = observer.symbol)
                graphic.attributes["observerIndex"] = index
                observersGraphicsOverlay.graphics.add(graphic)
            }

            // Create a ContinuousField from a raster file containing elevation data
            val filePaths = listOf(filePath)
            ContinuousField.createFromFiles(filePaths, band = 0)
                .onFailure {
                    messageDialogVM.showMessageDialog(it)
                }.onSuccess { continuousField ->
                    // Create line of sight positions for target and observers
                    val targetPositions = listOf(
                        LineOfSightPosition(targetPosition, HeightOrigin.Relative)
                    )
                    val observerPositions = observers.map { observer ->
                        LineOfSightPosition(observer.position, HeightOrigin.Relative)
                    }

                    // Create the line of sight parameters with the observer and target positions
                    val parameters = LineOfSightParameters()
                    parameters.observerTargetPairs =
                        ObserverTargetPairs(observerPositions, targetPositions)

                    // Create a LineOfSightFunction from the continuous field and line of sight parameters
                    val lineOfSightFunction = LineOfSightFunction(elevation = continuousField, parameters)

                    // Evaluate the function to get LineOfSight results
                    lineOfSightFunction.evaluate()
                        .onFailure {
                            messageDialogVM.showMessageDialog(it)
                        }.onSuccess { results ->
                            // Store the results by observer
                            for ((index, result) in results.withIndex()) {
                                lineOfSightResults[observers[index]] = result
                            }

                            // Add the line of sight results to a graphics overlay
                            for (result in results) {
                                // Use LineOfSight.targetVisibility to determine if the observer
                                // position has a direct line of sight to the target position
                                val targetVisibility = result.targetVisibility

                                // Add the visible line segment if it exists
                                if (result.visibleLine != null) {
                                    val graphic = Graphic(
                                            geometry = result.visibleLine,
                                            symbol = visibleLineSymbol
                                        )
                                    graphic.attributes["targetVisibility"] = targetVisibility
                                    resultsGraphicsOverlay.graphics.add(graphic)
                                }

                                // Add the not visible line segment if it exists
                                if (result.notVisibleLine != null) {
                                    val graphic = Graphic(
                                        geometry = result.notVisibleLine,
                                        symbol = notVisibleLineSymbol
                                    )
                                    graphic.attributes["targetVisibility"] = targetVisibility
                                    resultsGraphicsOverlay.graphics.add(graphic)
                                }
                            }
                        }
                }
        }
    }

    /**
     * Set the visibility filter to [value], showing or hiding the results graphics as appropriate.
     */
    fun setVisibilityFilter(value: Boolean) {
        // Update UI state
        _lineOfSightUiState.update { it.copy(visibilityFilter = value) }

        // If the visibility filter is selected (true), hide results graphics for which the target
        // is not visible
        for (graphic in resultsGraphicsOverlay.graphics) {
            val targetVisibility = graphic.attributes["targetVisibility"] as Float
            graphic.isVisible = !value || targetVisibility == 1.0f
        }
    }

    /**
     * Handle a tap at the given [singleTapConfirmedEvent].
     */
    fun onTap(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        viewModelScope.launch {
            // Dismiss any existing callout
            selectedObserverGraphic = null

            // Identify graphic(s) at the tap position
            mapViewProxy.identifyGraphicsOverlays(
                singleTapConfirmedEvent.screenCoordinate,
                tolerance = 10.dp
            ).onSuccess { resultsList ->
                if (resultsList.isNotEmpty()) {
                    // Find the first (if any) result from the graphics overlay containing observers
                    val identifyResult = resultsList.find { result ->
                        result.graphicsOverlay == observersGraphicsOverlay
                    }
                    if (identifyResult != null) {
                        val graphics = identifyResult.graphics
                        if (graphics.isNotEmpty()) {
                            val observerGraphic = graphics.first()

                            // Get the observer, using the index retrieved from the graphic attributes
                            val observer = observers[observerGraphic.attributes["observerIndex"] as Int]

                            // Get the line of sight result for the observer
                            val lineOfSightResult = lineOfSightResults[observer]

                            // Display a callout with the result details
                            selectedObserverGraphic = observerGraphic
                            calloutContentTitle = observer.name
                            calloutContentDetail = lineOfSightResult?.detail()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns a String describing the contents of this LineOfSight.
 */
fun LineOfSight.detail(): String? {
    // If there was an error during the analysis, return the error message
    error?.let { return it.message }

    // If neither line is present, return an empty string (though this should not happen in a valid
    // result)
    if (notVisibleLine == null && visibleLine == null) return ""

    // Calculate the length of the visible line, which is the unobstructed distance from the
    // observer to the target
    val visibleLength = visibleLine?.let {
        GeometryEngine.lengthGeodetic(
            geometry = it,
            lengthUnit = LinearUnit.meters,
            curveType = GeodeticCurveType.Geodesic
        )
    } ?: 0.0

    // If there is no not-visible line, the target is fully visible from the observer; return a
    // message with the length of the visible line
    if (notVisibleLine == null) {
        return "Target visible from observer over $visibleLength meters."
    }

    // Otherwise, the target is not fully visible; return a message with the unobstructed length
    return "Target not visible from observer. Obstructed after $visibleLength meters."
}

data class LineOfSightUiState(
    val visibilityFilter: Boolean
) {
    companion object {
        // Initial values to drive the UI on launch
        val initialLineOfSightUiState = LineOfSightUiState(
            visibilityFilter = false
        )
    }
}

data class Observer(
    val name: String,
    val color: Color,
    val x: Double,
    val y: Double
) {
    val position = Point(x, y, SpatialReference.webMercator())
    val symbol = SimpleMarkerSymbol(style = SimpleMarkerSymbolStyle.Triangle, color, size = 15f)
}
