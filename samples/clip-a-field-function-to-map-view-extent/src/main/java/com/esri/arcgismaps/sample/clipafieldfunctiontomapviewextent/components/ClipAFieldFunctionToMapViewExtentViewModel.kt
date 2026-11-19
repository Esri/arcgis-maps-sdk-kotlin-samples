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

package com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldClipFunction
import com.arcgismaps.analysis.ContinuousFieldSupplier
import com.arcgismaps.analysis.FloatSupplier
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.raster.ColorRamp
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PresetColorRampType
import com.arcgismaps.mapping.symbology.raster.StretchRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AnalysisViewStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.R
import com.esri.arcgismaps.sample.clipafieldfunctiontomapviewextent.components.ClipFunctionUiState.Companion.initialClipFunctionUiState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

const val CLIP_VISIBLE_EXTENT_FRACTION = 0.6
const val MINIMUM_ELEVATION_METERS = 0.0f
const val MAXIMUM_ELEVATION_METERS = 874.0f

class ClipAFieldFunctionToMapViewExtentViewModel(app: Application) : AndroidViewModel(app) {
    // Create a state flow to hold the UI state for the supporting pane controls
    private val _clipFunctionUiState = MutableStateFlow(initialClipFunctionUiState)

    // Expose the state flow as read-only for the UI
    val uiState = _clipFunctionUiState.asStateFlow()

    // Create a MapViewProxy, used to set viewpoint
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the overlays it uses
    val arcGISMap by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISTopographic))
    val analysisOverlay by mutableStateOf(AnalysisOverlay())
    val graphicsOverlay by mutableStateOf(GraphicsOverlay())

    // Indicates when the ProgressIndicator should be displayed
    var displayProgressIndicator by mutableStateOf(false)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.clip_a_field_function_to_map_view_extent_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + "arran.tif"

    // Elevation range suppliers
    private val minimumElevationSupplier = FloatSupplier(MINIMUM_ELEVATION_METERS)
    private val maximumElevationSupplier = FloatSupplier(MAXIMUM_ELEVATION_METERS)

    // Variables that are initialized in the init block and referenced in event listener(s)
    private lateinit var elevationField: ContinuousField
    private lateinit var continuousFieldClipFunction: ContinuousFieldClipFunction
    private lateinit var fieldAnalysis: FieldAnalysis

    // Stretch renderer for visualizing the elevation
    private val stretchRenderer = StretchRenderer(
        parameters = MinMaxStretchParameters(
            minValues = listOf(MINIMUM_ELEVATION_METERS.toDouble()),
            maxValues = listOf(MAXIMUM_ELEVATION_METERS.toDouble())
        ),
        gammas = listOf(1.0),
        estimateStatistics = false,
        colorRamp = ColorRamp.create(
            type = PresetColorRampType.Surface,
            size = 256
        )
    )

    // Graphic used to show the clip extent
    private val clipExtentGraphic = Graphic(
        symbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Null,
            outline = SimpleLineSymbol(
                color = Color.fromRgba(r = 78, g = 50, b = 50, a = 255),
                width = 1f
            )
        )
    )

    init {
        // Add clip extent graphic to the graphics overlay
        graphicsOverlay.graphics.add(clipExtentGraphic)

        viewModelScope.launch {
            // Load the map and handle any errors by showing a message dialog
            arcGISMap.load().getOrElse { error ->
                messageDialogVM.showMessageDialog(error)
                return@launch
            }

            // Create a ContinuousField from a raster file containing elevation data
            elevationField = ContinuousField.createFromFiles(filePaths = listOf(filePath), band = 0)
                .getOrElse { error ->
                    messageDialogVM.showMessageDialog(error)
                    return@launch
                }

            // Create a continuous field function from the elevation field
            val continuousFieldFunction = ContinuousFieldSupplier(elevationField)

            // Create a boolean field function for the elevation range with map algebra
            val elevationSelection =
                continuousFieldFunction.isGreaterThanOrEqualTo(minimumElevationSupplier) and
                        continuousFieldFunction.isLessThanOrEqualTo(maximumElevationSupplier)

            // Mask the elevation field using the boolean field function to create the masked
            // elevation function
            val maskedElevationFunction =
                continuousFieldFunction.mask(elevationSelection)

            // Create the continuous field clip function using the masked elevation function. Use
            // null initially for the extent, which will be updated later based on the map view
            // visible area.
            continuousFieldClipFunction = ContinuousFieldClipFunction(
                field = maskedElevationFunction,
                extent = null
            )

            // Create the field analysis using the continuous field clip function and add it to the
            // analysis overlay
            fieldAnalysis =
                FieldAnalysis(continuousFieldClipFunction, stretchRenderer)
            fieldAnalysis.opacity = 0.7f
            fieldAnalysis.isVisible = false // made visible in onVisibleAreaChanged()
            analysisOverlay.analyses.add(fieldAnalysis)

            // Set the viewpoint of the map to the center of the elevation field's extent
            mapViewProxy.setViewpointCenter(
                center = elevationField.extent.center,
                scale = 150000.0
            )
        }
    }

    /**
     * An AnalysisViewStatus listener that displays the progress indicator when the status of the
     * analysis is Updating and hides it otherwise. Also displays details of any error that occurs
     * when the analysis is displayed.
     */
    fun analysisViewStatusListener(event: GeoView.GeoViewAnalysisViewStatusChanged) {
        when (val status = event.analysisViewStatus) {
            is AnalysisViewStatus.Error -> {
                messageDialogVM.showMessageDialog(
                    throwable = status.details
                )
                displayProgressIndicator = false
            }
            AnalysisViewStatus.UpToDate -> {
                displayProgressIndicator = false
            }
            AnalysisViewStatus.Updating -> {
                displayProgressIndicator = true
            }
        }
    }

    /**
     * Updates the minimum elevation and maximum elevation values.
     */
    fun onMinMaxElevationChange(minimumElevation: Float, maximumElevation: Float) {
        _clipFunctionUiState.update {
            it.copy(minimumElevation = minimumElevation, maximumElevation = maximumElevation)
        }
        minimumElevationSupplier.value = minimumElevation
        maximumElevationSupplier.value = maximumElevation
    }

    /**
     * Listener called when the visible area of the MapView has changed. Calculates a clip geometry
     * based on the new [visibleArea], and uses it to update continuousFieldClipFunction's extent
     * and the graphic that's used to show the current clip extent.
     */
    fun onVisibleAreaChanged(visibleArea: Polygon) {
        // Just return if initialization not complete yet
        if (!::fieldAnalysis.isInitialized) {
            return
        }

        // Build the clip geometry based on the new visible area extent
        val clipGeometry = buildClipGeometry(visibleArea.extent)

        // If no overlap between the visible area and the elevation field, hide the analysis and the
        // clip extent graphic, so stale results are not displayed
        if (clipGeometry == null) {
            clipExtentGraphic.isVisible = false
            fieldAnalysis.isVisible = false
            displayProgressIndicator = false
            return
        }

        // Update the graphic used to show the current clip extent
        clipExtentGraphic.geometry = clipGeometry

        // Update the clip function's extent based on the new clip geometry
        continuousFieldClipFunction.extent = clipGeometry.extent
        fieldAnalysis.isVisible = true
        clipExtentGraphic.isVisible = true
    }

    /**
     * Calculates a clip geometry based on the given [visibleExtent]. Returns the intersection
     * between that and the extent of the elevation field, or null if there is no intersection.
     */
    private fun buildClipGeometry(visibleExtent: Envelope) : Geometry? {
        // Get the extent of the elevation field
        val elevationFieldExtent = elevationField.extent

        // Generate an inset extent based on the visible extent and the clip fraction
        val insetExtent = Envelope(
            center = visibleExtent.center,
            width = visibleExtent.width * CLIP_VISIBLE_EXTENT_FRACTION,
            height = visibleExtent.height * CLIP_VISIBLE_EXTENT_FRACTION
        )

        // Ensure the inset extent is within the elevation field's extent
        val intersection = GeometryEngine.intersectionOrNull(
            geometry1 = insetExtent,
            geometry2 = elevationFieldExtent
        )
        return when {
            intersection == null -> null
            intersection.isEmpty -> null
            else -> intersection
        }
    }
}

data class ClipFunctionUiState(
    val minimumElevation: Float,
    val maximumElevation: Float
) {
    companion object {
        // Initial parameters to drive the UI on launch
        val initialClipFunctionUiState = ClipFunctionUiState(
            minimumElevation = MINIMUM_ELEVATION_METERS,
            maximumElevation = MAXIMUM_ELEVATION_METERS
        )
    }
}
