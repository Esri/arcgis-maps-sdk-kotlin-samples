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

package com.esri.arcgismaps.sample.applysimplerenderertographicsoverlay.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ApplySimpleRendererToGraphicsOverlayViewModel(app: Application) : AndroidViewModel(app) {
    // ArcGISMap centered on Yellowstone National Park
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISImagery).apply {
        initialViewpoint = Viewpoint(latitude = 44.462, longitude = -110.829, scale = 1e4)
    }

    // GraphicsOverlay with a simple renderer (red cross marker)
    val graphicsOverlay = GraphicsOverlay().apply {
        // Create points for geysers in Yellowstone
        val oldFaithful = Point(-110.828140, 44.460458, SpatialReference.wgs84())
        val cascadeGeyser = Point(-110.829004, 44.462438, SpatialReference.wgs84())
        val plumeGeyser = Point(-110.829381, 44.462735, SpatialReference.wgs84())
        // Add graphics for each point
        graphics.addAll(
            listOf(
                Graphic(oldFaithful),
                Graphic(cascadeGeyser),
                Graphic(plumeGeyser)
            )
        )
        // Create a simple renderer with a red cross symbol
        renderer = SimpleRenderer(
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Cross,
                color = Color.red,
                size = 12f
            )
        )
    }

    // Message dialog for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
