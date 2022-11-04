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

package com.esri.arcgismaps.sample.showresultofspatialoperations

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PointBuilder
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.showresultofspatialoperations.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val inputGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private val resultGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    // simple black line symbol for outlines
    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f)
    private val resultFillSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.red, lineSymbol)

    private lateinit var inputPolygon1: Polygon
    private lateinit var inputPolygon2: Polygon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        val spinnerSelector = activityMainBinding.selector
        lifecycle.addObserver(mapView)

        // set up the adapter
        val arrayAdapter =
            ArrayAdapter(this, R.layout.dropdown_item, resources.getStringArray(R.array.operation))
        spinnerSelector.setAdapter(arrayAdapter)
        // set the first item to be selected by default
        spinnerSelector.setText(arrayAdapter.getItem(0), false);


        // set up the MapView
        mapView.apply {
            // create an ArcGISMap with a light gray basemap
            map = ArcGISMap(BasemapStyle.ArcGISLightGray)
            // create graphics overlays to show the inputs and results of the spatial operation
            graphicsOverlays.add(inputGeometryGraphicsOverlay)
            graphicsOverlays.add(resultGeometryGraphicsOverlay)
        }

        // create input polygons and add graphics to display these polygons in an overlay
        createPolygons()

        // center the map view on the input geometries
        val envelope = inputPolygon1.extent
        lifecycleScope.launch{
            if (envelope != null) {
                mapView.setViewpointGeometry(envelope, 20.0)
            }
        }

    }

    private fun createPolygons() {
        // create input polygon 1
        val polygonBuilder1 = PolygonBuilder(SpatialReference.webMercator()) {
            // add points to the point collection
            addPoint(Point(-13160.0, 6710100.0))
            addPoint(Point(-13300.0, 6710500.0))
            addPoint(Point(-13760.0, 6710730.0))
            addPoint(Point(-14660.0, 6710000.0))
            addPoint(Point(-13960.0, 6709400.0))
        }
        inputPolygon1 = polygonBuilder1.toGeometry() as Polygon

        // create and add a blue graphic to show input polygon 1
        val blueFill = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.blue, lineSymbol)
        inputGeometryGraphicsOverlay.graphics.add(Graphic(inputPolygon1, blueFill))

        val outerRing = PointBuilder(SpatialReference.webMercator()) {
            this.

        }

        /*
        // create a mutable part list
        val heartParts = MutablePart(spatialReference).apply {
            addAll(
                listOf(leftCurve, leftArc, rightArc, rightCurve)
            )
        }
        // return the heart
        return Polygon(listOf(heartParts).asIterable())
        */
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }
}
