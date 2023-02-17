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

package com.esri.arcgismaps.sample.sketchonmap

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
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
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditorCreationMode
import com.esri.arcgismaps.sample.sketchonmap.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private var pointSymbol: SimpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Square, Color(-0x10000), 20f)
    private var lineSymbol: SimpleLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color(-0x7800), 4f)
    private var fillSymbol: SimpleFillSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Cross, Color(0x40FFA9A9), lineSymbol)
    private var geometryEditor: GeometryEditor? = null
    private var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // define symbols
        fillSymbol = SimpleFillSymbol(SimpleFillSymbolStyle.Cross, Color(0x40FFA9A9), lineSymbol)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISLightGray)
        mapView.map = map
        mapView.setViewpoint(Viewpoint(34.056295, -117.195800, 100000.0))
        mapView.graphicsOverlays.add(graphicsOverlay)

        // create a new sketch editor and add it to the map view
        geometryEditor = GeometryEditor()
        mapView.geometryEditor = geometryEditor
    }

    /**
     * When the point button is clicked, reset other buttons, show the point button as selected, and start point
     * drawing mode.
     */
    fun createModePoint(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.pointButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Point)
    }

    /**
     * When the multipoint button is clicked, reset other buttons, show the multipoint button as selected, and start
     * multipoint drawing mode.
     */
    fun createModeMultipoint(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.pointButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Multipoint)
    }

    /**
     * When the polyline button is clicked, reset other buttons, show the polyline button as selected, and start
     * polyline drawing mode.
     */
    fun createModePolyline(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.polylineButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Polyline)
    }

    /**
     * When the polygon button is clicked, reset other buttons, show the polygon button as selected, and start polygon
     * drawing mode.
     */
    fun createModePolygon(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.polygonButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Polygon)
    }

    /**
     * When the freehand line button is clicked, reset other buttons, show the freehand line button as selected, and
     * start freehand line drawing mode.
     */
    fun createModeFreehandLine(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.freehandLineButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.FreehandPolyline)
    }

    /**
     * When the freehand polygon button is clicked, reset other buttons, show the freehand polygon button as selected,
     * and enable freehand polygon drawing mode.
     */
    fun createModeFreehandPolygon(view: View) {
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.freehandPolygonButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.FreehandPolygon)
    }

    /**
     * De-selects all buttons.
     */
    private fun resetButtons() {
        activityMainBinding.pointLinePolygonToolbar.apply {
            pointButton.isSelected = false
            pointsButton.isSelected = false
            polylineButton.isSelected = false
            polygonButton.isSelected = false
            freehandLineButton.isSelected = false
            freehandPolygonButton.isSelected = false
        }
    }

    /**
     * When the undo button is clicked, undo the last event on the SketchEditor.
     */
    fun undo(view :View) {
        if (geometryEditor?.canUndo?.value == true) {
            geometryEditor?.undo()
        }
    }

    /**
     * When the redo button is clicked, redo the last undone event on the SketchEditor.
     */
    fun redo(view :View) {
        if (geometryEditor?.canRedo?.value == true) {
            geometryEditor?.redo()
        }
    }

    /**
     * When the stop button is clicked, check that sketch is valid. If so, get the geometry from the sketch, set its
     * symbol and add it to the graphics overlay.
     */
    fun stop(view :View) {
        // get the geometry from sketch editor
        val sketchGeometry = geometryEditor?.geometry?.value ?: return reportNotValid()
        geometryEditor?.stop()
        resetButtons()

        // create a graphic from the sketch editor geometry
        val graphic = Graphic(sketchGeometry)
        // assign a symbol based on geometry type
        when (sketchGeometry) {
            is Polygon -> graphic.symbol = fillSymbol
            is Polyline -> graphic.symbol = lineSymbol
            is Point, is Multipoint -> graphic.symbol = pointSymbol
            is Envelope -> TODO()
        }

        // add the graphic to the graphics overlay
        graphicsOverlay.graphics.add(graphic)
    }

    /**
     * Called if sketch is invalid. Reports to user why the sketch was invalid.
     */
    private fun reportNotValid() {
        val validIf: String =
            when {
                geometryEditor?.creationMode === GeometryEditorCreationMode.Point -> "Point only valid if it contains an x & y coordinate."
                geometryEditor?.creationMode === GeometryEditorCreationMode.Multipoint -> "Multipoint only valid if it contains at least one vertex."
                geometryEditor?.creationMode === GeometryEditorCreationMode.Polyline || geometryEditor?.creationMode === GeometryEditorCreationMode.FreehandPolyline -> "Polyline only valid if it contains at least one part of 2 or more vertices."
                geometryEditor?.creationMode === GeometryEditorCreationMode.Polygon || geometryEditor?.creationMode === GeometryEditorCreationMode.FreehandPolygon -> "Polygon only valid if it contains at least one part of 3 or more vertices which form a closed ring."
                else -> "No sketch creation mode selected."
            }
        showError("Sketch geometry invalid: $validIf")
    }


    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
