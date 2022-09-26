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

package com.esri.arcgisruntime.sample.showresultofspatialoperations

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.geometry.Point
import arcgisruntime.geometry.Polygon
import arcgisruntime.geometry.PolygonBuilder
import arcgisruntime.geometry.SpatialReference
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.symbology.SimpleFillSymbol
import arcgisruntime.mapping.symbology.SimpleFillSymbolStyle
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.sample.showresultofspatialoperations.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val inputGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private val resultGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    // simple black line symbol for outlines
    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.BLACK, 1f)
    private val resultFillSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.RED, lineSymbol)

    private lateinit var inputPolygon1: Polygon
    private lateinit var inputPolygon2: Polygon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

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
        val polygonBuilder1 = PolygonBuilder(SpatialReference.webMercator()).apply {
            // add points to the point collection
            addPoint(Point(-13160.0, 6710100.0))
            addPoint(Point(-13300.0, 6710500.0))
            addPoint(Point(-13760.0, 6710730.0))
            addPoint(Point(-14660.0, 6710000.0))
            addPoint(Point(-13960.0, 6709400.0))
        }
        inputPolygon1 = polygonBuilder1.toGeometry() as Polygon

        // create and add a blue graphic to show input polygon 1
        val blueFill = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.BLUE, lineSymbol)
        inputGeometryGraphicsOverlay.graphics.add(Graphic(inputPolygon1, blueFill))

        //TODO
        // Figure out of multi part builder is implemented?
    }
}
