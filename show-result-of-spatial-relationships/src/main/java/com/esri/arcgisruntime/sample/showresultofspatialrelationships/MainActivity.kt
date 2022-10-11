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

package com.esri.arcgisruntime.sample.showresultofspatialrelationships

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.geometry.MutablePointCollection
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
import com.esri.arcgisruntime.sample.showresultofspatialrelationships.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create a graphics overlay
        val graphicsOverlay = GraphicsOverlay()

        mapView.apply {
            // create and add a map with a topographic basemap style
            map = ArcGISMap(BasemapStyle.ArcGISTopographic)
            // set selection color
            selectionProperties.color = Color.RED
            // add graphics overlay
            graphicsOverlays.add(graphicsOverlay)
        }

        // add the polygon points to the polygon builder
        val polygonBuilder = PolygonBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(-5991501.677830, 5599295.131468))
            addPoint(Point(-6928550.398185, 2087936.739807))
            addPoint(Point(-3149463.800709, 1840803.011362))
            addPoint(Point(-1563689.043184, 3714900.452072))
            addPoint(Point(-3180355.516764, 5619889.608838))
        }
        // create a polygon from the point collection
        val polygon = polygonBuilder.toGeometry() as Polygon

        val polygonSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.ForwardDiagonal, Color.GREEN,
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, -0xff0100, 2f)
        )
        val polygonGraphic = Graphic(polygon, polygonSymbol)
        graphicsOverlay.graphics.add(polygonGraphic)

    }
}
