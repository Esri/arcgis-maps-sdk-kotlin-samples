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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.MutablePart
import com.arcgismaps.geometry.Point
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
import com.esri.arcgismaps.sample.showresultofspatialoperations.databinding.ShowResultOfSpatialOperationsActivityMainBinding
import kotlinx.coroutines.launch

private val Color.Companion.blue: Color
    get() {
        return fromRgba(0, 0, 255, 255)
    }

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ShowResultOfSpatialOperationsActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.show_result_of_spatial_operations_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // enum to keep track of the selected operation to display on the map
    enum class SpatialOperation(val menuPosition: Int) {
        NO_OPERATION(0),
        INTERSECTION(1),
        UNION(2),
        DIFFERENCE(3),
        SYMMETRIC_DIFFERENCE(4)
    }

    // create the graphic overlays
    private val inputGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }
    private val resultGeometryGraphicsOverlay: GraphicsOverlay by lazy { GraphicsOverlay() }

    // simple black line symbol for outlines
    private val lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f)

    // red fill symbol for result
    private val resultFillSymbol =
        SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.red, lineSymbol)

    // the two polygons for perform spatial operations
    private lateinit var inputPolygon1: Polygon
    private lateinit var inputPolygon2: Polygon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        // set up the adapter
        val arrayAdapter = ArrayAdapter(
            this,
            com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
            resources.getStringArray(R.array.operation)
        )
        activityMainBinding.bottomListItems.apply {
            setAdapter(arrayAdapter)
            setOnItemClickListener { _, _, i, _ ->
                updateGeometry(i)
            }
        }

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
        val envelope = GeometryEngine.union(inputPolygon1, inputPolygon2).extent
        lifecycleScope.launch {
            mapView.setViewpointGeometry(envelope, 20.0)
        }
    }

    private fun updateGeometry(position: Int) {
        // clear previous operation result
        resultGeometryGraphicsOverlay.graphics.clear()
        // create a result geometry of the spatial operation
        var resultGeometry: Geometry? = null
        // get the selected operation
        when (SpatialOperation.entries.find { it.menuPosition == position }) {
            SpatialOperation.NO_OPERATION -> { /* No operation needed */
            }
            SpatialOperation.INTERSECTION -> {
                resultGeometry = GeometryEngine.intersectionOrNull(inputPolygon1, inputPolygon2)
            }
            SpatialOperation.UNION -> {
                resultGeometry = GeometryEngine.union(inputPolygon1, inputPolygon2)
            }
            SpatialOperation.DIFFERENCE -> {
                resultGeometry = GeometryEngine.differenceOrNull(inputPolygon1, inputPolygon2)
            }
            SpatialOperation.SYMMETRIC_DIFFERENCE -> {
                resultGeometry = GeometryEngine.symmetricDifferenceOrNull(inputPolygon1, inputPolygon2)
            }
            null -> {}
        }

        // add a graphic from the result geometry, showing result in red
        if (resultGeometry != null) {
            val resultGraphic = Graphic(resultGeometry, resultFillSymbol).also {
                // select the result to highlight it
                it.isSelected = true
            }
            // add the result graphic to the graphic overlay
            resultGeometryGraphicsOverlay.graphics.add(resultGraphic)
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
        inputPolygon1 = polygonBuilder1.toGeometry()

        // create and add a blue graphic to show input polygon 1
        val blueFill = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.blue, lineSymbol)
        inputGeometryGraphicsOverlay.graphics.add(Graphic(inputPolygon1, blueFill))

        // outer ring
        val outerRing = MutablePart.createWithPoints(
            listOf(
                Point(-13060.0, 6711030.0),
                Point(-12160.0, 6710730.0),
                Point(-13160.0, 6709700.0),
                Point(-14560.0, 6710730.0),
                Point(-13060.0, 6711030.0),
            ),
            SpatialReference.webMercator()
        )

        // inner ring
        val innerRing = MutablePart.createWithPoints(
            listOf(
                Point(-13060.0, 6710910.0),
                Point(-12450.0, 6710660.0),
                Point(-13160.0, 6709900.0),
                Point(-14160.0, 6710630.0),
                Point(-13060.0, 6710910.0)
            ),
            SpatialReference.webMercator()
        )

        // add both parts (rings) to a polygon and create a geometry from it
        inputPolygon2 = Polygon(listOf(outerRing, innerRing))

        // create and add a green graphic to show input polygon 2
        val greenFill = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.green, lineSymbol)
        inputGeometryGraphicsOverlay.graphics.add(Graphic(inputPolygon2, greenFill))
    }
}
