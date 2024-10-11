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

package com.esri.arcgismaps.sample.showresultofspatialrelationships

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.SpatialRelationship
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.esri.arcgismaps.sample.showresultofspatialrelationships.databinding.ShowResultOfSpatialRelationshipsActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ShowResultOfSpatialRelationshipsActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.show_result_of_spatial_relationships_activity_main)
    }

    // create a MapView using binding
    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // create a graphics overlay
    private val graphicsOverlay by lazy {
        GraphicsOverlay()
    }

    // text view to display the selected graphic
    private val selectedGraphicTV by lazy {
        activityMainBinding.selectedGraphicTV
    }

    // create the polygon graphic
    private val polygonGraphic by lazy {
        // add polygon points to the polygon builder
        val polygonBuilder = PolygonBuilder(SpatialReference.webMercator()) {
            addPoint(Point(-5991501.677830, 5599295.131468))
            addPoint(Point(-6928550.398185, 2087936.739807))
            addPoint(Point(-3149463.800709, 1840803.011362))
            addPoint(Point(-1563689.043184, 3714900.452072))
            addPoint(Point(-3180355.516764, 5619889.608838))
        }
        // create a polygon from the polygon builder
        val polygon = polygonBuilder.toGeometry()
        val polygonSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.ForwardDiagonal, Color.green,
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.green, 2f)
        )
        Graphic(polygon, polygonSymbol)
    }


    // create the polyline graphic
    private val polylineGraphic by lazy {
        // add polyline points to the polyline builder
        val polylineBuilder = PolylineBuilder(SpatialReference.webMercator()) {
            addPoint(Point(-4354240.726880, -609939.795721))
            addPoint(Point(-3427489.245210, 2139422.933233))
            addPoint(Point(-2109442.693501, 4301843.057130))
            addPoint(Point(-1810822.771630, 7205664.366363))
        }
        // create a polyline graphic
        val polyline = polylineBuilder.toGeometry()
        val polylineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.red, 4f)
        Graphic(polyline, polylineSymbol)
    }


    // create the point graphic
    private val pointGraphic by lazy {
        // create a point graphic
        val point = Point(-4487263.495911, 3699176.480377, SpatialReference.webMercator())
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.blue, 10f)
        Graphic(point, locationMarker)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        // add MapView to the lifecycle
        lifecycle.addObserver(mapView)
        mapView.apply {
            // add a map with a topographic basemap style
            map = ArcGISMap(BasemapStyle.ArcGISTopographic)
            // set selection color
            selectionProperties.color = Color.red
            // add graphics overlay
            graphicsOverlays.add(graphicsOverlay)
            //set viewpoint
            setViewpoint(Viewpoint(33.183564, -42.428668, 90000000.0))
        }
        // add the graphics to the graphics overlay
        graphicsOverlay.graphics.addAll(listOf(polygonGraphic, polylineGraphic, pointGraphic))
        // set an on touch listener on the map view
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { tapEvent ->
                // get the tapped coordinate
                val screenCoordinate = tapEvent.screenCoordinate
                // identify the relationships of tapped graphic
                identifyGraphicRelationships(screenCoordinate)
            }
        }
    }

    /**
     * Identifies the selected graphic tapped at the [screenCoordinate]
     * and finds the relations to other graphics on the map.
     */
    private suspend fun identifyGraphicRelationships(screenCoordinate: ScreenCoordinate) {
        // get the graphic selected at the given ScreenCoordinate
        val identifyGraphicsOverlayResult =
            mapView.identifyGraphicsOverlay(graphicsOverlay, screenCoordinate, 1.0, false)
        // get identified graphics overlay, else show an error
        val identifyGraphicsOverlay = identifyGraphicsOverlayResult.getOrElse {
            return showError(it.message.toString())
        }
        // get the identified selected graphics
        val identifiedGraphics = identifyGraphicsOverlay.graphics
        // if no graphic was selected
        if (identifiedGraphics.isEmpty()) {
            // display text and clear selected
            selectedGraphicTV.text = getString(R.string.select_graphic)
            graphicsOverlay.clearSelection()
            return
        }
        // create HashMap that will hold relationships in between graphics
        val relationships = mutableMapOf<String, List<SpatialRelationship>>()
        // add the graphics as keys to the hashmap
        relationships["Point"] = emptyList()
        relationships["Polyline"] = emptyList()
        relationships["Polygon"] = emptyList()
        // select the identified graphic
        graphicsOverlay.clearSelection()
        // get the first graphic identified
        val identifiedGraphic = identifiedGraphics[0]
        // set the identified graphic to be selected
        identifiedGraphic.isSelected = true
        // tracks the type of geometry selected
        var selectedGraphicName = ""
        // find the geometry of the selected graphic
        when (val selectedGeometry = identifiedGraphic.geometry) {
            // if selected geometry is a point
            is Point -> {
                // get the point-polyline relationships
                relationships["Polyline"] =
                    getSpatialRelationships(selectedGeometry, polylineGraphic.geometry)
                // get the point-polygon relationships
                relationships["Polygon"] =
                    getSpatialRelationships(selectedGeometry, polygonGraphic.geometry)
                // update the name of the selected geometry
                selectedGraphicName = "Point"
            }
            // if selected geometry is a polyline
            is Polyline -> {
                // get the polyline-polygon relationships
                relationships["Polygon"] =
                    getSpatialRelationships(selectedGeometry, polygonGraphic.geometry)
                // get the polyline-point relationships
                relationships["Point"] =
                    getSpatialRelationships(selectedGeometry, pointGraphic.geometry)
                // update the name of the selected geometry
                selectedGraphicName = "Polyline"
            }
            // if selected geometry is a polygon
            is Polygon -> {
                // get the polygon-polyline relationships
                relationships["Polyline"] =
                    getSpatialRelationships(selectedGeometry, polylineGraphic.geometry)
                // get the polygon-point relationships
                relationships["Point"] =
                    getSpatialRelationships(selectedGeometry, pointGraphic.geometry)
                // update the name of the selected geometry
                selectedGraphicName = "Polygon"
            }
            // no other graphic on map
            else -> {}
        }
        // display selected graphic text
        selectedGraphicTV.text = "$selectedGraphicName geometry is selected"
        // create and display a dialog with the established graphics
        RelationshipsDialog(layoutInflater, this, relationships, selectedGraphicName).createAndDisplayDialog()

    }

    /**
     * Gets a list of spatial relationships that the [a] geometry has to the [b] geometry.
     */
    private fun getSpatialRelationships(
        a: Geometry?,
        b: Geometry?
    ): List<SpatialRelationship> {
        // check if either geometry is null
        if (a == null || b == null) {
            return emptyList()
        }
        val relationships: MutableList<SpatialRelationship> = mutableListOf()
        if (GeometryEngine.crosses(a, b))
            relationships.add(SpatialRelationship.Crosses)
        if (GeometryEngine.contains(a, b))
            relationships.add(SpatialRelationship.Contains)
        if (GeometryEngine.disjoint(a, b))
            relationships.add(SpatialRelationship.Disjoint)
        if (GeometryEngine.intersects(a, b))
            relationships.add(SpatialRelationship.Intersects)
        if (GeometryEngine.overlaps(a, b))
            relationships.add(SpatialRelationship.Overlaps)
        if (GeometryEngine.touches(a, b))
            relationships.add(SpatialRelationship.Touches)
        if (GeometryEngine.within(a, b))
            relationships.add(SpatialRelationship.Within)
        return relationships
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }
}
