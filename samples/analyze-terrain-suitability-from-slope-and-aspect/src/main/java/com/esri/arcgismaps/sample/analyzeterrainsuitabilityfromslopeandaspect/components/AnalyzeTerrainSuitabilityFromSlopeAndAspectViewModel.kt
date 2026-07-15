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

package com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.BooleanFieldFunction
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldFunction
import com.arcgismaps.analysis.interactive.FieldAnalysis
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.symbology.raster.Colormap
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.AnalysisViewStatus
import com.arcgismaps.mapping.view.GeoView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.analyzeterrainsuitabilityfromslopeandaspect.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class AnalyzeTerrainSuitabilityFromSlopeAndAspectViewModel(app: Application) : AndroidViewModel(app) {
    // Create a state flow to hold the UI state for the supporting pane controls
    private val _slopeAspectUiState = MutableStateFlow(SlopeAspectUiState.defaultState)

    // Expose the state flow as read-only for the UI
    val adaptiveUiState = _slopeAspectUiState.asStateFlow()

    // Create a MapViewProxy, used to set viewpoint
    val mapViewProxy = MapViewProxy()

    // Initialize and keep track of the ArcGISMap & the AnalysisOverlay it uses
    val arcGISMap = ArcGISMap(SpatialReference(wkid = 32630)) // UTM30N spatial reference
    var analysisOverlay by mutableStateOf(AnalysisOverlay())

    // Indicates when the progress indicator should be displayed
    var displayProgressIndicator by mutableStateOf(false)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Location of file containing elevation data
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(
            R.string.analyze_terrain_suitability_from_slope_and_aspect_app_name
        ) + File.separator
    }
    private val filePath = provisionPath + "arran.tif"

    // Field functions used in map algebra
    private lateinit var elevationFieldFunction: ContinuousFieldFunction
    private lateinit var slopeFunction: ContinuousFieldFunction
    private lateinit var aspectFunction: ContinuousFieldFunction
    private lateinit var aboveSeaLevelSelection: BooleanFieldFunction

    // FieldAnalysis objects for the two analysis scenarios
    private var shelteredSlopesAnalysis: FieldAnalysis? = null
    private var exposedSlopesAnalysis: FieldAnalysis? = null

    init {
        viewModelScope.launch {
            // Create a ContinuousField from a raster file containing elevation data and project it
            // to the UTM30N spatial reference
            val filePaths = listOf(filePath)
            ContinuousField.createFromFiles(filePaths, band = 0, SpatialReference(wkid = 32630))
                .onFailure {
                    messageDialogVM.showMessageDialog(it)
                }.onSuccess { continuousField ->
                    // Center the MapView on the data we have
                    mapViewProxy.setViewpointCenter(continuousField.extent.center, scale = 200000.0)

                    // Create the continuous field function for the elevation data
                    elevationFieldFunction = ContinuousFieldFunction.create(continuousField)

                    // Derive slope and aspect from the elevation field
                    slopeFunction = elevationFieldFunction.slope()
                    aspectFunction = elevationFieldFunction.aspect()

                    // Keep only land areas above sea level
                    aboveSeaLevelSelection = elevationFieldFunction.isGreaterThanOrEqualTo(0f)

                    // Create FieldAnalysis objects for the 2 scenarios to be shown
                    shelteredSlopesAnalysis = createScenarioAnalysis(
                        slopeMin = 0f, // flat terrain
                        slopeMax = 20f, // moderate slopes
                        aspectStart = 112.5f, // east-south-east facing aspect
                        aspectEnd = 247.5f, // west-south-west facing aspect
                        elevationMin = 0f,
                        elevationMax = 300f, // avoid higher elevations
                        color = Color.fromRgba(r = 0, g = 180, b = 0, a = 255)
                    )
                    exposedSlopesAnalysis = createScenarioAnalysis(
                        slopeMin = 20f, // moderate slopes
                        slopeMax = 80f, // very steep slopes
                        aspectStart = 202.5f, // south-south-west facing aspect
                        aspectEnd = 67.5f, // east-north-east facing aspect
                        elevationMin = 300f,
                        elevationMax = 850f, // higher elevations more exposed
                        color = Color.fromRgba(r = 180, g = 0, b = 180, a = 255)
                    )

                    // Display the progress indicator and make the initially selected scenario
                    // visible; calculation of the analysis starts when it is made visible
                    displayProgressIndicator = true
                    shelteredSlopesAnalysis?.isVisible =
                        _slopeAspectUiState.value.scenarioOption == ScenarioOption.Sheltered
                    exposedSlopesAnalysis?.isVisible =
                        _slopeAspectUiState.value.scenarioOption == ScenarioOption.Exposed
                }
        }
    }

    /**
     * Creates a FieldAnalysis for a given scenario based on slope, aspect, and elevation ranges.
     * The FieldAnalysis is added to the AnalysisOverlay, but its visibility is set false so it
     * won't be displayed yet.
     */
    private fun createScenarioAnalysis(
        slopeMin: Float,
        slopeMax: Float,
        aspectStart: Float,
        aspectEnd: Float,
        elevationMin: Float,
        elevationMax: Float,
        color: Color
    ): FieldAnalysis {
        // Create a BooleanFieldFunction for the scenario
        val scenarioFieldFunction = createScenarioFieldFunction(
            slopeMin = slopeMin,
            slopeMax = slopeMax,
            aspectStart = aspectStart,
            aspectEnd = aspectEnd,
            elevationMin = elevationMin,
            elevationMax = elevationMax
        )

        // Create a colormap and renderer to display the results; white for areas that don't match
        // the scenario results, and the given color for those that do
        val colormap = Colormap.create(
            listOf(Color.white, color)
        )
        val colormapRenderer = ColormapRenderer(colormap)

        // Create the FieldAnalysis, set its visibility to false, and add it to the AnalysisOverlay
        val analysis = FieldAnalysis(scenarioFieldFunction, colormapRenderer)
        analysis.isVisible = false
        analysisOverlay.analyses.add(analysis)
        return analysis
    }

    /**
     * Creates a BooleanFieldFunction for a given scenario based on slope, aspect, and elevation
     * ranges.
     */
    private fun createScenarioFieldFunction(
        slopeMin: Float,
        slopeMax: Float,
        aspectStart: Float,
        aspectEnd: Float,
        elevationMin: Float,
        elevationMax: Float,
    ): BooleanFieldFunction {
        // Create BooleanFieldFunctions for slope, aspect and elevation that assign pixels a value
        // of 1 when within the range of values provided for the scenario, and 0 when outside the
        // range. Note that `and` and `or` functions can be called using the infix notation.
        val slopeRangeMask =
            slopeFunction.isGreaterThanOrEqualTo(slopeMin) and
                    slopeFunction.isLessThanOrEqualTo(slopeMax)

        // Handle the case where the aspect range crosses the 0-degree line (e.g. 225 to 45 degrees)
        val aspectRangeMask =
            if (aspectStart <= aspectEnd)
                aspectFunction.isGreaterThanOrEqualTo(aspectStart) and aspectFunction.isLessThanOrEqualTo(aspectEnd)
            else (aspectFunction.isGreaterThanOrEqualTo(aspectStart) and aspectFunction.isLessThan(360f)) or
                (aspectFunction.isGreaterThanOrEqualTo(0f) and aspectFunction.isLessThanOrEqualTo(aspectEnd))

        val elevationRangeMask =
            elevationFieldFunction.isGreaterThanOrEqualTo(elevationMin) and
                    elevationFieldFunction.isLessThanOrEqualTo(elevationMax)

        // Combine the slope, aspect, and elevation masks with the land-only aboveSeaLevelSelection
        // to create a final BooleanFieldFunction for the scenario
        return (slopeRangeMask and aspectRangeMask and elevationRangeMask).mask(aboveSeaLevelSelection)
    }

    /**
     * An AnalysisViewStatus listener that displays the progress indicator when the status of the
     * current scenario analysis is Updating and hides it when not Updating.
     */
    fun analysisViewStatusListener(event: GeoView.GeoViewAnalysisViewStatusChanged) {
        val currentScenarioAnalysis = when (adaptiveUiState.value.scenarioOption) {
            ScenarioOption.Sheltered -> shelteredSlopesAnalysis
            ScenarioOption.Exposed -> exposedSlopesAnalysis
        }
        if (event.analysis.equals(currentScenarioAnalysis)) {
            displayProgressIndicator = event.analysisViewStatus == AnalysisViewStatus.Updating
        }
    }

    /**
     * Updates the currently selected ScenarioOption.
     */
    fun updateScenarioOption(selectedScenarioOption: ScenarioOption) {
        // Update the UI state
        _slopeAspectUiState.update { currentState ->
            currentState.copy(scenarioOption = selectedScenarioOption)
        }

        // Update visibility of the FieldAnalysis objects, so only the newly selected one is visible
        shelteredSlopesAnalysis?.isVisible =
            _slopeAspectUiState.value.scenarioOption == ScenarioOption.Sheltered
        exposedSlopesAnalysis?.isVisible =
            _slopeAspectUiState.value.scenarioOption == ScenarioOption.Exposed
    }
}

data class SlopeAspectUiState(
    val scenarioOption: ScenarioOption
) {
    companion object {
        val defaultState = SlopeAspectUiState(
            scenarioOption = ScenarioOption.Sheltered
        )
    }
}

enum class ScenarioOption {
    Sheltered, Exposed
}
