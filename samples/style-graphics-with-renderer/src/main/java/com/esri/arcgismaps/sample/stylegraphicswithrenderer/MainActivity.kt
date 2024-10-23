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

package com.esri.arcgismaps.sample.stylegraphicswithrenderer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.AngularUnit
import com.arcgismaps.geometry.CubicBezierSegment
import com.arcgismaps.geometry.EllipticArcSegment
import com.arcgismaps.geometry.GeodesicEllipseParameters
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.MutablePart
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
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
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.stylegraphicswithrenderer.databinding.StyleGraphicsWithRendererActivityMainBinding

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: StyleGraphicsWithRendererActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.style_graphics_with_renderer_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        //  add a map with a topographic basemap style
        mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)
        mapView.setViewpoint(Viewpoint(15.169193, 16.333479, 100_000_000.0))

        // add graphics overlays
        mapView.graphicsOverlays.addAll(
            listOf(
                makeRenderedPointGraphicsOverlay(),
                makeRenderedLineGraphicsOverlay(),
                makeRenderedPolygonGraphicsOverlay(),
                makeRenderedCurvedPolygonGraphicsOverlay(),
                makeRenderedEllipseGraphicsOverlay()
            )
        )
    }

    /**
     * Make a point, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedPointGraphicsOverlay(): GraphicsOverlay {
        // create point
        val pointGeometry = Point(40e5, 40e5, SpatialReference.webMercator())
        // create graphic for point
        val pointGraphic = Graphic(pointGeometry)
        // red diamond point symbol
        val pointSymbol =
            SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.red, 10f)
        // create simple renderer
        val pointRenderer = SimpleRenderer(pointSymbol)
        // create a new graphics overlay with these settings and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(pointGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = pointRenderer
        }
    }

    /**
     * Create a polyline, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedLineGraphicsOverlay(): GraphicsOverlay {
        // create line
        val lineBuilder = PolylineBuilder(SpatialReference.webMercator()) {
            addPoint(-10e5, 40e5)
            addPoint(20e5, 50e5)
        }
        // create graphic for polyline
        val lineGraphic = Graphic(lineBuilder.toGeometry())
        // solid blue line symbol
        val lineSymbol =
            SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.blue, 5f)
        // create simple renderer
        val lineRenderer = SimpleRenderer(lineSymbol)

        // create graphic overlay for polyline and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(lineGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = lineRenderer
        }
    }

    /**
     * Create a polygon, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedPolygonGraphicsOverlay(): GraphicsOverlay {
        // create polygon
        val polygonBuilder = PolygonBuilder(SpatialReference.webMercator()) {
            addPoint(-20e5, 20e5)
            addPoint(20e5, 20e5)
            addPoint(20e5, -20e5)
            addPoint(-20e5, -20e5)
        }
        // create graphic for polygon
        val polygonGraphic = Graphic(polygonBuilder.toGeometry())
        // solid yellow polygon symbol
        val polygonSymbol =
            SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.yellow, null)
        // create simple renderer
        val polygonRenderer = SimpleRenderer(polygonSymbol)

        // create graphic overlay for polygon and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(polygonGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = polygonRenderer
        }
    }

    /**
     * Create a curved polygon, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedCurvedPolygonGraphicsOverlay(): GraphicsOverlay {
        // create a point for the center of the geometry
        val originPoint = Point(40e5, 5e5, SpatialReference.webMercator())
        // create polygon
        val curvedPolygonGeometry = makeHeartGeometry(originPoint, 10e5)
        // create graphic for polygon
        val polygonGraphic = Graphic(curvedPolygonGeometry)
        // create a simple fill symbol with outline
        val curvedLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.black, 1f)
        val curvedFillSymbol =
            SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.red, curvedLineSymbol)
        // create simple renderer
        val polygonRenderer = SimpleRenderer(curvedFillSymbol)

        // create graphic overlay for polygon and add it to the map view
        return GraphicsOverlay().apply {
            // add graphic to overlay
            graphics.add(polygonGraphic)
            // set the renderer on the graphics overlay to the new renderer
            renderer = polygonRenderer
        }
    }

    /**
     * Create a heart-shape geometry with Bezier and elliptic arc segments from a given [center]
     * point and [sideLength].
     */
    private fun makeHeartGeometry(center: Point, sideLength: Double): Geometry {
        val spatialReference = center.spatialReference
        // the x and y coordinates to simplify the calculation
        val minX = center.x - 0.5 * sideLength
        val minY = center.y - 0.5 * sideLength
        // the radius of the arcs
        val arcRadius = sideLength * 0.25

        // bottom left curve
        val leftCurveStart = Point(center.x, minY, spatialReference)
        val leftCurveEnd = Point(minX, minY + 0.75 * sideLength, spatialReference)
        val leftControlPoint1 = Point(center.x, minY + 0.25 * sideLength, spatialReference)
        val leftControlPoint2 = Point(minX, center.y, spatialReference)
        val leftCurve = CubicBezierSegment(
            leftCurveStart,
            leftControlPoint1,
            leftControlPoint2,
            leftCurveEnd,
            spatialReference
        )

        // top left arc
        val leftArcCenter =
            Point(minX + 0.25 * sideLength, minY + 0.75 * sideLength, spatialReference)
        val leftArc = EllipticArcSegment.createCircularEllipticArc(
            leftArcCenter,
            arcRadius,
            Math.PI,
            -Math.PI,
            spatialReference
        )

        // top right arc
        val rightArcCenter =
            Point(minX + 0.75 * sideLength, minY + 0.75 * sideLength, spatialReference)
        val rightArc = EllipticArcSegment.createCircularEllipticArc(
            rightArcCenter,
            arcRadius,
            Math.PI,
            -Math.PI,
            spatialReference
        )

        // bottom right curve
        val rightCurveStart = Point(minX + sideLength, minY + 0.75 * sideLength, spatialReference)
        val rightControlPoint1 = Point(minX + sideLength, center.y, spatialReference)
        val rightCurve = CubicBezierSegment(
            rightCurveStart,
            rightControlPoint1,
            leftControlPoint1,
            leftCurveStart,
            spatialReference
        )

        // create a mutable part list
        val heartParts = MutablePart.createWithSegments(
            listOf(leftCurve, leftArc, rightArc, rightCurve),
            spatialReference
        )
        // return the heart
        return Polygon(listOf(heartParts).asIterable())
    }

    /**
     * Create an ellipse, its graphic, a graphics overlay for it, and add it to the map view.
     */
    private fun makeRenderedEllipseGraphicsOverlay(): GraphicsOverlay {
        // create and set all the parameters so that the ellipse has a major axis of 400 kilometres,
        // a minor axis of 200 kilometres and is rotated at an angle of -45 degrees
        val parameters = GeodesicEllipseParameters.createForPolygon().apply {
            axisDirection = -45.0
            angularUnit = AngularUnit.degrees
            center = Point(40e5, 23e5, SpatialReference.webMercator())
            linearUnit = LinearUnit.kilometers
            maxPointCount = 100L
            maxSegmentLength = 20.0
            semiAxis1Length = 200.0
            semiAxis2Length = 400.0
        }

        // define the ellipse parameters to a polygon geometry
        val polygon = GeometryEngine.ellipseGeodesicOrNull(parameters)
        // set the ellipse fill color
        val ellipseSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Solid, Color.magenta, null)
        // return the purple ellipse
        return GraphicsOverlay().apply {
            // add the symbol to the renderer and add it to the graphic overlay
            renderer = SimpleRenderer(ellipseSymbol)
            graphics.add(Graphic(polygon))
        }
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }

    private val Color.Companion.yellow: Color
        get() {
            return fromRgba(255, 255, 0, 255)
        }

    private val Color.Companion.magenta: Color
        get() {
            return fromRgba(255, 0, 255, 255)
        }
}
