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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val IMAGE_SERVICE_URL =
    "https://sampleserver7.arcgisonline.com/server/rest/services/amberg_germany/ImageServer"

enum class MosaicRuleType(val label: String) {
    ObjectID("Object ID"),
    NorthWest("North West"),
    Center("Center"),
    ByAttribute("By Attribute"),
    LockRaster("Lock Raster")
}

class ApplyMosaicRuleToRastersViewModel(app: Application) : AndroidViewModel(app) {
    // The ArcGISMap for the sample
    var arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = Viewpoint(
                center = Point(
                    4482515.0,
                    5411935.0
                ), scale = 25000.0
            )
        }
    )

    // The ImageServiceRaster
    private val imageServiceRaster = ImageServiceRaster(IMAGE_SERVICE_URL)

    // The RasterLayer containing the ImageServiceRaster
    private val rasterLayer = RasterLayer(imageServiceRaster)

    // The available mosaic rule types
    val mosaicRuleTypes = MosaicRuleType.entries

    // The currently selected mosaic rule type
    private val _selectedRuleType = MutableStateFlow(MosaicRuleType.ObjectID)
    val selectedRuleType = _selectedRuleType.asStateFlow()

    // Loading state for UI
    var isLoading by mutableStateOf(true)
        private set

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    // Center point of the raster service (Amberg, Germany)
    private var imageServiceRasterCenter: Point = Point(x = 0.0, y = 0.0)

    init {
        // Add the raster layer to the map
        arcGISMap.operationalLayers.add(rasterLayer)
        // Set initial mosaic rule
        imageServiceRaster.mosaicRule = createMosaicRule(MosaicRuleType.ObjectID)
        // Load the raster layer and get the center
        viewModelScope.launch {
            rasterLayer.load().onSuccess {
                imageServiceRaster.serviceInfo?.fullExtent?.center?.let { center ->
                    imageServiceRasterCenter = center
                }
                isLoading = false
            }.onFailure {
                isLoading = false
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    fun updateMosaicRule(type: MosaicRuleType) {
        _selectedRuleType.value = type
        isLoading = true
        imageServiceRaster.mosaicRule = createMosaicRule(type)
        // Optionally, recenter the map
        viewModelScope.launch {
            rasterLayer.load().onSuccess {
                imageServiceRaster.serviceInfo?.fullExtent?.center?.let { center ->
                    imageServiceRasterCenter = center
                }
                isLoading = false
            }.onFailure {
                isLoading = false
                messageDialogVM.showMessageDialog(it)
            }
        }
    }

    fun getRasterCenter(): Point = imageServiceRasterCenter

    private fun createMosaicRule(type: MosaicRuleType): MosaicRule {
        return MosaicRule().apply {
            when (type) {
                MosaicRuleType.ObjectID -> {
                    mosaicMethod = MosaicMethod.None
                }

                MosaicRuleType.NorthWest -> {
                    mosaicMethod = MosaicMethod.Northwest
                    mosaicOperation = MosaicOperation.First
                }

                MosaicRuleType.Center -> {
                    mosaicMethod = MosaicMethod.Center
                    mosaicOperation = MosaicOperation.Blend
                }

                MosaicRuleType.ByAttribute -> {
                    mosaicMethod = MosaicMethod.Attribute
                    sortField = "OBJECTID"
                }

                MosaicRuleType.LockRaster -> {
                    mosaicMethod = MosaicMethod.LockRaster
                    lockRasterIds.addAll(listOf(1, 7, 12))
                }
            }
        }
    }
}
