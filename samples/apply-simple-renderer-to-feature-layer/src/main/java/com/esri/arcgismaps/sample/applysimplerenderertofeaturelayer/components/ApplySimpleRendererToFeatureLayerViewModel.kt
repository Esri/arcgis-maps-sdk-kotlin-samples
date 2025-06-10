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

package com.esri.arcgismaps.sample.applysimplerenderertofeaturelayer.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.portal.Portal
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class ApplySimpleRendererToFeatureLayerViewModel(app: Application) : AndroidViewModel(app) {
    // The ArcGISMap instance with the basemap and initial viewpoint
    var arcGISMap =
        ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
            initialViewpoint = Viewpoint(latitude = 35.61, longitude = -82.44, scale = 1e4)
        }

    // The FeatureLayer to which we will apply the renderer
    var featureLayer: FeatureLayer? = null

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            // Create the feature layer from the portal item
            val portalItem = PortalItem(
                portal = Portal.arcGISOnline(connection = Portal.Connection.Anonymous),
                itemId = "6d41340931544829acc8f68c27e69dec"
            )
            val layer = FeatureLayer.createWithItem(portalItem)
            featureLayer = layer
            arcGISMap.operationalLayers.add(layer)
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Applies a simple renderer with a random color to the feature layer.
     */
    fun applyRandomSimpleRenderer() {
        featureLayer?.let { layer ->
            // Pick a random color from the list
            val colorList = listOf(Color.red, Color.yellow, Color.blue, Color.green)
            val randomColor = colorList[Random.nextInt(colorList.size)]
            // Create a simple marker symbol
            val symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Circle,
                color = randomColor,
                size = 10f
            )
            // Create and apply the simple renderer
            val renderer = SimpleRenderer(symbol)
            layer.renderer = renderer
        }
    }
}

// Extension property to provide blue color for ArcGISMaps Color class
val Color.Companion.blue: Color
    get() = fromRgba(0, 0, 255, 255)
