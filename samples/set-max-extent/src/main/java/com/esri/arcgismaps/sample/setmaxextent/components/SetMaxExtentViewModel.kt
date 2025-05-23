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

package com.esri.arcgismaps.sample.setmaxextent.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

private const val MAX_EXTENT_ENABLED_TEXT = "Maximum extent enabled"
private const val MAX_EXTENT_DISABLED_TEXT = "Maximum extent disabled"

class SetMaxExtentViewModel(app: Application) : AndroidViewModel(app) {

    // defines an envelope representing a rectangular area
    private val extentEnvelope = Envelope(
        Point(-12139393.2109, 5012444.0468),
        Point(-11359277.5124, 4438148.7816)
    )

    // create a map with the BasemapStyle streets focused on Colorado
    val coloradoMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        // set the map's max extent to an envelope of Colorado's northwest and southeast corners
        maxExtent = extentEnvelope
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            coloradoMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // create a graphics overlay of the map's max extent
    val coloradoGraphicsOverlay = GraphicsOverlay().apply {
        // set the graphic's geometry to the max extent of the map
        graphics.add(Graphic(coloradoMap.maxExtent))
        // create a simple red dashed line renderer
        renderer = SimpleRenderer(SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.red, 5f))
    }

    // graphics overlays that are applied to the map
    val graphicsOverlays = listOf(coloradoGraphicsOverlay)

    // the text that indicates whether the max extent is enabled
    var extentText = mutableStateOf(MAX_EXTENT_ENABLED_TEXT)
        private set

    // tracks whether the max extent feature is currently enabled.
    var maxExtentEnabled = mutableStateOf(true)
        private set

    // this function is called when the switch is toggled.
    fun onSwitch(isChecked: Boolean){
        // set max extent to the state of Colorado
        if(isChecked) {
            extentText.value = MAX_EXTENT_ENABLED_TEXT
            coloradoMap.maxExtent = extentEnvelope
        }
        // disable the max extent of the map, map is free to pan around
        else {
            extentText.value = MAX_EXTENT_DISABLED_TEXT
            coloradoMap.maxExtent = null
        }

        maxExtentEnabled.value = isChecked
    }
}
