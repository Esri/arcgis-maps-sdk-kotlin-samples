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

package com.esri.arcgismaps.sample.applymapalgebra.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.ContinuousField
import com.arcgismaps.analysis.ContinuousFieldFunction
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.raster.ColorRamp
import com.arcgismaps.mapping.symbology.raster.Colormap
import com.arcgismaps.mapping.symbology.raster.ColormapRenderer
import com.arcgismaps.mapping.symbology.raster.StretchRenderer
import com.arcgismaps.mapping.symbology.raster.MinMaxStretchParameters
import com.arcgismaps.mapping.symbology.raster.PresetColorRampType
import com.arcgismaps.raster.Raster
import com.esri.arcgismaps.sample.applymapalgebra.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory

private const val ORIGINAL_ELEVATION_LAYER_NAME = "Original elevation"
private const val MAP_ALGEBRA_RESULTS_LAYER_NAME = "Map algebra results"

/**
 * ViewModel for the Apply map algebra sample.
 *
 * The sample starts by showing the original elevation raster. After the user runs
 * categorization, it adds a results layer and lets the user switch between the two layers.
 */
class ApplyMapAlgebraViewModel(app: Application) : AndroidViewModel(app) {

    // Path where sample data would be provisioned if available.
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path.toString() + File.separator + app.getString(
            R.string.apply_map_algebra_app_name
        )
    }

    // Local raster expected by the sample workflow.
    private val elevationRasterPath = provisionPath + File.separator + "arran.tif"

    // The map displayed by the MapView.
    val arcGISMap =
        ArcGISMap(BasemapStyle.ArcGISHillshadeDark).apply {
            initialViewpoint = Viewpoint(
                55.584612,
                -5.234218,
                500_000.0
            )
        }

    // Controls whether the UI should show the layer switcher.
    var hasMapAlgebraResults by mutableStateOf(false)
        private set

    // UI state for running analysis
    var isPerformingAnalysis by mutableStateOf(false)
        private set

    // Currently selected visible raster layer name.
    var selectedRasterLayerName by mutableStateOf(ORIGINAL_ELEVATION_LAYER_NAME)
        private set

    // Show the results option only after processing creates the output raster.
    val availableLayerNames: List<String>
        get() = if (!hasMapAlgebraResults) {
            listOf(ORIGINAL_ELEVATION_LAYER_NAME)
        } else {
            listOf(ORIGINAL_ELEVATION_LAYER_NAME, MAP_ALGEBRA_RESULTS_LAYER_NAME)
        }

    // Used to surface errors to the Compose UI
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the source elevation raster at startup so users see the original layer first.
        viewModelScope.launch {
            if (File(elevationRasterPath).exists()) {
                val elevationLayer = RasterLayer(Raster.createWithPath(elevationRasterPath)).apply {
                    name = ORIGINAL_ELEVATION_LAYER_NAME
                }
                arcGISMap.operationalLayers += elevationLayer
                selectedRasterLayerName = ORIGINAL_ELEVATION_LAYER_NAME
                elevationLayer.load().onSuccess {
                    // Create a stretch renderer to visualize elevation values
                    val stretchParams = MinMaxStretchParameters(minValues = listOf(0.0), maxValues = listOf(874.0))
                    val colorRamp = ColorRamp.create(PresetColorRampType.Surface, size = 256)
                    val stretchRenderer = StretchRenderer(
                        parameters = stretchParams,
                        gammas = listOf(1.0),
                        estimateStatistics = false,
                        colorRamp = colorRamp
                    )
                    elevationLayer.renderer = stretchRenderer
                    elevationLayer.opacity = 0.5f
                }.onFailure { throwable ->
                    messageDialogVM.showMessageDialog(
                        title = "Failed to load elevation raster",
                        description = throwable.message.toString()
                    )
                }
            } else {
                messageDialogVM.showMessageDialog(
                    title = "Elevation raster not found",
                    description = "Place arran.tif into the sample's provisioned folder: $provisionPath"
                )
            }

            // Load the map
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Runs map algebra on the source elevation raster and creates the
     * "Map algebra results" layer used by the sample's layer switcher.
     */
    fun categorizeElevation() {
        // Ensure we have a raster to analyze
        if (!File(elevationRasterPath).exists()) {
            messageDialogVM.showMessageDialog(
                title = "Elevation raster not found",
                description = "Place arran.tif into the sample's provisioned folder: $provisionPath"
            )
            return
        }

        viewModelScope.launch {
            isPerformingAnalysis = true
            try {
                // Build a continuous field from the source elevation raster.
                val elevationField = ContinuousField.createFromFiles(
                    filePaths = listOf(elevationRasterPath),
                    band = 0
                ).getOrThrow()

                // Mask out values below sea level before classifying the terrain.
                val continuousFieldFunction = ContinuousFieldFunction.create(elevationField)
                val elevationFieldFunction = continuousFieldFunction.mask(
                    selection = continuousFieldFunction.isGreaterThanOrEqualTo(0.0f)
                )

                // Group the elevation values into 10-meter bins.
                val tenMeterBin = ((elevationFieldFunction / 10f).floor() * 10f).toDiscreteFieldFunction()

                // Build the three geomorphological categories used by the sample.
                val isRaisedShoreline = tenMeterBin.isGreaterThanOrEqualTo(0) and tenMeterBin.isLessThan(10)
                val isIceCovered = tenMeterBin.isGreaterThanOrEqualTo(10) and tenMeterBin.isLessThan(600)
                val isIceFreeHighGround = tenMeterBin.isGreaterThanOrEqualTo(600)

                // Replace each matching range with a category value and evaluate the output raster.
                val geomorphicFn = tenMeterBin
                    .replaceIf(isRaisedShoreline, 1)
                    .replaceIf(isIceCovered, 2)
                    .replaceIf(isIceFreeHighGround, 3)
                val discreteField = geomorphicFn.evaluate().getOrThrow()

                // Export the processed raster and load it back as a RasterLayer.
                val exportedFiles = discreteField.exportToFiles(
                    createTempDirectory().absolutePathString(),
                    "geomorphicCategorization"
                ).getOrThrow()
                val resultRaster = Raster.createWithPath(exportedFiles.first())

                // Apply a 3-color palette to emulate categorized output in this template.
                val colormap = Colormap.create(
                    mapOf(
                        1 to Color.fromRgba(82, 158, 235, 255), // Raised shoreline - blue
                        2 to Color.fromRgba(102, 204, 204, 255), // Ice covered - teal
                        3 to Color.fromRgba(140, 100, 65, 255), // Ice-free high ground - brown
                    )
                )
                val colormapRenderer = ColormapRenderer(colormap)

                // Create a RasterLayer from the result Raster and apply the colormap renderer
                val resultLayer = RasterLayer(resultRaster).apply {
                    name = MAP_ALGEBRA_RESULTS_LAYER_NAME
                    renderer = colormapRenderer
                    opacity = 0.5f
                }

                // Load the result layer and add to the map's operational layers.
                resultLayer.load().onSuccess {
                    arcGISMap.operationalLayers += resultLayer
                    hasMapAlgebraResults = true
                    selectRasterLayer(MAP_ALGEBRA_RESULTS_LAYER_NAME)
                }.onFailure { throwable ->
                    messageDialogVM.showMessageDialog(
                        title = "Error creating results",
                        description = throwable.message.toString()
                    )
                }
            } catch (throwable: Throwable) {
                messageDialogVM.showMessageDialog(
                    title = "Error during analysis",
                    description = throwable.message.toString()
                )
            }
            finally {
                isPerformingAnalysis = false
            }
        }
    }

    /**
     * Helper to toggle visibility between the original elevation raster and the results raster.
     */
    fun selectRasterLayer(layerName: String) {
        arcGISMap.operationalLayers
            .filterIsInstance<RasterLayer>()
            .forEach { layer ->
                val isSelected = layer.name == layerName
                layer.isVisible = isSelected
                if (isSelected) selectedRasterLayerName = layerName
            }
    }
}
