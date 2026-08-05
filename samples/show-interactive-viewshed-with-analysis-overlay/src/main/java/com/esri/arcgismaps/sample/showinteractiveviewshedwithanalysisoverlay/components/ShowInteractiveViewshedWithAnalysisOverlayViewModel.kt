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
import com.arcgismaps.mapping.view.AnalysisViewStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LongPressEvent
import com.arcgismaps.mapping.view.PanChangeEvent
import com.arcgismaps.mapping.view.PanChangeEvent.PanStatus
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.R
import com.esri.arcgismaps.sample.showinteractiveviewshedwithanalysisoverlay.components.ViewshedUiState.Companion.initialViewshedUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.Path

class ShowInteractiveViewshedWithAnalysisOverlayViewModel(app: Application) : AndroidViewModel(app) {
    // Initialize and keep track of UI state
    private val _viewshedUiState = MutableStateFlow(initialViewshedUiState)
    val viewshedUiState = _viewshedUiState.asStateFlow()

    // Create a MapViewProxy, used to convert screen points to map points
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the overlays it uses
    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISImagery).apply {
            initialViewpoint =
                Viewpoint(latitude = 55.610000, longitude = -5.200346, scale = 150000.0)
        }
    )
    var analysisOverlay by mutableStateOf(AnalysisOverlay())
    var graphicsOverlay by mutableStateOf(GraphicsOverlay())

    // Create and keep track of ViewshedParameters
    private val viewshedParameters by mutableStateOf(ViewshedParameters())

    // Setup initial observer position, and a symbol and Graphic to draw at the observer position
    private val initialObserverPosition = Point(
        x = -579246.504,
        y = 7479619.947,
        z = initialViewshedUiState.observerElevation,
        spatialReference = SpatialReference.webMercator()
    )
    private val observerSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Circle,
        color = Color.blue,
        size = 10.0f
    )
    private val observerGraphic =
        Graphic(geometry = initialObserverPosition, symbol = observerSymbol)

    // Indicates if observer position is currently being dragged across the map
    var isDragging by mutableStateOf(false)

    // Keep track of haptic feedback events, used when dragging the observer position
    private val _dragHapticEvents = MutableSharedFlow<DragHapticEvent>(extraBufferCapacity = 1)
    val dragHapticEvents = _dragHapticEvents.asSharedFlow()

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.show_interactive_viewshed_with_analysis_overlay_app_name
        )
    }
    private val filePath = Path(provisionPath, app.getString(R.string.elevation_data_filename))

    // Used to surface errors to the Compose UI
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Configure the ViewshedParameters using initial values from the UI state
        viewshedParameters.apply {
            observerPosition = initialObserverPosition
            targetHeight = initialViewshedUiState.targetHeight
            maxRadius = initialViewshedUiState.maxRadius
            fieldOfView = initialViewshedUiState.fieldOfView
            heading = initialViewshedUiState.heading
        }

        viewModelScope.launch {
            // Display a symbol to mark the observer position
            graphicsOverlay.graphics.add(observerGraphic)

            // Create a ContinuousField from a raster file containing elevation data
            val filePaths = listOf(filePath.toString())
            ContinuousField.createFromFiles(filePaths = filePaths, band = 0)
                .onFailure {
                    messageDialogVM.showMessageDialog(it)
                }.onSuccess { continuousField ->
                    // Create a ContinuousFieldFunction from the ContinuousField
                    val continuousFieldFunction = ContinuousFieldFunction.create(continuousField)

                    // Create a ViewshedFunction using the ContinuousFieldFunction and
                    // ViewshedParameters, then convert it to a DiscreteFieldFunction
                    val viewshedFunction =
                        ViewshedFunction(
                            elevation = continuousFieldFunction,
                            parameters = viewshedParameters
                        )
                    val discreteViewshed = viewshedFunction.toDiscreteFieldFunction()

                    // Create a ColormapRenderer from a Colormap with colors that represent visible
                    // and non-visible results
                    val areaNotVisibleColor = Color.gray
                    val areaVisibleColor = Color.fromRgba(r = 136, g = 204, b = 132, a = 100)
                    val colors = listOf(areaNotVisibleColor, areaVisibleColor)
                    val colormap = Colormap.create(colors)
                    val colormapRenderer = ColormapRenderer(colormap)

                    // Create a FieldAnalysis from the DiscreteFieldFunction and ColormapRenderer,
                    // then add it to the AnalysisOverlay's collection of analysis objects to
                    // display the results
                    val analysis =
                        FieldAnalysis(discreteFieldFunction = discreteViewshed, colormapRenderer)
                    analysisOverlay.analyses.add(analysis)
                }
        }
    }

    /**
     * Sets a new observer elevation.
     */
    fun setObserverElevation(observerElevation: Float) {
        viewshedParameters.observerPosition?.let { oldPos ->
            val observerPosition = Point(
                x = oldPos.x,
                y = oldPos.y,
                z = observerElevation.toDouble(),
                spatialReference = oldPos.spatialReference
            )
            syncObserverPosition(observerPosition)
            _viewshedUiState.update { it.copy(observerElevation = observerElevation.toDouble()) }
        }
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
     * Sets the observer position to the location of the given tap [event].
     */
    fun onTap(event: SingleTapConfirmedEvent) {
        setNewObserverPosition(event.mapPoint)
    }

    /**
     * Acts on a long press [event] by setting the observer position to the location of the long
     * press and allowing it to be dragged across the map.
     */
    fun onLongPress(event: LongPressEvent) {
        observerGraphic.isSelected = true
        isDragging = true
        _dragHapticEvents.tryEmit(DragHapticEvent.Start)
        setNewObserverPosition(event.mapPoint)
    }

    /**
     * Acts on a pan [event]. If the observer position is currently being dragged, the new position
     * is set to match the current screen coordinate. Dragging is terminated when panning ends.
     */
    fun onPan(event: PanChangeEvent) {
        if (isDragging) {
            setNewObserverPosition(mapViewProxy.screenToLocationOrNull(event.screenCoordinate))
            if (event.status == PanStatus.End) {
                observerGraphic.isSelected = false
                isDragging = false
                _dragHapticEvents.tryEmit(DragHapticEvent.End)
            }
        }
    }

    /**
     * Sets the observer position to the given [mapPoint].
     */
    private fun setNewObserverPosition(mapPoint: Point?) {
        if (mapPoint != null) {
            viewshedParameters.observerPosition?.let { oldPos ->
                val observerPosition = when (oldPos.z) {
                    null -> Point(x = mapPoint.x, y = mapPoint.y, mapPoint.spatialReference)
                    else -> Point(
                        x = mapPoint.x,
                        y = mapPoint.y,
                        z = oldPos.z!!,
                        mapPoint.spatialReference
                    )
                }
                syncObserverPosition(observerPosition)
            }
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

    /**
     * Display dialog if there is an error with analysis.
     */
    fun analysisViewStatusListener(event: GeoView.GeoViewAnalysisViewStatusChanged) {
        if (event.analysisViewStatus is AnalysisViewStatus.Error) {
            messageDialogVM.showMessageDialog(
                throwable = (event.analysisViewStatus as AnalysisViewStatus.Error).details
            )
        }
    }
}

data class ViewshedUiState(
    val observerElevation: Double,
    val targetHeight: Double,
    val maxRadius: Double,
    val fieldOfView: Double,
    val heading: Double,
    val elevationSamplingInterval: Double
) {
    companion object {
        // Initial viewshed parameters to drive the UI on launch
        val initialViewshedUiState = ViewshedUiState(
            observerElevation = 20.0,
            targetHeight = 20.0,
            maxRadius = 8000.0,
            fieldOfView = 150.0,
            heading = 10.0,
            elevationSamplingInterval = 0.0
        )
    }
}

enum class DragHapticEvent {
    Start,
    End,
}
