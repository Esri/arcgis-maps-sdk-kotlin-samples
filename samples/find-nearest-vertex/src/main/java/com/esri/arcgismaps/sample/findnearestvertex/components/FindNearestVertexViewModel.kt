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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FindNearestVertexViewModel(application: Application) : AndroidViewModel(application) {
    // California zone 5 (ftUS) state plane coordinate system.
    private val statePlaneCaliforniaZone5SpatialReference = SpatialReference(2229)

    // create a portal
    val portal = Portal("https://arcgisruntime.maps.arcgis.com")
    // get the USA States Generalized Boundaries layer from the portal using its ID
    val portalItem = PortalItem(portal, "8c2d6d7df8fa4142b0a1211c8dd66903")
    // create a feature layer from the portal item
    private val usStatesGeneralizedLayer = FeatureLayer.createWithItemAndLayerId(portalItem, 0)

    // create a MapViewProxy to interact with the MapView
    val mapViewProxy = MapViewProxy()

    // create graphic symbol for the tapped location
    private val tappedLocationGraphic =
        Graphic(symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.X, Color.magenta, 15f))

    // create graphic symbol for the nearest coordinate
    private val nearestCoordinateGraphic =
        Graphic(symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 10f))

    // create graphic symbol for the nearest vertex
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

    val arcGISMap by mutableStateOf(
        ArcGISMap(statePlaneCaliforniaZone5SpatialReference).apply {
            // set the map viewpoint to the polygon
            initialViewpoint =
                Viewpoint(polygon.extent.center, 8e6)
        }
    )

    // state flow of nearest vertex distance and nearest coordinate distance for presentation in UI
    private val _distanceInformationFlow = MutableStateFlow<DistanceInformation?>(null)
    val distanceInformationFlow = _distanceInformationFlow.asStateFlow()

    // create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onSuccess {
                // and add the USA States Generalized Boundaries feature layer to the map's operational layers
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
        }
    }

    /**
     * Finds the nearest vertex from [mapPoint] from the [polygon].
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
        // calculate the distances to the nearest vertex and nearest coordinate then convert to miles
        val vertexDistance = ((nearestVertexResult?.distance)?.div(5280.0))?.toInt()!!
        val coordinateDistance = ((nearestCoordinateResult?.distance)?.div(5280.0))?.toInt()!!
        // update the distance information so the UI can display it
        _distanceInformationFlow.value = DistanceInformation(vertexDistance, coordinateDistance)
    }

    /**
     * Define a blue color for the nearest vertex graphic.
     */
    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

    /**
     * Define a magenta color for the tapped location graphic.
     */
    private val Color.Companion.magenta: Color
        get() {
            return fromRgba(255, 0, 255, 255)
        }

    /**
     * Data class representing vertex distance and coordinate distance.
     */
    data class DistanceInformation (val vertexDistance: Int, val coordinateDistance: Int)
}
