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

package com.esri.arcgismaps.sample.rendermultilayersymbols

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.DashGeometricEffect
import com.arcgismaps.mapping.symbology.HatchFillSymbolLayer
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.MultilayerPointSymbol
import com.arcgismaps.mapping.symbology.MultilayerPolygonSymbol
import com.arcgismaps.mapping.symbology.MultilayerPolylineSymbol
import com.arcgismaps.mapping.symbology.MultilayerSymbol
import com.arcgismaps.mapping.symbology.PictureMarkerSymbolLayer
import com.arcgismaps.mapping.symbology.SolidFillSymbolLayer
import com.arcgismaps.mapping.symbology.SolidStrokeSymbolLayer
import com.arcgismaps.mapping.symbology.StrokeSymbolLayerCapStyle
import com.arcgismaps.mapping.symbology.SymbolAnchor
import com.arcgismaps.mapping.symbology.SymbolAnchorPlacementMode
import com.arcgismaps.mapping.symbology.SymbolLayer
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VectorMarkerSymbolElement
import com.arcgismaps.mapping.symbology.VectorMarkerSymbolLayer
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.esri.arcgismaps.sample.rendermultilayersymbols.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

// define offset used to keep a consistent distance between symbols in the same column
private const val OFFSET = 20.0

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // define the graphics overlay to add the multilayer symbols
    private var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        mapView.apply {
            // set the map to be displayed in the layout's map view
            mapView.map = ArcGISMap(BasemapStyle.ArcGISLightGray)
            // add the graphic overlay to the map view
            graphicsOverlays.add(graphicsOverlay)
        }

        // create labels to go above each category of graphic
        addTextGraphics()

        // create picture marker symbols, from URI or local cache
        addImageGraphics()

        // add graphics with simple vector marker symbol elements (MultilayerPoint Simple Markers on app UI)
        val solidFillSymbolLayer = SolidFillSymbolLayer(Color.red)
        val multilayerPolygonSymbol = MultilayerPolygonSymbol(listOf(solidFillSymbolLayer))
        val solidStrokeSymbolLayer =
            SolidStrokeSymbolLayer(1.0, Color.red, listOf(DashGeometricEffect()))
        val multilayerPolylineSymbol = MultilayerPolylineSymbol(listOf(solidStrokeSymbolLayer))

        // define vector element for a diamond, triangle and cross
        val diamondGeometry =
            Geometry.fromJsonOrNull("{\"rings\":[[[0.0,2.5],[2.5,0.0],[0.0,-2.5],[-2.5,0.0],[0.0,2.5]]]}")
        val triangleGeometry =
            Geometry.fromJsonOrNull("{\"rings\":[[[0.0,5.0],[5,-5.0],[-5,-5.0],[0.0,5.0]]]}")
        val crossGeometry =
            Geometry.fromJsonOrNull("{\"paths\":[[[-1,1],[0,0],[1,-1]],[[1,1],[0,0],[-1,-1]]]}")

        if (diamondGeometry == null || triangleGeometry == null || crossGeometry == null) {
            showError("Error reading geometry from json")
            return
        }

        // add red diamond graphic
        addGraphicsWithVectorMarkerSymbolElements(
            multilayerPolygonSymbol, diamondGeometry, 0.0
        )
        // add red triangle graphic
        addGraphicsWithVectorMarkerSymbolElements(
            multilayerPolygonSymbol, triangleGeometry, OFFSET
        )
        // add red cross graphic
        addGraphicsWithVectorMarkerSymbolElements(
            multilayerPolylineSymbol, crossGeometry, 2 * OFFSET
        )

        // create line marker symbols with short dash dot dot
        addLineGraphicsWithMarkerSymbols(
            listOf(4.0, 6.0, 0.5, 6.0, 0.5, 6.0), 0.0
        )
        // create line marker symbol with short dash
        addLineGraphicsWithMarkerSymbols(
            listOf(4.0, 6.0), OFFSET
        )
        // create line marker symbol with dash dot dot
        addLineGraphicsWithMarkerSymbols(
            listOf(7.0, 9.0, 0.5, 9.0), 2 * OFFSET
        )

        // create polygon marker symbols
        // cross-hatched diagonal lines
        addPolygonGraphicsWithMarkerSymbols(
            listOf(-45.0, 45.0), 0.0
        )
        // hatched diagonal lines
        addPolygonGraphicsWithMarkerSymbols(
            listOf(-45.0), OFFSET
        )
        // hatched vertical lines
        addPolygonGraphicsWithMarkerSymbols(
            listOf(90.0), 2 * OFFSET
        )

        // define vector element for a hexagon which will be used as the basis of a complex point
        val complexPointGeometry =
            Geometry.fromJsonOrNull("{\"rings\":[[[-2.89,5.0],[2.89,5.0],[5.77,0.0],[2.89,-5.0],[-2.89,-5.0],[-5.77,0.0],[-2.89,5.0]]]}")

        // create the more complex multilayer graphics: a point, polygon, and polyline
        complexPointGeometry?.let { addComplexPoint(it) }
        addComplexPolygon()
        addComplexPolyline()
    }

    /**
     * Creates the label graphics to be displayed above each category of symbol,
     * and adds them to the graphics overlay.
     */
    private fun addTextGraphics() {
        graphicsOverlay.graphics.addAll(
            listOf(
                Graphic(
                    Point(-150.0, 50.0, SpatialReference.wgs84()),
                    getTextSymbol("MultilayerPoint\nSimple Markers")
                ), Graphic(
                    Point(-80.0, 50.0, SpatialReference.wgs84()),
                    getTextSymbol("MultilayerPoint\nPicture Markers")
                ), Graphic(
                    Point(0.0, 50.0, SpatialReference.wgs84()),
                    getTextSymbol("Multilayer\nPolyline")
                ), Graphic(
                    Point(65.0, 50.0, SpatialReference.wgs84()),
                    getTextSymbol("Multilayer\nPolygon")
                ), Graphic(
                    Point(130.0, 50.0, SpatialReference.wgs84()),
                    getTextSymbol("Multilayer\nComplex Symbols")
                )
            )
        )
    }

    /**
     * @return the TextSymbol with the [text] to be displayed on the map.
     */
    private fun getTextSymbol(text: String): TextSymbol {
        val textSymbol = TextSymbol(
            text, Color.black, 10F, HorizontalAlignment.Center, VerticalAlignment.Middle
        )
        // give the text symbol a white background
        textSymbol.backgroundColor = Color.white
        return textSymbol
    }

    /**
     * Create picture marker symbols from online URI and local cache.
     */
    private fun addImageGraphics() {
        // URI of image to display
        val blueTentImageURI =
            "https://static.arcgis.com/images/Symbols/OutdoorRecreation/Camping.png"
        // load the PictureMarkerSymbolLayer using the image URI
        val pictureMarkerFromUri = PictureMarkerSymbolLayer(blueTentImageURI)

        lifecycleScope.launch {
            pictureMarkerFromUri.load().getOrElse {
                showError("Picture marker symbol layer failed to load from URI: ${it.message}")
            }
            // add loaded layer to the map
            addGraphicFromPictureMarkerSymbolLayer(pictureMarkerFromUri, 0.0)
            // load blue pin from as a bitmap
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(resources, R.drawable.blue_pin)
            }
            // load the PictureMarkerSymbolLayer using the bitmap drawable
            val pictureMarkerFromCache = withContext(Dispatchers.IO) {
                PictureMarkerSymbolLayer.createWithImage(BitmapDrawable(resources, bitmap))
            }
            pictureMarkerFromCache.load().getOrElse {
                showError("Picture marker symbol layer failed to load from bitmap: ${it.message}")
            }
            // add loaded layer to the map
            addGraphicFromPictureMarkerSymbolLayer(pictureMarkerFromCache, 40.0)
        }
    }

    /**
     * Loads a picture marker symbol layer and after it has loaded, creates a new multilayer point symbol from it.
     * A graphic is created from the multilayer point symbol and added to the graphics overlay.
     *
     * The [pictureMarkerSymbolLayer] to be loaded.
     * The [offset] value used to keep a consistent distance between symbols in the same column.
     *
     */
    private suspend fun addGraphicFromPictureMarkerSymbolLayer(
        pictureMarkerSymbolLayer: PictureMarkerSymbolLayer, offset: Double
    ) = withContext(Dispatchers.IO) {
        // wait for the picture marker symbol layer to load and check it has loaded
        pictureMarkerSymbolLayer.load().getOrElse {
            showError("Picture marker symbol layer failed to load: ${it.message}")
        }
        // set the size of the layer and create a new multilayer point symbol from it
        pictureMarkerSymbolLayer.size = 40.0
        val multilayerPointSymbol = MultilayerPointSymbol(listOf(pictureMarkerSymbolLayer))
        // create location for the symbol
        val point = Point(-80.0, 20.0 - offset, SpatialReference.wgs84())

        // create graphic with the location and symbol and add it to the graphics overlay
        val graphic = Graphic(point, multilayerPointSymbol)
        graphicsOverlay.graphics.add(graphic)
    }

    /**
     * Adds new graphics constructed from multilayer point symbols.
     *
     * The [multilayerSymbol] to construct the vector marker symbol element with.
     * The input [geometry] for the vector marker symbol element.
     * [offset] the value used to keep a consistent distance between symbols in the same column.
     */
    private fun addGraphicsWithVectorMarkerSymbolElements(
        multilayerSymbol: MultilayerSymbol, geometry: Geometry, offset: Double
    ) {
        // define a vector element and create a new multilayer point symbol from it
        val vectorMarkerSymbolElement = VectorMarkerSymbolElement(geometry, multilayerSymbol)
        val vectorMarkerSymbolLayer = VectorMarkerSymbolLayer(listOf(vectorMarkerSymbolElement))
        val multilayerPointSymbol = MultilayerPointSymbol(listOf(vectorMarkerSymbolLayer))

        // create point graphic using the symbol and add it to the graphics overlay
        val graphic =
            Graphic(Point(-150.0, 20 - offset, SpatialReference.wgs84()), multilayerPointSymbol)
        graphicsOverlay.graphics.add(graphic)
    }

    /**
     * Adds new graphics constructed from multilayer polyline symbols.
     *
     * The pattern of [dashSpacing] dots/dashes used by the line and
     * [offset] the value used to keep a consistent distance between symbols in the same column.
     */
    private fun addLineGraphicsWithMarkerSymbols(dashSpacing: List<Double>, offset: Double) {
        // create a dash effect from the provided values
        val dashGeometricEffect = DashGeometricEffect(dashSpacing)
        // create stroke used by line symbols
        val solidStrokeSymbolLayer = SolidStrokeSymbolLayer(
            3.0, Color.red, listOf(dashGeometricEffect)
        ).apply {
            capStyle = StrokeSymbolLayerCapStyle.Round
        }
        // create a polyline for the multilayer polyline symbol
        val polylineBuilder = PolylineBuilder(SpatialReference.wgs84()) {
            addPoint(Point(-30.0, 20 - offset))
            addPoint(Point(30.0, 20 - offset))
        }
        // create a multilayer polyline symbol from the solidStrokeSymbolLayer
        val multilayerPolylineSymbol = MultilayerPolylineSymbol(listOf(solidStrokeSymbolLayer))
        // create a polyline graphic with geometry using the symbol created above, and add it to the graphics overlay
        graphicsOverlay.graphics.add(
            Graphic(
                polylineBuilder.toGeometry(), multilayerPolylineSymbol
            )
        )
    }

    /**
     * Adds new graphics constructed from multilayer polygon symbols.
     *
     * Takes a list containing the [angles] at which to draw fill lines within the polygon and
     * [offset] the value used to keep a consistent distance between symbols in the same column.
     */
    private fun addPolygonGraphicsWithMarkerSymbols(angles: List<Double>, offset: Double) {
        val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()) {
            addPoint(Point(60.0, 25 - offset))
            addPoint(Point(70.0, 25 - offset))
            addPoint(Point(70.0, 20 - offset))
            addPoint(Point(60.0, 20 - offset))
        }

        // create a stroke symbol layer to be used by patterns
        val strokeForHatches = SolidStrokeSymbolLayer(2.0, Color.red, listOf(DashGeometricEffect()))

        // create a stroke symbol layer to be used as an outline for aforementioned patterns
        val strokeForOutline =
            SolidStrokeSymbolLayer(1.0, Color.black, listOf(DashGeometricEffect()))

        // create a list to hold all necessary symbol layers - at least one for patterns and one for an outline at the end
        val symbolLayerList = mutableListOf<SymbolLayer>()

        // for each angle, create a symbol layer using the pattern stroke, with hatched lines at the given angle
        for (i in angles.indices) {
            val hatchFillSymbolLayer = HatchFillSymbolLayer(
                MultilayerPolylineSymbol(listOf(strokeForHatches)), angles[i]
            )
            // define separation distance for lines and add them to the symbol layer list
            hatchFillSymbolLayer.separation = 9.0
            symbolLayerList.add(hatchFillSymbolLayer)
        }

        // assign the outline layer to the last element of the symbol layer list
        symbolLayerList.add(strokeForOutline)
        // create a multilayer polygon symbol from the symbol layer list
        val multilayerPolygonSymbol =
            MultilayerPolygonSymbol(symbolLayerList)
        // create a polygon graphic with geometry using the symbol created above, and add it to the graphics overlay
        val graphic = Graphic(polygonBuilder.toGeometry(), multilayerPolygonSymbol)
        graphicsOverlay.graphics.add(graphic)
    }

    /**
     * Creates a complex point from multiple symbol layers and a provided geometry.
     * @param complexPointGeometry a base geometry upon which other symbol layers are drawn.
     */
    private fun addComplexPoint(complexPointGeometry: Geometry) {
        // create marker layers for complex point
        val orangeSquareVectorMarkerLayer: VectorMarkerSymbolLayer =
            getLayerForComplexPoint(Color.cyan, Color.blue, 11.0)
        val blackSquareVectorMarkerLayer: VectorMarkerSymbolLayer =
            getLayerForComplexPoint(Color.black, Color.cyan, 6.0)
        val purpleSquareVectorMarkerLayer: VectorMarkerSymbolLayer = getLayerForComplexPoint(
            Color.transparent, Color.magenta, 14.0
        )

        // set anchors for marker layers
        orangeSquareVectorMarkerLayer.anchor =
            SymbolAnchor(-4.0, -6.0, SymbolAnchorPlacementMode.Absolute)
        blackSquareVectorMarkerLayer.anchor =
            SymbolAnchor(2.0, 1.0, SymbolAnchorPlacementMode.Absolute)
        purpleSquareVectorMarkerLayer.anchor =
            SymbolAnchor(4.0, 2.0, SymbolAnchorPlacementMode.Absolute)

        // create a yellow hexagon with a black outline
        val yellowFillLayer = SolidFillSymbolLayer(Color.yellow)
        val blackOutline = SolidStrokeSymbolLayer(2.0, Color.black, listOf(DashGeometricEffect()))
        val hexagonVectorElement = VectorMarkerSymbolElement(
            complexPointGeometry, MultilayerPolylineSymbol(listOf(yellowFillLayer, blackOutline))
        )
        val hexagonVectorMarkerLayer = VectorMarkerSymbolLayer(listOf(hexagonVectorElement)).apply {
            size = 35.0
        }

        // create the multilayer point symbol
        val multilayerPointSymbol = MultilayerPointSymbol(
            listOf(
                hexagonVectorMarkerLayer,
                orangeSquareVectorMarkerLayer,
                blackSquareVectorMarkerLayer,
                purpleSquareVectorMarkerLayer
            )
        )

        // create the multilayer point graphic using the symbols created above
        val complexPointGraphic =
            Graphic(Point(130.0, 20.0, SpatialReference.wgs84()), multilayerPointSymbol)
        graphicsOverlay.graphics.add(complexPointGraphic)
    }

    /**
     * Creates a symbol layer for use in the composition of a complex point
     * using [fillColor], [outlineColor] as colors and the [size] of the symbol
     * Then return a [VectorMarkerSymbolLayer] of the created symbol.
     */
    private fun getLayerForComplexPoint(
        fillColor: Color, outlineColor: Color, size: Double
    ): VectorMarkerSymbolLayer {
        // create the fill layer and outline
        val fillLayer = SolidFillSymbolLayer(fillColor)
        val outline = SolidStrokeSymbolLayer(
            2.0, outlineColor, listOf(DashGeometricEffect())
        )
        // create a geometry from an envelope
        val geometry = Envelope(
            Point(-0.5, -0.5, SpatialReference.wgs84()), Point(0.5, 0.5, SpatialReference.wgs84())
        )
        //create a symbol element using the geometry, fill layer, and outline
        val vectorMarkerSymbolElement = VectorMarkerSymbolElement(
            geometry, MultilayerPolygonSymbol(listOf(fillLayer, outline))
        )
        // create a symbol layer containing just the above symbol element, set its size, and return it
        val vectorMarkerSymbolLayer =
            VectorMarkerSymbolLayer(listOf(vectorMarkerSymbolElement)).apply {
                this.size = size
            }
        return vectorMarkerSymbolLayer
    }

    /**
     * Adds a complex polygon generated with multiple symbol layers.
     */
    private fun addComplexPolygon() {
        // create the multilayer polygon symbol
        val multilayerPolygonSymbol = MultilayerPolygonSymbol(getLayersForComplexPolys(true))
        // create the polygon
        val polygonBuilder = PolygonBuilder(SpatialReference.wgs84()) {
            addPoint(Point(120.0, 0.0))
            addPoint(Point(140.0, 0.0))
            addPoint(Point(140.0, -10.0))
            addPoint(Point(120.0, -10.0))
        }
        // create a multilayer polygon graphic with geometry using the symbols
        // created above and add it to the graphics overlay
        graphicsOverlay.graphics.add(
            Graphic(
                polygonBuilder.toGeometry(), multilayerPolygonSymbol
            )
        )
    }

    /**
     * Adds a complex polyline generated with multiple symbol layers.
     */
    private fun addComplexPolyline() {
        // create the multilayer polyline symbol
        val multilayerPolylineSymbol = MultilayerPolylineSymbol(getLayersForComplexPolys(false))
        val polylineBuilder = PolylineBuilder(SpatialReference.wgs84()) {
            addPoint(Point(120.0, -25.0))
            addPoint(Point(140.0, -25.0))
        }
        // create the multilayer polyline graphic with geometry using the symbols created above and add it to the graphics overlay
        graphicsOverlay.graphics.add(
            Graphic(
                polylineBuilder.toGeometry(), multilayerPolylineSymbol
            )
        )
    }

    /**
     * Generates and returns the symbol layers used by the addComplexPolygon and addComplexPolyline methods.
     * [includeRedFill] indicates whether to include the red fill needed by the complex polygon.
     * @return a list of symbol layers including the necessary effects.
     */
    private fun getLayersForComplexPolys(includeRedFill: Boolean): List<SymbolLayer> {
        // create a black dash effect
        val blackDashes = SolidStrokeSymbolLayer(
            1.0, Color.black, listOf(DashGeometricEffect(listOf(5.0, 3.0)))
        ).apply {
            capStyle = StrokeSymbolLayerCapStyle.Square
        }

        // create a black outline
        val blackOutline = SolidStrokeSymbolLayer(
            7.0, Color.black, listOf(DashGeometricEffect())
        ).apply {
            capStyle = StrokeSymbolLayerCapStyle.Round
        }

        // create a yellow stroke inside
        val yellowStroke = SolidStrokeSymbolLayer(
            5.0, Color.yellow, listOf(DashGeometricEffect())
        ).apply {
            capStyle = StrokeSymbolLayerCapStyle.Round
        }

        return if (includeRedFill) {
            // create a red filling for the polygon
            val redFillLayer = SolidFillSymbolLayer(Color.red)
            listOf(redFillLayer, blackOutline, yellowStroke, blackDashes)
        } else {
            listOf(blackOutline, yellowStroke, blackDashes)
        }
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
