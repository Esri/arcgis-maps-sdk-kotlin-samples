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

package com.esri.arcgismaps.sample.stylegraphicswithsymbols

import android.os.Bundle
import com.esri.arcgismaps.sample.sampleslib.EdgeToEdgeCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.stylegraphicswithsymbols.databinding.StyleGraphicsWithSymbolsActivityMainBinding

class MainActivity : EdgeToEdgeCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        // set up data binding for the activity
        val activityMainBinding: StyleGraphicsWithSymbolsActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.style_graphics_with_symbols_activity_main)
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // create the graphics overlay
        val graphicsOverlay = GraphicsOverlay()

        mapView.apply {
            // create a map with the BasemapStyle Oceans and display it in this view
            map = ArcGISMap(BasemapStyle.ArcGISOceans)
            setViewpoint(Viewpoint(56.075844, -2.681572, 100000.0))
            // add the overlay to the map view
            graphicsOverlays.add(graphicsOverlay)
        }

        // add some buoy positions to the graphics overlay
        val buoyPoints = createBuoyGraphics()
        graphicsOverlay.graphics.addAll(buoyPoints)

        // add boat trip polyline to graphics overlay
        val tripRouteGraphic = createRoute()
        graphicsOverlay.graphics.add(tripRouteGraphic)

        // add nesting ground polygon to graphics overlay
        val nestingGround = createNestingGround()
        graphicsOverlay.graphics.add(nestingGround)

        // add text symbols and points to graphics overlay
        val textGraphics = createTextGraphics()
        graphicsOverlay.graphics.addAll(textGraphics)
    }

    /**
     * Create Graphics for some points.
     *
     * @return a new graphic
     */
    private fun createBuoyGraphics(): Array<Graphic> {
        // define the buoy locations
        val buoy1Loc = Point(-2.712642647560347, 56.06281256681154, SpatialReference.wgs84())
        val buoy2Loc = Point(-2.690841695957230, 56.06444173689877, SpatialReference.wgs84())
        val buoy3Loc = Point(-2.669727388499094, 56.06425007340287, SpatialReference.wgs84())
        val buoy4Loc = Point(-2.639515046119973, 56.06127916736989, SpatialReference.wgs84())

        // create a marker symbol
        val buoyMarker =
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 10.0f)

        // create graphics
        return arrayOf(
            Graphic(buoy1Loc, buoyMarker),
            Graphic(buoy2Loc, buoyMarker),
            Graphic(buoy3Loc, buoyMarker),
            Graphic(buoy4Loc, buoyMarker)
        )
    }

    /**
     * Create graphics which display text at specific locations.
     *
     * @return a new graphic
     */
    private fun createTextGraphics(): Array<Graphic> {
        // create a point geometry
        val bassLocation =
            Point(-2.640631, 56.078083, SpatialReference.wgs84())
        val craigleithLocation =
            Point(-2.720324, 56.073569, SpatialReference.wgs84())

        // create text symbols
        val bassRockSymbol = TextSymbol(
            getString(R.string.bassrock),
            Color.blue,
            10.0f,
            HorizontalAlignment.Left, VerticalAlignment.Bottom
        )
        val craigleithSymbol = TextSymbol(
            getString(R.string.craigleith),
            Color.blue,
            10.0f,
            HorizontalAlignment.Right, VerticalAlignment.Top
        )

        // define graphics from each geometry and symbol
        val bassRockGraphic = Graphic(bassLocation, bassRockSymbol)
        val craigleithGraphic = Graphic(craigleithLocation, craigleithSymbol)

        return arrayOf(bassRockGraphic, craigleithGraphic)
    }

    /**
     * Create a graphic which displays a polyline.
     *
     * @return a new graphic
     */
    private fun createRoute(): Graphic {
        // define a polyline for the boat trip
        val boatRoute: Polyline = getBoatTripGeometry()
        // define a line symbol
        val lineSymbol =
            SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.magenta, 4.0f)

        // create and return a new graphic
        return Graphic(boatRoute, lineSymbol)
    }

    /**
     * Create a graphic which displays a polygon.
     *
     * @return a new graphic
     */
    private fun createNestingGround(): Graphic {
        // define the polygon for the nesting ground
        val nestingGround = getNestingGroundGeometry()
        // define the fill symbol and outline
        val outlineSymbol =
            SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.blue, 1.0f)
        val fillSymbol = SimpleFillSymbol(
            SimpleFillSymbolStyle.DiagonalCross, Color.green,
            outlineSymbol
        )

        // create and return a new graphic
        return Graphic(nestingGround, fillSymbol)
    }

    /**
     * Create a polyline representing the route of the boat trip.
     *
     * @return a new polyline
     */
    private fun getBoatTripGeometry(): Polyline {
        // a new point collection to make up the polyline
        val boatPositionsPolylineBuilder = PolylineBuilder(SpatialReference.wgs84()) {
            // add positions to the point collection
            addPoint(Point(-2.718479122792677, 56.06147084563517))
            addPoint(Point(-2.719680750046392, 56.06147084563517))
            addPoint(Point(-2.722084004553823, 56.06214171205971))
            addPoint(Point(-2.726375530459948, 56.06386674355254))
            addPoint(Point(-2.726890513568683, 56.06607083814320))
            addPoint(Point(-2.727062174604927, 56.06779569383808))
            addPoint(Point(-2.725517225278723, 56.06875391365391))
            addPoint(Point(-2.723113970771293, 56.06942465335233))
            addPoint(Point(-2.719165766937657, 56.07028701581465))
            addPoint(Point(-2.713672613777817, 56.07057446568132))
            addPoint(Point(-2.709381087871692, 56.07095772883556))
            addPoint(Point(-2.704402917820587, 56.07153261642126))
            addPoint(Point(-2.698223120515766, 56.07239493172226))
            addPoint(Point(-2.692386645283435, 56.07325722773041))
            addPoint(Point(-2.686721831087350, 56.07335303720707))
            addPoint(Point(-2.681228677927500, 56.07354465544585))
            addPoint(Point(-2.676422168912640, 56.07421531177896))
            addPoint(Point(-2.669899049535339, 56.07488595644139))
            addPoint(Point(-2.664749218447989, 56.07574819671591))
            addPoint(Point(-2.659427726324393, 56.07613140842321))
            addPoint(Point(-2.654792878345778, 56.07622721075461))
            addPoint(Point(-2.651359657620878, 56.07651461631978))
            addPoint(Point(-2.647754775859732, 56.07708942101955))
            addPoint(Point(-2.645008199279812, 56.07814320736718))
            addPoint(Point(-2.643291588917362, 56.08025069360931))
            addPoint(Point(-2.638656740938747, 56.08044227755186))
            addPoint(Point(-2.636940130576297, 56.07881378367495))
            addPoint(Point(-2.636425147467562, 56.07728102068079))
            addPoint(Point(-2.637798435757522, 56.07661041769850))
            addPoint(Point(-2.638656740938747, 56.07507756705851))
            addPoint(Point(-2.641231656482422, 56.07479015077557))
            addPoint(Point(-2.642776605808628, 56.07574819671591))
            addPoint(Point(-2.645694843424792, 56.07546078543464))
            addPoint(Point(-2.647239792750997, 56.07459853872940))
            addPoint(Point(-2.649299725185938, 56.07268236586862))
            addPoint(Point(-2.653076267983328, 56.07182005699860))
            addPoint(Point(-2.655479522490758, 56.07086191340429))
            addPoint(Point(-2.658741082179413, 56.07047864929729))
            addPoint(Point(-2.663375930158029, 56.07028701581465))
            addPoint(Point(-2.666637489846684, 56.07009538137926))
            addPoint(Point(-2.670070710571584, 56.06990374599109))
            addPoint(Point(-2.674190575441464, 56.06913719491074))
            addPoint(Point(-2.678310440311345, 56.06808316228391))
            addPoint(Point(-2.682086983108735, 56.06789151689155))
            addPoint(Point(-2.686893492123596, 56.06760404701653))
            addPoint(Point(-2.691185018029721, 56.06722075051504))
            addPoint(Point(-2.695133221863356, 56.06702910083509))
            addPoint(Point(-2.698223120515766, 56.06683745020233))
            addPoint(Point(-2.701656341240667, 56.06645414607839))
            addPoint(Point(-2.706119528183037, 56.06607083814320))
            addPoint(Point(-2.710067732016672, 56.06559169786458))
            addPoint(Point(-2.713329291705327, 56.06520838135397))
            addPoint(Point(-2.716762512430227, 56.06453756828941))
            addPoint(Point(-2.718307461756433, 56.06348340989081))
            addPoint(Point(-2.719165766937657, 56.06281256681154))
            addPoint(Point(-2.719852411082638, 56.06204587471371))
            addPoint(Point(-2.719165766937657, 56.06166252294756))
            addPoint(Point(-2.718307461756433, 56.06147084563517))
        }

        // create the polyline from the point collection
        return boatPositionsPolylineBuilder.toGeometry()
    }

    /**
     * Create a polygon from a point collection.
     *
     * @return a new polygon
     */
    private fun getNestingGroundGeometry(): Polygon {
        // a new point collection to make up the polygon
        val pointsPolygonBuilder = PolygonBuilder(SpatialReference.wgs84()) {
            // add points to the point collection
            addPoint(Point(-2.643077012566659, 56.07712534604447))
            addPoint(Point(-2.642819521015944, 56.07717324600376))
            addPoint(Point(-2.642540571836003, 56.07774804087097))
            addPoint(Point(-2.642712232869812, 56.07792766250863))
            addPoint(Point(-2.642454741319098, 56.07829887790651))
            addPoint(Point(-2.641853927700763, 56.07852639525372))
            addPoint(Point(-2.640974164902487, 56.07880180919243))
            addPoint(Point(-2.639987113958079, 56.07881378366685))
            addPoint(Point(-2.639407757968971, 56.07908919555142))
            addPoint(Point(-2.638764029092183, 56.07917301616904))
            addPoint(Point(-2.638485079912242, 56.07896945149566))
            addPoint(Point(-2.638570910429147, 56.07820308072684))
            addPoint(Point(-2.638785486721410, 56.07756841839600))
            addPoint(Point(-2.639193181676709, 56.07719719596109))
            addPoint(Point(-2.639944198699627, 56.07675411934114))
            addPoint(Point(-2.640652300464093, 56.07673016910844))
            addPoint(Point(-2.640673758093319, 56.07632301287509))
            addPoint(Point(-2.640180232621116, 56.07599967986049))
            addPoint(Point(-2.640244605508794, 56.07584400003405))
            addPoint(Point(-2.640416266542604, 56.07578412301025))
            addPoint(Point(-2.640888334385582, 56.07580807383093))
            addPoint(Point(-2.641768097183858, 56.07623918605773))
            addPoint(Point(-2.642197249768383, 56.07625116132851))
            addPoint(Point(-2.642840978645171, 56.07661041772168))
            addPoint(Point(-2.643077012566659, 56.07712534604447))
        }

        // create a polygon from the point collection
        return pointsPolygonBuilder.toGeometry()
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
