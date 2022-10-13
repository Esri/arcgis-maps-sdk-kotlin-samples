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
import androidx.lifecycle.lifecycleScope
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.data.SpatialRelationship
import arcgisruntime.geometry.Geometry
import arcgisruntime.geometry.GeometryEngine
import arcgisruntime.geometry.Point
import arcgisruntime.geometry.Polygon
import arcgisruntime.geometry.PolygonBuilder
import arcgisruntime.geometry.Polyline
import arcgisruntime.geometry.PolylineBuilder
import arcgisruntime.geometry.SpatialReference
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.symbology.SimpleFillSymbol
import arcgisruntime.mapping.symbology.SimpleFillSymbolStyle
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.symbology.SimpleMarkerSymbol
import arcgisruntime.mapping.symbology.SimpleMarkerSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import arcgisruntime.mapping.view.ScreenCoordinate
import com.esri.arcgisruntime.sample.showresultofspatialrelationships.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        // add MapView to the lifecycle
        lifecycle.addObserver(mapView)
        mapView.apply {
            // add a map with a topographic basemap style
            map = ArcGISMap(BasemapStyle.ArcGISTopographic)
            // set selection color
            selectionProperties.color = Color.RED
            // add graphics overlay
            graphicsOverlays.add(graphicsOverlay)
            //set viewpoint
            setViewpoint(Viewpoint(33.183564, -42.428668480377, 90000000.0))
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
        // create HashMap that will hold relationships in between graphics
        val relationships = HashMap<String, List<SpatialRelationship>>()
        val identifyGraphicsOverlayResult =
            mapView.identifyGraphicsOverlay(graphicsOverlay, screenCoordinate, 1.0, false)
        identifyGraphicsOverlayResult.onSuccess { identifyGraphicsOverlay ->
            val identifiedGraphics = identifyGraphicsOverlay.graphics
            // no graphic selected
            if (identifiedGraphics.isEmpty()) {
                // display text and clear selected
                selectedGraphicTV.text = getString(R.string.select_graphic)
                graphicsOverlay.clearSelection()
                return
            }

            // clear previous results
            relationships["Point"] = emptyList()
            relationships["Polyline"] = emptyList()
            relationships["Polygon"] = emptyList()
            // select the identified graphic
            graphicsOverlay.clearSelection()
            // get the first graphic identified
            val identifiedGraphic = identifiedGraphics[0]
            identifiedGraphic.isSelected = true

            when (val selectedGeometry = identifiedGraphic.geometry) {
                // if selected geometry is a point
                is Point -> {
                    relationships["Polyline"] =
                        getSpatialRelationships(selectedGeometry, polylineGraphic.geometry)
                    relationships["Polygon"] =
                        getSpatialRelationships(selectedGeometry, polygonGraphic.geometry)
                    selectedGraphicTV.text = "Point geometry is selected"
                }
                // if selected geometry is a polyline
                is Polyline -> {
                    relationships["Polygon"] =
                        getSpatialRelationships(selectedGeometry, polygonGraphic.geometry)
                    relationships["Point"] =
                        getSpatialRelationships(selectedGeometry, pointGraphic.geometry)
                    selectedGraphicTV.text = "Polyline geometry is selected"
                }
                // if selected geometry is a polygon
                is Polygon -> {
                    relationships["Polyline"] =
                        getSpatialRelationships(selectedGeometry, polylineGraphic.geometry)
                    relationships["Point"] =
                        getSpatialRelationships(selectedGeometry, pointGraphic.geometry)
                    selectedGraphicTV.text = "Polygon geometry is selected"
                }
                // no graphic selected
                else -> {
                    selectedGraphicTV.text = getString(R.string.select_graphic)
                }
            }
            RelationshipsDialog().createDialog(layoutInflater, this, relationships)

        }.onFailure {
            //TODO
        }
    }

    /**
     * Gets a list of spatial relationships that the first geometry has to the second geometry.
     *
     * @param a first geometry
     * @param b second geometry
     * @return list of relationships a has to b
     */
    private fun getSpatialRelationships(
        a: Geometry?,
        b: Geometry?
    ): List<SpatialRelationship> {
        val relationships: MutableList<SpatialRelationship> = mutableListOf()
        // check if either geometry is null
        if (a == null || b == null) {
            return mutableListOf()
        }
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

    /**
     * Gets a string representation of the spatial relationship list
     *
     * @param relationshipList a list of spatial relationships
     * @return a string list of spatial relationships
     */
    private fun relationshipStringList(relationshipList: List<SpatialRelationship>): MutableList<String> {
        val stringList = mutableListOf<String>()
        for (relationship in relationshipList) {
            stringList.add(relationship.toString())
        }
        return stringList
    }


    private val polygonGraphic by lazy {
        // add polygon points to the polygon builder
        val polygonBuilder = PolygonBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(-5991501.677830, 5599295.131468))
            addPoint(Point(-6928550.398185, 2087936.739807))
            addPoint(Point(-3149463.800709, 1840803.011362))
            addPoint(Point(-1563689.043184, 3714900.452072))
            addPoint(Point(-3180355.516764, 5619889.608838))
        }
        // create a polygon from the polygon builder
        val polygon = polygonBuilder.toGeometry() as Polygon
        val polygonSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.ForwardDiagonal, Color.GREEN,
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.GREEN, 2f)
        )
        Graphic(polygon, polygonSymbol)
    }

    private val polylineGraphic by lazy {
        // add polyline points to the polyline builder
        val polylineBuilder = PolylineBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(-4354240.726880, -609939.795721))
            addPoint(Point(-3427489.245210, 2139422.933233))
            addPoint(Point(-2109442.693501, 4301843.057130))
            addPoint(Point(-1810822.771630, 7205664.366363))
        }
        // create a polyline graphic
        val polyline = polylineBuilder.toGeometry() as Polyline
        val polylineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.RED, 4f)
        Graphic(polyline, polylineSymbol)
    }

    private val pointGraphic by lazy {
        // create a point graphic
        val point = Point(-4487263.495911, 3699176.480377, SpatialReference.webMercator())
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.BLUE, 10f)
        Graphic(point, locationMarker)
    }
}
