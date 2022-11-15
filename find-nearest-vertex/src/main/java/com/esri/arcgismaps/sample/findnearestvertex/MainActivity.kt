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

package com.esri.arcgismaps.sample.findnearestvertex

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.findnearestvertex.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val distanceLayout: ConstraintLayout by lazy {
        activityMainBinding.distanceLayout
    }

    private val vertexDistanceTextView: TextView by lazy {
        activityMainBinding.vertexDistanceTextView
    }

    private val coordinateDistanceTextView: TextView by lazy {
        activityMainBinding.coordinateDistanceTextView
    }

    // California zone 5 (ftUS) state plane coordinate system.
    private val statePlaneCaliforniaZone5SpatialReference = SpatialReference(2229)

    // create graphics with symbols for tapped location, nearest coordinate, and nearest vertex
    private val tappedLocationGraphic = Graphic().apply {
        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.magenta, 15f)
    }
    // create graphic symbol of the nearest coordinate
    private val nearestCoordinateGraphic = Graphic().apply {
        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 10f)
    }
    // create graphic symbol of the nearest vertex
    private val nearestVertexGraphic = Graphic().apply {
        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.blue, 15f)
    }

    // create a graphic for the polygon
    private val polygon = PolygonBuilder(statePlaneCaliforniaZone5SpatialReference) {
        addPoint(Point(6627416.41469281, 1804532.53233782))
        addPoint(Point(6669147.89779046, 2479145.16609522))
        addPoint(Point(7265673.02678292, 2484254.50442408))
        addPoint(Point(7676192.55880379, 2001458.66365744))
    }.toGeometry()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // set up the outline and fill color of the polygon
        val polygonOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2f)
        val polygonFillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.ForwardDiagonal,
            Color.green,
            polygonOutlineSymbol
        )
        val polygonGraphic = Graphic(polygon, polygonFillSymbol)
        // create a graphics overlay to show the polygon, tapped location, and nearest vertex/coordinate
        val graphicsOverlay = GraphicsOverlay().apply {
            graphics.addAll(
                listOf(
                    polygonGraphic,
                    tappedLocationGraphic,
                    nearestCoordinateGraphic,
                    nearestVertexGraphic
                )
            )
        }

        // create a map using the portal item
        val map = ArcGISMap(statePlaneCaliforniaZone5SpatialReference)
        val portal = Portal("https://arcgisruntime.maps.arcgis.com", false)
        val portalItem = PortalItem(portal, "99fd67933e754a1181cc755146be21ca")
        val usStatesGeneralizedLayer = FeatureLayer(portalItem, 0)
        lifecycleScope.launch {
            map.basemap.collect { basemap ->
                // and add the FeatureLayer to the map's basemap
                basemap?.baseLayers?.add(usStatesGeneralizedLayer)
                mapView.map = map
                // add the graphics overlay to the map view
                mapView.graphicsOverlays.add(graphicsOverlay)
                // zoom to the polygon's extent
                mapView.setViewpointGeometry(polygon.extent, 100.0)
                // find nearest vertex on map tapped
                findNearestVertex(mapView.onSingleTapConfirmed)
            }
        }
    }

    /**
     * Collects events from [onSingleTapConfirmed] of the map and finds the nearest vertex
     */
    private suspend fun findNearestVertex(onSingleTapConfirmed: SharedFlow<SingleTapConfirmedEvent>) {
        onSingleTapConfirmed.collect { event ->
            // get map point tapped, return if null
            val mapPoint = event.mapPoint ?: return@collect
            // show where the user clicked
            tappedLocationGraphic.geometry = mapPoint
            // use the geometry engine to get the nearest vertex, return if null
            val nearestVertexResult =
                GeometryEngine.nearestVertex(polygon, mapPoint) ?: return@collect
            // set the nearest vertex graphic's geometry to the nearest vertex
            nearestVertexGraphic.geometry = nearestVertexResult.coordinate
            // use the geometry engine to get the nearest coordinate, return if null
            val nearestCoordinateResult =
                GeometryEngine.nearestCoordinate(polygon, mapPoint) ?: return@collect
            // set the nearest coordinate graphic's geometry to the nearest coordinate
            nearestCoordinateGraphic.geometry = nearestCoordinateResult.coordinate
            // show the distances to the nearest vertex and nearest coordinate
            distanceLayout.visibility = View.VISIBLE
            // convert distance to miles
            val vertexDistance = (nearestVertexResult.distance / 5280.0).toInt()
            val coordinateDistance = (nearestCoordinateResult.distance / 5280.0).toInt()
            // set the distance to the text views
            vertexDistanceTextView.text = getString(R.string.nearest_vertex, vertexDistance)
            coordinateDistanceTextView.text =
                getString(R.string.nearest_coordinate, coordinateDistance)
        }
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

    private val Color.Companion.magenta: Color
        get() {
            return fromRgba(255, 0, 255, 255)
        }
}
