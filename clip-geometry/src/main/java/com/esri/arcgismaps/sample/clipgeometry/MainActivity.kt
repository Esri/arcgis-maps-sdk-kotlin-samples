/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.clipgeometry

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
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
import com.esri.arcgismaps.sample.clipgeometry.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        // get the MapView instance from the data binding
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)
        // create and add a map with a topographic basemap style
        mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        // set the viewpoint of the MapView
        mapView.setViewpoint(Viewpoint(40.0, -106.0, 10000000.0))

        // create a graphics overlay to contain the geometry to clip
        val graphicsOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(graphicsOverlay)

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
        graphicsOverlay.graphics.add(coloradoGraphic)

        // create a graphics overlay to contain the clipping envelopes
        val envelopesOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(envelopesOverlay)

        // create a dotted red outline symbol
        val redOutline = SimpleLineSymbol(SimpleLineSymbolStyle.Dot, Color.red, 3f)

        // create a envelope outside Colorado
        val outsideEnvelope = Envelope(
            Point(-11858344.321294, 5147942.225174),
            Point(-12201990.219681, 5297071.577304)
        )
        val outside = Graphic(outsideEnvelope, redOutline)
        envelopesOverlay.graphics.add(outside)

        // create a envelope intersecting Colorado
        val intersectingEnvelope = Envelope(
            Point(-11962086.479298, 4566553.881363),
            Point(-12260345.183558, 4332053.378376)
        )
        val intersecting = Graphic(intersectingEnvelope, redOutline)
        envelopesOverlay.graphics.add(intersecting)

        // create a envelope inside Colorado
        val containedEnvelope = Envelope(
            Point(-11655182.595204, 4741618.772994),
            Point(-11431488.567009, 4593570.068343)
        )
        val contained = Graphic(containedEnvelope, redOutline)
        envelopesOverlay.graphics.add(contained)

        // create a graphics overlay to contain the clipped areas
        val clipAreasOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(clipAreasOverlay)

        // create a button to perform the clip operation
        val clipButton: Button = findViewById(R.id.clipButton)
        clipButton.setOnClickListener {
            // disable button
            clipButton.isEnabled = false
            // for each envelope, clip the Colorado geometry and show the result,
            // replacing the original Colorado graphic
            coloradoGraphic.isVisible = false
            for (graphic in envelopesOverlay.graphics) {
                val geometry =
                    coloradoGraphic.geometry?.let { coloradoGeometry ->
                        GeometryEngine.clip(coloradoGeometry, graphic.geometry as Envelope)
                    }
                val clippedGraphic = Graphic(geometry, fillSymbol)
                clipAreasOverlay.graphics.add(clippedGraphic)

            }
        }
    }
}
