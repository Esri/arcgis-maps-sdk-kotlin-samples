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

package com.esri.arcgismaps.sample.findnearestvertex.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
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
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class FindNearestVertexViewModel(application: Application) : AndroidViewModel(application) {
    // California zone 5 (ftUS) state plane coordinate system.
    private val statePlaneCaliforniaZone5SpatialReference = SpatialReference(2229)

    val portal = Portal("https://arcgisruntime.maps.arcgis.com")
    val portalItem = PortalItem(portal, "8c2d6d7df8fa4142b0a1211c8dd66903")
    private val usStatesGeneralizedLayer = FeatureLayer.createWithItemAndLayerId(portalItem, 0)

    // create a MapViewProxy to interact with the MapView
    val mapViewProxy = MapViewProxy()

    // create graphics with symbols for tapped location, nearest coordinate, and nearest vertex
    private val tappedLocationGraphic =
        Graphic(symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.magenta, 15f))

    // create graphic symbol of the nearest coordinate
    private val nearestCoordinateGraphic =
        Graphic(symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 10f))

    // create graphic symbol of the nearest vertex
    private val nearestVertexGraphic =
        Graphic(symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.blue, 15f))

    // create a polygon geometry
    val polygon = PolygonBuilder(statePlaneCaliforniaZone5SpatialReference) {
        addPoint(Point(6627416.41469281, 1804532.53233782))
        addPoint(Point(6669147.89779046, 2479145.16609522))
        addPoint(Point(7265673.02678292, 2484254.50442408))
        addPoint(Point(7676192.55880379, 2001458.66365744))
    }.toGeometry()
    // set up the outline and fill color of the polygon
    private val polygonOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2f)
    private val polygonFillSymbol = SimpleFillSymbol(
        SimpleFillSymbolStyle.ForwardDiagonal,
        Color.green,
        polygonOutlineSymbol
    )
    // create a polygon graphic
    private val polygonGraphic = Graphic(polygon, polygonFillSymbol)
    // create a graphics overlay to show the polygon, tapped location, and nearest vertex/coordinate
    val graphicsOverlay = GraphicsOverlay(
        listOf(
            polygonGraphic,
            tappedLocationGraphic,
            nearestCoordinateGraphic,
            nearestVertexGraphic
        )
    )

    //TODO - delete mutable state when the map does not change or the screen does not need to observe changes
    val arcGISMap by mutableStateOf(
        ArcGISMap(statePlaneCaliforniaZone5SpatialReference).apply {
            // set the map viewpoint to the polygon
            initialViewpoint =
                Viewpoint(polygon.extent)
        }
    )

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onSuccess {
                // and add the feature layer to the map's operational layers
                arcGISMap.operationalLayers.add(usStatesGeneralizedLayer)
            }.onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load map",
                    description = error.message.toString()
                )
            }
        }
    }

    /**
     * Handle a tap from the user and call [findNearestVertex].
     */
    fun onMapViewTapped(event: SingleTapConfirmedEvent) {
        event.mapPoint?.let { point ->
            findNearestVertex(point, polygon)
            mapViewProxy.setViewpoint(Viewpoint(polygon))
        }
    }

    /**
     * Finds the nearest vertex from [mapPoint] from the [polygon]
     */
    private fun findNearestVertex(mapPoint: Point, polygon: Polygon) {
        // show where the user clicked
        tappedLocationGraphic.geometry = mapPoint
        // use the geometry engine to get the nearest vertex
        val nearestVertexResult =
            GeometryEngine.nearestVertex(polygon, mapPoint)
        // set the nearest vertex graphic's geometry to the nearest vertex
        nearestVertexGraphic.geometry = nearestVertexResult?.coordinate
        // use the geometry engine to get the nearest coordinate
        val nearestCoordinateResult =
            GeometryEngine.nearestCoordinate(polygon, mapPoint)
        // set the nearest coordinate graphic's geometry to the nearest coordinate
        nearestCoordinateGraphic.geometry = nearestCoordinateResult?.coordinate
        // show the distances to the nearest vertex and nearest coordinate
        //distanceLayout.visibility = View.VISIBLE
        // convert distance to miles
        val vertexDistance = ((nearestVertexResult?.distance)?.div(5280.0))?.toInt() // MAGIC NUMBER - CHANGE?
        val coordinateDistance = ((nearestCoordinateResult?.distance)?.div(5280.0))?.toInt()
        // set the distance to the text views
        //vertexDistanceTextView.text = getString(R.string.nearest_vertex, vertexDistance)
        //coordinateDistanceTextView.text =
        //    getString(R.string.nearest_coordinate, coordinateDistance)

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
