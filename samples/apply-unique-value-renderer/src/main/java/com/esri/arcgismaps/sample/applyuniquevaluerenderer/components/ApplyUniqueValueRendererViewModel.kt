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

package com.esri.arcgismaps.sample.applyuniquevaluerenderer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the ApplyUniqueValueRenderer sample.
 *
 * This ViewModel builds an ArcGISMap, adds a FeatureLayer from a service, and
 * applies a UniqueValueRenderer based on the SUB_REGION field. The map and
 * layer are created during initialization and loaded in initialization. Errors are
 * reported through message dialog.
 */
class ApplyUniqueValueRendererViewModel(application: Application) : AndroidViewModel(application) {

    // Create the map with a Topographic basemap and an initial viewpoint
    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            // Center and scale the map to show the US
            initialViewpoint = Viewpoint(
                center = Point(
                    x = -12356253.6,
                    y = 3842795.4,
                    spatialReference = SpatialReference.webMercator()
                ),
                scale = 52681563.2
            )
    }


    // Service feature table for the U.S. states (subregions)
    private val censusFeatureTable = ServiceFeatureTable(
        uri = "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Census/MapServer/3"
    )

    // Feature layer created from the service feature table
    private val statesFeatureLayer = FeatureLayer.createWithFeatureTable(censusFeatureTable)

    // Message dialog helper for presenting errors to the user
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Configure the renderer and add the layer to the map up-front, but load operations run in coroutine
        configureUniqueValueRenderer()

        // Add the feature layer to the map's operational layers
        arcGISMap.operationalLayers.add(statesFeatureLayer)

        // Load the map and the feature layer, report failures via the message dialog
        viewModelScope.launch {
            arcGISMap.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = throwable.message.toString()
                )
            }
        }
    }

    // Build and apply a UniqueValueRenderer to the statesFeatureLayer
    private fun configureUniqueValueRenderer() {
        // Outline used for all region fill symbols
        val stateOutline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.white,
            width = 0.7f
        )

        // Region fill symbols
        val pacificFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(4, 122, 255, 255), // blue
            outline = stateOutline
        )

        val mountainFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.green,
            outline = stateOutline
        )

        val westSouthCentralFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(165, 90, 0, 255), // brown-like
            outline = stateOutline
        )

        // Unique values for the renderer, based on SUB_REGION values
        val pacificValue = UniqueValue(
            description = "Pacific Region",
            label = "Pacific",
            symbol = pacificFill,
            values = listOf("Pacific")
        )

        val mountainValue = UniqueValue(
            description = "Rocky Mountain Region",
            label = "Mountain",
            symbol = mountainFill,
            values = listOf("Mountain")
        )

        val westSouthCentralValue = UniqueValue(
            description = "West South Central Region",
            label = "West South Central",
            symbol = westSouthCentralFill,
            values = listOf("West South Central")
        )

        // Default symbol for any other regions not explicitly defined
        val defaultFill = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Cross,
            color = Color.fromRgba(200, 200, 200, 255),
            outline = stateOutline
        )

        // Create the renderer and apply it to the feature layer
        val uniqueValueRenderer = UniqueValueRenderer(
            fieldNames = listOf("SUB_REGION"),
            uniqueValues = listOf(pacificValue, mountainValue, westSouthCentralValue),
            defaultLabel = "Other",
            defaultSymbol = defaultFill
        )

        statesFeatureLayer.renderer = uniqueValueRenderer
    }
}
