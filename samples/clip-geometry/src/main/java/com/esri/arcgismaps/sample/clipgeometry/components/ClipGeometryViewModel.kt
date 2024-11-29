/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.clipgeometry.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.clipgeometry.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ClipGeometryViewModel(application: Application) : AndroidViewModel(application) {
    val arcGISMap by mutableStateOf(
        // create  a map with a topographic basemap style
        ArcGISMap(BasemapStyle.ArcGISTopographic)
    )

    // create a MapViewProxy to interact with the MapView
    val mapViewProxy = MapViewProxy()

    // graphics overlay to display graphics
    val graphicsOverlay = GraphicsOverlay()

    private var coloradoGraphic  = createColoradoGraphic()
    private var coloradoFillSymbol = SimpleFillSymbol(
        SimpleFillSymbolStyle.Solid,
        Color(R.color.transparentDarkBlue),
        SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2f)
    )
    private var envelopesGraphics = emptyList<Graphic>()

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    val isClipButtonEnabled = mutableStateOf(true)
    val isResetButtonEnabled = mutableStateOf(false)

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }.onSuccess {
                // set the viewpoint of the map
                mapViewProxy.setViewpoint(Viewpoint(40.0, -106.0, 10000000.0))

                coloradoGraphic = createColoradoGraphic()
                envelopesGraphics = createEnvelopeGraphics()

                resetGeometry()
            }
        }
    }


    // clear the current graphics, then re-add the graphics for Colorado and the clip boxes
    fun resetGeometry() {

        graphicsOverlay.graphics.clear()

        graphicsOverlay.graphics.add(coloradoGraphic)
        coloradoGraphic.isVisible = true

        envelopesGraphics.forEach { graphic ->
            graphicsOverlay.graphics.add(graphic)
        }

        // update the reset button and clip button
        isResetButtonEnabled.value = false
        isClipButtonEnabled.value = true
    }

    // clip the Colorado geometry using the clip boxes and then add the result to the graphics overlay
    fun clipGeometry() {

        coloradoGraphic.isVisible = false

        // go through each envelope and clip the colorado geometry using it
        envelopesGraphics.forEach { graphic ->
            val geometry =
                coloradoGraphic.geometry?.let { coloradoGeometry ->
                    GeometryEngine.clipOrNull(coloradoGeometry, graphic.geometry as Envelope)
                }
            val clippedGraphic = Graphic(geometry, coloradoFillSymbol)
            graphicsOverlay.graphics.add(clippedGraphic)
        }

        // update the reset button and clip button
        isResetButtonEnabled.value = true
        isClipButtonEnabled.value = false
    }

    /**
     * Create colorado graphic.
     */
    private fun createColoradoGraphic(
    ) : Graphic {
        // create a blue graphic of Colorado
        val colorado = Envelope(
            Point(-11362327.128340, 5012861.290274),
            Point(-12138232.018408, 4441198.773776)
        )
        val fillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.Solid,
            Color(R.color.transparentDarkBlue),
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2f)
        )
        val coloradoGraphic = Graphic(colorado, fillSymbol)

        return coloradoGraphic
    }

    /**
     * Create three envelopes, outside, inside and intersecting colorado graphic.
     */
    private fun createEnvelopeGraphics(
    ) : List<Graphic> {
        // create a dotted red outline symbol
        val redOutline = SimpleLineSymbol(SimpleLineSymbolStyle.Dot, Color.red, 3f)

        // create a envelope outside Colorado
        val outsideEnvelope = Envelope(
            Point(-11858344.321294, 5147942.225174),
            Point(-12201990.219681, 5297071.577304)
        )
        val outside = Graphic(outsideEnvelope, redOutline)

        // create a envelope intersecting Colorado
        val intersectingEnvelope = Envelope(
            Point(-11962086.479298, 4566553.881363),
            Point(-12260345.183558, 4332053.378376)
        )
        val intersecting = Graphic(intersectingEnvelope, redOutline)

        // create a envelope inside Colorado
        val containedEnvelope = Envelope(
            Point(-11655182.595204, 4741618.772994),
            Point(-11431488.567009, 4593570.068343)
        )
        val contained = Graphic(containedEnvelope, redOutline)

        return listOf(outside, intersecting, contained)
    }

}
