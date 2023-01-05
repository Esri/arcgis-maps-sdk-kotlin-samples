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

package com.esri.arcgismaps.sample.setmaxextent

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
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
import com.esri.arcgismaps.sample.setmaxextent.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val extentSwitch: SwitchCompat by lazy {
        activityMainBinding.extentSwitch
    }

    private val extentTextView: TextView by lazy {
        activityMainBinding.extentText
    }

    private val extentEnvelope = Envelope(
        Point(-12139393.2109, 5012444.0468),
        Point(-11359277.5124, 4438148.7816)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create a map with the BasemapStyle streets focused on Colorado
        val coloradoMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            // set the map's max extent to an envelope of Colorado's northwest and southeast corners
            maxExtent = extentEnvelope
        }

        // create a graphics overlay of the map's max extent
        val coloradoGraphicsOverlay = GraphicsOverlay().apply {
            // set the graphic's geometry to the max extent of the map
            graphics.add(Graphic(coloradoMap.maxExtent))
            // create a simple red dashed line renderer
            renderer = SimpleRenderer(SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.red, 5f))
        }

        extentSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // set max extent to the state of Colorado
                coloradoMap.maxExtent = extentEnvelope
                extentTextView.text = getString(R.string.extentEnable)
            } else {
                // disable the max extent of the map, map is free to pan around
                coloradoMap.maxExtent = null
                extentTextView.text = getString(R.string.extentDisable)
            }
        }

        mapView.apply {
            // set the map to the map view
            map = coloradoMap
            // set the graphics overlay to the map view
            graphicsOverlays.add(coloradoGraphicsOverlay)
        }
    }
}
