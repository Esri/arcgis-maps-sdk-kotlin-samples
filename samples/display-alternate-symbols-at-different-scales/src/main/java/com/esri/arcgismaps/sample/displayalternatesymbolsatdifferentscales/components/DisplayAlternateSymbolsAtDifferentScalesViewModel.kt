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

package com.esri.arcgismaps.sample.displayalternatesymbolsatdifferentscales.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SymbolReferenceProperties
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the sample demonstrating alternate symbols at different scales.
 */
class DisplayAlternateSymbolsAtDifferentScalesViewModel(app: Application) : AndroidViewModel(app) {

    // Feature layer created from the service, using URL for the SF311 incidents feature layer
    private val featureLayer = FeatureLayer.createWithFeatureTable(
        featureTable = ServiceFeatureTable(
            uri = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/SF311/FeatureServer/0"
        )
    )

    // Map initialized to the a viewpoint and feature layer.
    val arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        val center = Point(
            x = -13631200.0,
            y = 4546830.0,
            spatialReference = com.arcgismaps.geometry.SpatialReference.webMercator()
        )
        initialViewpoint = Viewpoint(center = center, scale = 7500.0)
        // Add the incidents feature layer
        operationalLayers += featureLayer
    }

    // MapViewProxy to perform viewpoint changes
    val mapViewProxy = MapViewProxy()

    // Expose the current MapView scale
    private val _currentScale = MutableStateFlow(arcGISMap.initialViewpoint?.targetScale)
    val currentScale = _currentScale.asStateFlow()

    // Message dialog helper for errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Load the map with the feature layer
            arcGISMap.load().onFailure { return@launch messageDialogVM.showMessageDialog(it) }
            // Configure the unique value renderer
            configureUniqueValueRenderer()
        }
    }

    /**
     * Build the multilayer symbols with reference scale ranges and apply a [UniqueValueRenderer]
     * to the feature layer. Alternate symbols are assigned to scale ranges.
     */
    private fun configureUniqueValueRenderer() {
        // Red triangle used for the base symbol (0 - 5,000)
        val redTriangle = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Triangle,
            color = Color.red,
            size = 30f
        ).toMultilayerSymbol().apply {
            referenceProperties = SymbolReferenceProperties(
                minScale = 5_000.0,
                maxScale = 0.0
            )
        }

        // Blue square alternate symbol (5,000 - 10,000)
        val blueSquare = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Square,
            color = Color.blue,
            size = 30f
        ).toMultilayerSymbol().apply {
            referenceProperties = SymbolReferenceProperties(
                minScale = 10_000.0,
                maxScale = 5_000.0
            )
        }

        // Yellow diamond alternate symbol (10,000 - 20,000)
        val yellowDiamond = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Diamond,
            color = Color.yellow,
            size = 30f
        ).toMultilayerSymbol().apply {
            referenceProperties = SymbolReferenceProperties(
                minScale = 20_000.0,
                maxScale = 10_000.0
            )
        }

        // Unique value using the red triangle and the alternate symbols
        val uniqueValue = UniqueValue(
            description = "unique values based on request type",
            label = "unique value",
            symbol = redTriangle,
            values = listOf("Damaged Property"),
            alternateSymbols = listOf(blueSquare, yellowDiamond)
        )

        // Default purple diamond for other values
        val purpleDiamond = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Diamond,
            color = Color.fromRgba(r = 128, g = 0, b = 128),
            size = 15f
        ).toMultilayerSymbol()

        val uniqueValueRenderer = UniqueValueRenderer(
            fieldNames = listOf("req_type"),
            uniqueValues = listOf(uniqueValue),
            defaultSymbol = purpleDiamond
        )

        // Set the renderer to symbolize geo-elements with a distinct symbol.
        featureLayer.renderer = uniqueValueRenderer
    }

    /**
     * Called from the MapView composable when the viewpoint changes.
     */
    fun updateScale(newScale: Double?) {
        _currentScale.value = newScale
    }

    /**
     * Reset the viewpoint to the map's initial viewpoint using MapViewProxy.
     */
    fun resetViewpoint() {
        val initialViewpoint = arcGISMap.initialViewpoint ?: return
        viewModelScope.launch {
            mapViewProxy.setViewpointAnimated(initialViewpoint)
        }
    }
}

// Provide a blue color companion for convenience.
private val Color.Companion.blue: Color
    get() = fromRgba(0, 0, 255, 255)
