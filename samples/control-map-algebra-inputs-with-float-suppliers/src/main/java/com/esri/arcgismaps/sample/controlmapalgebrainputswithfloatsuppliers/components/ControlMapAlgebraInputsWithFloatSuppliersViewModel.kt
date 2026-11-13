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

package com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldSupplier
import com.arcgismaps.analysis.FloatSupplier
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.symbology.raster.ColorRamp
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PresetColorRampType
import com.arcgismaps.mapping.symbology.raster.StretchRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AnalysisViewStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.R
import com.esri.arcgismaps.sample.controlmapalgebrainputswithfloatsuppliers.components.FloatSuppliersUiState.Companion.initialFloatSuppliersUiState
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

const val MAXIMUM_SLOPE_DEGREES = 85.0

class ControlMapAlgebraInputsWithFloatSuppliersViewModel(app: Application) : AndroidViewModel(app) {
    // Create a state flow to hold the UI state for the supporting pane controls
    private val _floatSuppliersUiState = MutableStateFlow(initialFloatSuppliersUiState)

    // Expose the state flow as read-only for the UI
    val uiState = _floatSuppliersUiState.asStateFlow()

    // Create the spatial reference for the map and the elevation file (UTM zone 32N).
    val spatialReference = SpatialReference(wkid = 32632)

    // Create a MapViewProxy, used to set viewpoint & convert screen point to location
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the analysis overlay it uses
    val arcGISMap by mutableStateOf(ArcGISMap(spatialReference))
    var analysisOverlay by mutableStateOf(AnalysisOverlay())

    // Indicates when the ProgressIndicator should be displayed
    var displayProgressIndicator by mutableStateOf(false)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.control_map_algebra_inputs_with_float_suppliers_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + "Meribel_32N.tif"

    // Create slope range suppliers, initialized with values of 0 to 10 degrees; this will highlight
    // gentle hills and flatter areas such as roads and man-made structures
    val minimumSlopeFloatSupplier = FloatSupplier(initialFloatSuppliersUiState.minimumSlope)
    val maximumSlopeFloatSupplier = FloatSupplier(initialFloatSuppliersUiState.maximumSlope)

    // Stretch renderer for visualizing the slope, using a fixed degree scale as the selected slope
    // range changes
    private val slopeRenderer = StretchRenderer(
        parameters = MinMaxStretchParameters(
            minValues = listOf(0.0),
            maxValues = listOf(MAXIMUM_SLOPE_DEGREES)
        ),
        gammas = listOf(1.0),
        estimateStatistics = false,
        colorRamp = ColorRamp.create(
            type = PresetColorRampType.Viridis,
            size = 256
        )
    )

    // Stretch renderer for visualizing the elevation
    private val elevationRenderer = StretchRenderer(
        parameters = MinMaxStretchParameters(
            minValues = listOf(1199.0),
            maxValues = listOf(2740.0)
        ),
        gammas = listOf(1.0),
        estimateStatistics = false,
        colorRamp = ColorRamp.create(
            type = PresetColorRampType.Surface,
            size = 256
        )
    )

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure {
                messageDialogVM.showMessageDialog(it)
            }.onSuccess {
                // Create a ContinuousField from a raster file containing elevation data
                ContinuousField.createFromFiles(
                    filePaths = listOf(filePath),
                    band = 0,
                    spatialReference = spatialReference
                ).onFailure {
                    messageDialogVM.showMessageDialog(it)
                }.onSuccess { continuousField ->
                    // Create the elevation function from the continuous field
                    val elevationFunction = ContinuousFieldSupplier(continuousField)

                    // Create the slope function from the elevation function
                    val slopeFunction = elevationFunction.slope()

                    // Create a boolean field function based on the minimum and maximum slope suppliers
                    val slopeSelection =
                        slopeFunction.isGreaterThanOrEqualTo(minimumSlopeFloatSupplier) and
                                slopeFunction.isLessThanOrEqualTo(maximumSlopeFloatSupplier)

                    // Mask the slope function with the selected slope range to create a continuous
                    // field function
                    val maskedSlope = slopeFunction.mask(slopeSelection)

                    // Set the initial viewpoint of the map to the center of the continuous field's extent
                    mapViewProxy.setViewpointCenter(
                        center = continuousField.extent.center,
                        scale = 10000.0
                    )

                    // Create the elevation field analysis using the elevation function.
                    // Note: This acts as a basemap for the slope analysis in this sample.
                    val elevationFieldAnalysis = FieldAnalysis(elevationFunction, elevationRenderer)
                    elevationFieldAnalysis.opacity = 0.6f

                    // Add the elevation field analysis to the analysis overlay
                    analysisOverlay.analyses.add(elevationFieldAnalysis)

                    // Create the slope field analysis using the masked slope field function
                    val slopeFieldAnalysis = FieldAnalysis(maskedSlope, slopeRenderer)

                    // Add the slope field analysis to the analysis overlay
                    analysisOverlay.analyses.add(slopeFieldAnalysis)
                }
            }
        }
    }

    /**
     * An AnalysisViewStatus listener that displays the progress indicator when the status of the
     * analysis is Updating and hides it otherwise. Also displays details of any error that occurs
     * when the analysis is displayed.
     */
    fun analysisViewStatusListener(event: GeoView.GeoViewAnalysisViewStatusChanged) {
        when (event.analysisViewStatus) {
            is AnalysisViewStatus.Error -> {
                messageDialogVM.showMessageDialog(
                    throwable = (event.analysisViewStatus as AnalysisViewStatus.Error).details
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
     * Updates the minimum slope and maximum slope values.
     */
    fun onMinMaxSlopeChange(minimumSlope: Float, maximumSlope: Float) {
        _floatSuppliersUiState.update {
            it.copy(minimumSlope = minimumSlope, maximumSlope = maximumSlope)
        }
        minimumSlopeFloatSupplier.value = minimumSlope
        maximumSlopeFloatSupplier.value = maximumSlope
    }
}

data class FloatSuppliersUiState(
    val minimumSlope: Float,
    val maximumSlope: Float
) {
    companion object {
        // Initial parameters to drive the UI on launch
        val initialFloatSuppliersUiState = FloatSuppliersUiState(
            minimumSlope = 0.0f,
            maximumSlope = 10.0f
        )
    }
}
