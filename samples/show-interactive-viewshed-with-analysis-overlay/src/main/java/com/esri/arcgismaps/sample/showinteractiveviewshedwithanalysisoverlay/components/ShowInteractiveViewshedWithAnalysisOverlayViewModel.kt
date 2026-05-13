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

package com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldFunction
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.analysis.visibility.ViewshedFunction
import com.arcgismaps.analysis.visibility.ViewshedParameters
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.raster.Colormap
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.PanChangeEvent.PanStatus
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ShowInteractiveViewshedWithAnalysisOverlayViewModel(app: Application) : AndroidViewModel(app) {
    // Initialize and keep track of UI state
    private val initObserverElevation = 20.0
    private val initTargetHeight = 20.0
    private val initMaxRadius = 8000.0
    private val initFieldOfView = 150.0
    private val initHeading = 10.0
    private val initElevationSamplingInterval = 0.0

    private val initViewshedUiState = ViewshedUiState(
        observerElevation = initObserverElevation,
        targetHeight = initTargetHeight,
        maxRadius = initMaxRadius,
        fieldOfView = initFieldOfView,
        heading = initHeading,
        elevationSamplingInterval = initElevationSamplingInterval
    )

    private val _viewshedUiState = MutableStateFlow(initViewshedUiState)
    val viewshedUiState = _viewshedUiState.asStateFlow()

    // Initialize and keep track of the ArcGISMap & the overlays it uses
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint = Viewpoint(55.610000, -5.200346, 150000.0)
        }
    )
    var analysisOverlay by mutableStateOf(AnalysisOverlay())
    var graphicsOverlay by mutableStateOf(GraphicsOverlay())

    // Create and keep track of ViewshedParameters
    private val viewshedParameters by mutableStateOf(ViewshedParameters())

    // Setup initial observer position, and a symbol and Graphic to draw at the observer position
    private val initialObserverPosition =
        Point(-579246.504, 7479619.947, initObserverElevation, SpatialReference.webMercator())
    private val observerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Circle,
        Color.blue,
        10.0f
    )
    private val observerGraphic = Graphic(initialObserverPosition, observerSymbol)

    // Indicates if observer position is currently being dragged across the map
    private var isDragging = false

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(
            R.string.show_interactive_viewshed_with_analysis_overlay_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + app.getString(R.string.elevation_data_filename)

    init {
        // Configure the ViewshedParameters
        viewshedParameters.observerPosition = initialObserverPosition
        viewshedParameters.targetHeight = initTargetHeight
        viewshedParameters.maxRadius = initMaxRadius
        viewshedParameters.fieldOfView = initFieldOfView
        viewshedParameters.heading = initHeading

        viewModelScope.launch {
            // Load the map
            arcGISMap.load().getOrThrow()

            // Display a symbol to mark the observer position
            graphicsOverlay.graphics.add(observerGraphic)

            // Create a ContinuousField from a raster file containing elevation data
            val filePaths = listOf(filePath)
            val continuousField = ContinuousField.createFromFiles(filePaths, 0).getOrThrow()

            // Create a ContinuousFieldFunction from the ContinuousField
            val continuousFieldFunction = ContinuousFieldFunction.create(continuousField)

            // Create a ViewshedFunction using the ContinuousFieldFunction and ViewshedParameters,
            // then convert it to a DiscreteFieldFunction
            val viewshedFunction = ViewshedFunction(continuousFieldFunction, viewshedParameters)
            val discreteViewshed = viewshedFunction.toDiscreteFieldFunction()

            // Create a ColormapRenderer from a Colormap with colors that represent visible and
            // non-visible results
            val areaNotVisibleColor = Color.gray
            val areaVisibleColor = Color.fromRgba(136, 204, 132, 100) // translucent green
            val colors = listOf(areaNotVisibleColor, areaVisibleColor)
            val colormap = Colormap.create(colors)
            val colormapRenderer = ColormapRenderer(colormap)

            // Create a FieldAnalysis from the DiscreteFieldFunction and ColormapRenderer, then add
            // it to the AnalysisOverlay's collection of analysis objects to display the results
            val analysis = FieldAnalysis(discreteViewshed, colormapRenderer)
            analysisOverlay.analyses.add(analysis)
        }
    }

    /**
     * Sets a new observer elevation.
     */
    fun setObserverElevation(observerElevation: Float) {
        val oldPos = viewshedParameters.observerPosition
        val observerPosition = Point(oldPos!!.x, oldPos.y, observerElevation.toDouble())
        syncObserverPosition(observerPosition)
        _viewshedUiState.update { it.copy(observerElevation = observerElevation.toDouble()) }
    }

    /**
     * Sets a new target height.
     */
    fun setTargetHeight(targetHeight: Float) {
        viewshedParameters.targetHeight = targetHeight.toDouble()
        _viewshedUiState.update { it.copy(targetHeight = targetHeight.toDouble()) }
    }

    /**
     * Sets a new maximum radius.
     */
    fun setMaxRadius(maxRadius: Float) {
        viewshedParameters.maxRadius = maxRadius.toDouble()
        _viewshedUiState.update { it.copy(maxRadius = maxRadius.toDouble()) }
    }

    /**
     * Sets a new field of view.
     */
    fun setFieldOfView(fieldOfView: Float) {
        viewshedParameters.fieldOfView = fieldOfView.toDouble()
        _viewshedUiState.update { it.copy(fieldOfView = fieldOfView.toDouble()) }
    }

    /**
     * Sets a new heading.
     */
    fun setHeading(heading: Float) {
        viewshedParameters.heading = heading.toDouble()
        _viewshedUiState.update { it.copy(heading = heading.toDouble()) }
    }

    /**
     * Sets a new elevation sampling interval.
     */
    fun setElevationSamplingInterval(elevationSamplingInterval: Double) {
        viewshedParameters.elevationSamplingInterval = when (elevationSamplingInterval) {
            0.0 -> null
            else -> elevationSamplingInterval
        }
        _viewshedUiState.update { it.copy(elevationSamplingInterval = elevationSamplingInterval) }
    }

    /**
     * Sets the observer position to the given [mapPoint].
     */
    fun onTap(mapPoint: Point?) {
        setNewObserverPosition(mapPoint)
    }

    /**
     * Acts on a long press [event] by setting the observer position to the location of the long
     * press and allowing it to be dragged across the map.
     */
    fun onLongPress(event: LongPressEvent) {
        observerSymbol.color = Color.yellow
        isDragging = true
        setNewObserverPosition(event.mapPoint)
    }

    /**
     * Acts on a pan [event]. If the observer position is currently being dragged, the new position
     * is set to match the current screen coordinate. Dragging is terminated when panning ends.
     */
    fun onPan(event: PanChangeEvent, mapViewProxy: MapViewProxy) {
        if (isDragging) {
            setNewObserverPosition(mapViewProxy.screenToLocationOrNull(event.screenCoordinate))
            if (event.status == PanStatus.End) {
                observerSymbol.color = Color.blue
                isDragging = false
            }
        }
    }

    /**
     * Sets the observer position to the given [mapPoint].
     */
    private fun setNewObserverPosition(mapPoint: Point?) {
        if (mapPoint != null) {
            val observerPosition = when (viewshedParameters.observerPosition?.z) {
                null -> Point(mapPoint.x, mapPoint.y)
                else -> Point(mapPoint.x, mapPoint.y, viewshedParameters.observerPosition?.z)
            }
            syncObserverPosition(observerPosition)
        }
    }

    /**
     * Synchronizes setting of a new [observerPosition]. This needs to be set in the
     * [viewshedParameters] and also as the geometry of the [observerGraphic].
     */
    private fun syncObserverPosition(observerPosition: Point) {
        // Update the observer graphic geometry to the current observer position
        observerGraphic.geometry = observerPosition

        // Update the viewshed parameters to the current observer position, which triggers analysis
        viewshedParameters.observerPosition = observerPosition
    }
}

data class ViewshedUiState(
    val observerElevation: Double,
    val targetHeight: Double,
    val maxRadius: Double,
    val fieldOfView: Double,
    val heading: Double,
    val elevationSamplingInterval: Double
)
