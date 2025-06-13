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

package com.esri.arcgismaps.sample.applyclassbreaksrenderertosublayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.ArcGISMapImageSublayer
import com.arcgismaps.mapping.symbology.ClassBreak
import com.arcgismaps.mapping.symbology.ClassBreaksRenderer
import com.arcgismaps.mapping.symbology.Renderer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ApplyClassBreaksRendererToSublayerViewModel(app: Application) : AndroidViewModel(app) {

    // The map image layer
    private val mapImageLayer =
        ArcGISMapImageLayer("https://sampleserver6.arcgisonline.com/arcgis/rest/services/Census/MapServer")

    var arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            Envelope(
                xMin = -13934661.666904,
                yMin = 331181.323482,
                xMax = -7355704.998713,
                yMax = 9118038.075882,
                spatialReference = SpatialReference.webMercator()
            )
        )
        // Add the map image layer to the map
        operationalLayers.add(mapImageLayer)
    }

    // The counties sublayer
    private var countiesSublayer: ArcGISMapImageSublayer? = null

    // The original renderer of the counties sublayer
    private var originalRenderer: Renderer? = null

    private var classBreaksRenderer = ClassBreaksRenderer(
        fieldName = "POP2007",
        classBreaks = classBreaks
    )

    // Whether the class breaks renderer is currently applied
    var classBreaksRendererIsApplied by mutableStateOf(false)
        private set

    // Whether the sublayer is loaded and ready
    var isSublayerReady by mutableStateOf(false)
        private set

    // Message dialog view model for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {

            // Load the map image layer
            mapImageLayer.load().onSuccess {
                // Get the sublayers
                val sublayers = mapImageLayer.mapImageSublayers
                if (sublayers.size > 2) {
                    val sublayer = sublayers[2]
                    sublayer.load().onSuccess {
                        countiesSublayer = sublayer
                        originalRenderer = sublayer.renderer
                        isSublayerReady = true
                    }.onFailure { messageDialogVM.showMessageDialog(it) }
                } else {
                    messageDialogVM.showMessageDialog("Counties sublayer not found.")
                }
            }.onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Toggle the renderer on the counties sublayer between the original and the class breaks renderer.
     */
    fun toggleClassBreaksRenderer() {
        val sublayer = countiesSublayer ?: return
        classBreaksRendererIsApplied = !classBreaksRendererIsApplied
        sublayer.renderer = if (classBreaksRendererIsApplied) {
            classBreaksRenderer
        } else {
            originalRenderer
        }
    }

    /**
     * Companion object to hold the class breaks and symbols.
     */
    companion object {
        private val outline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.fromRgba(153, 153, 153, 255), // gray
            width = 0.5f
        )

        private val symbol1 = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(227, 235, 207, 255), // light green
            outline = outline
        )
        private val symbol2 = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(150, 193, 191, 255), // teal
            outline = outline
        )
        private val symbol3 = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(98, 167, 182, 255), // blue-green
            outline = outline
        )
        private val symbol4 = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(68, 125, 151, 255), // darker blue
            outline = outline
        )
        private val symbol5 = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.fromRgba(41, 84, 121, 255), // navy
            outline = outline
        )

        private val classBreaks = listOf(
            ClassBreak(
                description = "-99 to 8,560",
                label = "-99 to 8,560",
                minValue = -99.0,
                maxValue = 8560.0,
                symbol = symbol1
            ),
            ClassBreak(
                description = "> 8,560 to 18,109",
                label = "> 8,560 to 18,109",
                minValue = 8561.0,
                maxValue = 18109.0,
                symbol = symbol2
            ),
            ClassBreak(
                description = "> 18,109 to 35,501",
                label = "> 18,109 to 35,501",
                minValue = 18110.0,
                maxValue = 35501.0,
                symbol = symbol3
            ),
            ClassBreak(
                description = "> 35,501 to 86,100",
                label = "> 35,501 to 86,100",
                minValue = 35502.0,
                maxValue = 86100.0,
                symbol = symbol4
            ),
            ClassBreak(
                description = "> 86,100 to 10,110,975",
                label = "> 86,100 to 10,110,975",
                minValue = 86101.0,
                maxValue = 10110975.0,
                symbol = symbol5
            )
        )
    }
}
