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

package com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components

import android.app.Application
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.BooleanFieldSampleFunction
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldSampleFunction
import com.arcgismaps.analysis.ContinuousFieldSupplier
import com.arcgismaps.analysis.FloatSupplier
import com.arcgismaps.analysis.interactive.Analysis
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.raster.ColorRamp
import com.arcgismaps.mapping.symbology.raster.Colormap
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.StretchRenderer
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
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.R
import com.esri.arcgismaps.sample.getfieldfunctionvaluefromapoint.components.FieldSampleUiState.Companion.initialFieldSampleUiState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class GetFieldFunctionValueFromAPointViewModel(app: Application) : AndroidViewModel(app) {
    // Create a state flow to hold the UI state for the supporting pane controls
    private val _fieldSampleUiState = MutableStateFlow(initialFieldSampleUiState)

    // Expose the state flow as read-only for the UI
    val uiState = _fieldSampleUiState.asStateFlow()

    // Create a MapViewProxy, used to set viewpoint & convert screen point to location
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the overlays it uses
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISHillshadeDark))
    var analysisOverlay by mutableStateOf(AnalysisOverlay())
    var graphicsOverlay by mutableStateOf(GraphicsOverlay())

    // Indicates when the ProgressIndicator should be displayed
    var displayProgressIndicator by mutableStateOf(false)
        private set

    // Collection of Analysis instances currently updating, ProgressIndicator displayed when non-empty
    val updatingAnalyses = mutableSetOf<Analysis>()

    // Indicates if aircraft location is currently being dragged across the map
    var isDragging by mutableStateOf(false)

    // Keep track of haptic feedback events, used when dragging the aircraft location
    private val _dragHapticEvents = MutableSharedFlow<DragHapticEvent>(extraBufferCapacity = 1)
    val dragHapticEvents = _dragHapticEvents.asSharedFlow()

    // Keep track of Callout location & contents
    var calloutGraphic: Graphic? by mutableStateOf(null)
    var sampleElevation: Float? by mutableStateOf(null)
    var groundClearance: Float? by mutableStateOf(null)
    var groundClearanceStatus: String by mutableStateOf("")

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.get_field_function_value_from_a_point_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + "Meribel.tif"

    // Indicates if sampling of field functions is current in progress
    var isSampling = false

    // Variables related to the aircraft
    private val aircraftGraphic = Graphic()
    private var aircraftLocationPoint: Point? = null
    private val aircraftAltitudeFloatSupplier =
        FloatSupplier(initialFieldSampleUiState.aircraftAltitude)

    // Field sample functions used in the analysis
    private lateinit var continuousFieldSampleFunction: ContinuousFieldSampleFunction
    private lateinit var booleanFieldSampleFunction: BooleanFieldSampleFunction

    // Stretch renderer for visualizing the continuous field
    private val stretchRenderer = StretchRenderer(
        parameters = MinMaxStretchParameters(
            minValues = listOf(1199.0),
            maxValues = listOf(2740.0)
        ),
        gammas = listOf(1.0),
        estimateStatistics = false,
        colorRamp = ColorRamp.createFromColors(
            colors = listOf(Color.fromRgba(r = 96, g = 98, b = 148, a = 255), Color.white),
            size = 256
        )
    )

    // Colormap renderer for visualizing the boolean field
    val colormapRenderer = ColormapRenderer(
        colormap = Colormap.create(
            colors = listOf(
                Color.fromRgba(r = 210, g = 60, b = 60, a = 255), // red
                Color.fromRgba(r = 50, g = 170, b = 90, a = 0), // transparent
            )
        )
    )

    init {
        // Create a graphic to mark the aircraft position and add it to a graphics overlay
        val aircraftDrawable =
            ContextCompat.getDrawable(app, R.drawable.helicopter) as BitmapDrawable
        val aircraftSymbol = PictureMarkerSymbol.createWithImage(aircraftDrawable)
        aircraftSymbol.apply {
            width = 28f
            height = 28f
        }
        aircraftGraphic.symbol = aircraftSymbol
        graphicsOverlay.graphics.add(aircraftGraphic)

        viewModelScope.launch {
            // Load the map and handle any errors by showing a message dialog
            arcGISMap.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }.onSuccess {
                // Create a ContinuousField from a raster file containing elevation data
                ContinuousField.createFromFiles(filePaths = listOf(filePath), band = 0)
                    .onFailure {
                        messageDialogVM.showMessageDialog(it)
                    }.onSuccess { continuousField ->
                        // Create the continuous field function to sample elevation data from; this
                        // function represents the elevation data from the continuous field
                        val continuousFieldFunction = ContinuousFieldSupplier(continuousField)

                        // Create the continuous field sample function to sample elevation data from
                        // the continuous field function
                        continuousFieldSampleFunction =
                            ContinuousFieldSampleFunction(continuousFieldFunction, position = null)

                        // Create initial point to analyze at center of the continuous field's extent
                        val fieldCenter = continuousField.extent.center
                        aircraftLocationPoint = Point(
                            x = fieldCenter.x,
                            y = fieldCenter.y,
                            z = _fieldSampleUiState.value.aircraftAltitude.toDouble(),
                            spatialReference = fieldCenter.spatialReference
                        )

                        // Position the aircraft graphic at the initial point
                        aircraftGraphic.geometry = aircraftLocationPoint

                        // Create a boolean field function representing clearance between ground-level
                        // and value of the aircraft altitude slider. This boolean field function will
                        // return true where ground-level is below the supplied altitude value. The
                        // value is supplied by the aircraft altitude float supplier.
                        val booleanFieldFunction = continuousFieldFunction.isLessThan(
                            aircraftAltitudeFloatSupplier
                        )

                        // Create the boolean field sample function to sample aircraft clearance status
                        // from the boolean field function
                        booleanFieldSampleFunction =
                            BooleanFieldSampleFunction(booleanFieldFunction, position = null)

                        // Set the initial viewpoint of the map to the center of the continuous
                        // field's extent
                        mapViewProxy.setViewpointCenter(
                            center = continuousField.extent.center,
                            scale = 50000.0
                        )

                        // Create the field analysis for the continuous field function. This displays the
                        // elevation of the terrain using a black-to-white color ramp in a stretch renderer.
                        val continuousFieldAnalysis =
                            FieldAnalysis(continuousFieldFunction, stretchRenderer)
                        continuousFieldAnalysis.opacity = 0.5f

                        // Add the continuous field analysis to the analysis overlay
                        analysisOverlay.analyses.add(continuousFieldAnalysis)

                        // Create the field analysis for the boolean field function. This displays whether
                        // the aircraft meets the clearance criteria using a colormap renderer.
                        val booleanFieldAnalysis =
                            FieldAnalysis(booleanFieldFunction, colormapRenderer)
                        booleanFieldAnalysis.opacity = 0.5f

                        // Add the boolean field analysis to the analysis overlay
                        analysisOverlay.analyses.add(booleanFieldAnalysis)

                        // Sample the field functions at the initial aircraft location and display
                        // the results in a Callout
                        sampleFieldFunctionsAndDisplayCallout(aircraftLocationPoint!!)
                    }
            }
        }
    }

    /**
     * Samples the field functions at the given [samplePoint] and displays the results in a Callout.
     */
    private suspend fun sampleFieldFunctionsAndDisplayCallout(samplePoint: Point) {
        // Just return if sampling at a previous point is still in progress (a recursive call at the
        // end of this function will catch up with the latest aircraft location)
        if (isSampling) {
            return
        }
        isSampling = true

        // Set the position of each sample function to the sample point
        continuousFieldSampleFunction.position = samplePoint
        booleanFieldSampleFunction.position = samplePoint

        // Evaluate the sample functions
        continuousFieldSampleFunction.evaluate()
            .onFailure {
                isSampling = false
                messageDialogVM.showMessageDialog(it)
            }.onSuccess { continuousFieldSample ->
                // Get the value of the continuous field sample, which represents the terrain elevation
                val continuousFieldSampleValue = continuousFieldSample.value

                // Evaluate the aircraft clearance boolean sample function at the sample point
                booleanFieldSampleFunction.evaluate()
                    .onFailure {
                        isSampling = false
                        messageDialogVM.showMessageDialog(it)
                    }.onSuccess { groundClearanceBooleanSample ->
                        // Calculate the clearance of the aircraft above the terrain elevation
                        groundClearance = when (continuousFieldSampleValue) {
                            null -> null
                            else -> _fieldSampleUiState.value.aircraftAltitude - continuousFieldSampleValue
                        }

                        // Display the results of these samples in a Callout
                        sampleElevation = continuousFieldSampleValue
                        groundClearanceStatus = when (groundClearanceBooleanSample.value) {
                            null -> ""
                            true -> "Clear"
                            false -> "Not Clear"
                        }
                        calloutGraphic = aircraftGraphic

                        // Reset the sampling flag to indicate that sampling has completed
                        isSampling = false

                        // Check if the aircraft location has moved on while we sampled at the
                        // given sample point
                        if (samplePoint != aircraftLocationPoint) {
                            // Aircraft location has changed - re-sample at the new location
                            sampleFieldFunctionsAndDisplayCallout(aircraftLocationPoint!!)
                        }
                    }
            }
    }

    /**
     * An AnalysisViewStatus listener that displays the progress indicator when the status of any
     * analysis is Updating and hides it when no analysis is Updating. Also displays details of any
     * error that occurs when an analysis is displayed.
     */
    fun analysisViewStatusListener(event: GeoView.GeoViewAnalysisViewStatusChanged) {
        when (event.analysisViewStatus) {
            is AnalysisViewStatus.Error -> {
                messageDialogVM.showMessageDialog(
                    throwable = (event.analysisViewStatus as AnalysisViewStatus.Error).details
                )
                updatingAnalyses.remove(event.analysis)
            }
            AnalysisViewStatus.UpToDate -> {
                updatingAnalyses.remove(event.analysis)
            }
            AnalysisViewStatus.Updating -> {
                updatingAnalyses.add(event.analysis)
            }
        }
        displayProgressIndicator = updatingAnalyses.isNotEmpty()
    }

    /**
     * Sets a new aircraft altitude.
     */
    fun setAircraftAltitude(aircraftAltitude: Float) {
        // Guard against aircraftLocationPoint not being assigned yet
        val oldAircraftLocationPoint = aircraftLocationPoint ?: return

        // Update variables that are dependent on the aircraft altitude
        aircraftAltitudeFloatSupplier.value = aircraftAltitude
        aircraftLocationPoint = GeometryEngine.createWithZ(
            geometry = oldAircraftLocationPoint,
            z = aircraftAltitude.toDouble()
        )
        _fieldSampleUiState.update { it.copy(aircraftAltitude = aircraftAltitude) }

        // Launch a coroutine to resample the field functions and update the Callout
        viewModelScope.launch {
            sampleFieldFunctionsAndDisplayCallout(aircraftLocationPoint!!)
        }
    }

    /**
     * Sets the aircraft location to the location of the given tap [event].
     */
    fun onTap(event: SingleTapConfirmedEvent) {
        setNewAircraftLocation(event.mapPoint)
    }

    /**
     * Acts on a long press [event] by setting the aircraft location to the location of the long
     * press and allowing it to be dragged across the map.
     */
    fun onLongPress(event: LongPressEvent) {
        aircraftGraphic.isSelected = true
        isDragging = true
        _dragHapticEvents.tryEmit(DragHapticEvent.Start)
        setNewAircraftLocation(event.mapPoint)
    }

    /**
     * Acts on a pan [event]. If the aircraft location is currently being dragged, the new position
     * is set to match the current screen coordinate. Dragging is terminated when panning ends.
     */
    fun onPan(event: PanChangeEvent) {
        if (isDragging) {
            if (event.status == PanStatus.End) {
                aircraftGraphic.isSelected = false
                isDragging = false
                _dragHapticEvents.tryEmit(DragHapticEvent.End)
            }
            setNewAircraftLocation(mapViewProxy.screenToLocationOrNull(event.screenCoordinate))
        }
    }

    /**
     * Sets the aircraft location to the given [mapPoint].
     */
    private fun setNewAircraftLocation(mapPoint: Point?) {
        // Guard against field sample functions not being initialized yet
        if (!::continuousFieldSampleFunction.isInitialized ||
            !::booleanFieldSampleFunction.isInitialized
        ) {
            return
        }
        if (mapPoint != null) {
            // Create a new aircraft location Point
            aircraftLocationPoint = Point(
                x = mapPoint.x,
                y = mapPoint.y,
                z = _fieldSampleUiState.value.aircraftAltitude.toDouble(),
                mapPoint.spatialReference
            )

            // Update the aircraft graphic geometry to the new aircraft location
            aircraftGraphic.geometry = aircraftLocationPoint

            // Dismiss any existing callout
            calloutGraphic = null

            // Launch a coroutine to resample the field functions and display a new Callout
            viewModelScope.launch {
                sampleFieldFunctionsAndDisplayCallout(aircraftLocationPoint!!)
            }
        }
    }
}

data class FieldSampleUiState(
    val aircraftAltitude: Float
) {
    companion object {
        // Initial parameters to drive the UI on launch
        val initialFieldSampleUiState = FieldSampleUiState(
            aircraftAltitude = 2000.0f
        )
    }
}

enum class DragHapticEvent {
    Start,
    End,
}
