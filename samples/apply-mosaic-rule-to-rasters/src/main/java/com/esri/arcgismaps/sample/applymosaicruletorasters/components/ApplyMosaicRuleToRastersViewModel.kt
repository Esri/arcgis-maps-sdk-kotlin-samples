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

package com.esri.arcgismaps.sample.applymosaicruletorasters.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.raster.ImageServiceRaster
import com.arcgismaps.raster.MosaicMethod
import com.arcgismaps.raster.MosaicOperation
import com.arcgismaps.raster.MosaicRule
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Apply Mosaic Rule to Rasters sample.
 */
class ApplyMosaicRuleToRastersViewModel(app: Application) : AndroidViewModel(app) {

    // The map used in the sample, with a topographic basemap.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)

    // The ImageServiceRaster from the raster image service (Amberg, Germany).
    private val imageServiceRaster = ImageServiceRaster(
        url = "https://sampleserver7.arcgisonline.com/server/rest/services/amberg_germany/ImageServer"
    )

    // The RasterLayer that displays the ImageServiceRaster on the map.
    private val rasterLayer = RasterLayer(imageServiceRaster)

    // The list of available mosaic rule types for the dropdown menu.
    val mosaicRuleTypes = MosaicRuleType.entries

    // The currently selected mosaic rule type.
    private val _selectedRuleType = MutableStateFlow(MosaicRuleType.ObjectID)
    val selectedRuleType = _selectedRuleType.asStateFlow()

    // Loading state for the UI, true while the raster layer is loading.
    var isLoading by mutableStateOf(true)
        private set

    // Message dialog for error handling.
    val messageDialogVM = MessageDialogViewModel()

    // Center point of the raster service (Amberg, Germany), used for recentering the map.
    private val imageServiceRasterCenter: Point
        get() {
            return imageServiceRaster.serviceInfo?.fullExtent?.extent?.center ?: Point(
                x = 0.0,
                y = 0.0
            )
        }

    // MapViewProxy for controlling the viewpoint after raster is loaded.
    val mapViewProxy = MapViewProxy()

    init {
        // Add the raster layer to the map's operational layers.
        arcGISMap.operationalLayers.add(rasterLayer)
        // Set the initial mosaic rule (ObjectID/None).
        imageServiceRaster.mosaicRule = createMosaicRule(MosaicRuleType.ObjectID)
        updateMosaicRule(_selectedRuleType.value)
    }

    /**
     * Updates the mosaic rule on the raster layer to the selected [type].
     */
    fun updateMosaicRule(type: MosaicRuleType) {
        _selectedRuleType.value = type
        isLoading = true
        // Set the new mosaic rule on the image service raster.
        imageServiceRaster.mosaicRule = createMosaicRule(type)
        // Reload the raster layer and update the center point and viewpoint.
        viewModelScope.launch {
            rasterLayer.load().onSuccess {
                mapViewProxy.setViewpointAnimated(
                    Viewpoint(center = imageServiceRasterCenter, scale = 25000.0)
                )
                isLoading = false
            }.onFailure {
                isLoading = false
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    /**
     * Helper function to create a [MosaicRule] instance for the given [mosaicRuleType].
     */
    private fun createMosaicRule(mosaicRuleType: MosaicRuleType): MosaicRule {
        return MosaicRule().apply {
            when (mosaicRuleType) {
                MosaicRuleType.ObjectID -> {
                    // Default mosaic method.
                    mosaicMethod = MosaicMethod.None
                }

                MosaicRuleType.NorthWest -> {
                    // Sorts rasters by northwest location, uses 'first' operation.
                    mosaicMethod = MosaicMethod.Northwest
                    mosaicOperation = MosaicOperation.First
                }

                MosaicRuleType.Center -> {
                    // Sorts rasters by proximity to center, uses 'blend' operation.
                    mosaicMethod = MosaicMethod.Center
                    mosaicOperation = MosaicOperation.Blend
                }

                MosaicRuleType.ByAttribute -> {
                    // Sorts rasters by the OBJECTID attribute.
                    mosaicMethod = MosaicMethod.Attribute
                    sortField = "OBJECTID"
                }

                MosaicRuleType.LockRaster -> {
                    // Locks the mosaic to specific raster IDs.
                    mosaicMethod = MosaicMethod.LockRaster
                    lockRasterIds.addAll(listOf(1, 7, 12))
                }
            }
        }
    }
}

/**
 * Enum representing the available preset mosaic rule types for the sample.
 */
enum class MosaicRuleType(val label: String) {
    ObjectID("Object ID"),
    NorthWest("North West"),
    Center("Center"),
    ByAttribute("By Attribute"),
    LockRaster("Lock Raster")
}
