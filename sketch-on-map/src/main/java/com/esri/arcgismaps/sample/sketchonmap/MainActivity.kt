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
import com.arcgismaps.geometry.GeometryBuilder
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

    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    // create a symbol for the point graphic
    private var pointSymbol: SimpleMarkerSymbol = SimpleMarkerSymbol(
        SimpleMarkerSymbolStyle.Square,
        Color(-0x10000),
        20f
    )

    // create a symbol for a line graphic
    private var lineSymbol: SimpleLineSymbol = SimpleLineSymbol(
        SimpleLineSymbolStyle.Solid,
        Color(-0x7800),
        4f
    )

    // create a symbol for the fill graphic
    private var fillSymbol: SimpleFillSymbol = SimpleFillSymbol(
        SimpleFillSymbolStyle.Cross,
        Color(0x40FFA9A9),
        lineSymbol
    )

    // keep the instance graphic overlay to add graphics on the map
    private var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    // keep the instance to create new geometries, and change existing geometries
    private var geometryEditor: GeometryEditor? = GeometryEditor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        mapView.apply {
            map =  ArcGISMap(BasemapStyle.ArcGISLightGray)
            setViewpoint(Viewpoint(34.056295, -117.195800, 100000.0))
            graphicsOverlays.add(graphicsOverlay)
            this.geometryEditor = this@MainActivity.geometryEditor
        }
    }

    /**
     * When the point button is clicked, reset other buttons, show the point button as selected,
     * and start point drawing mode.
     */
    fun createModePoint(view: View) {
        setCurrentSelectionText("Point")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.pointButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Point)
    }

    /**
     * When the multipoint button is clicked, reset other buttons, show the multipoint button as
     * selected, and start multipoint drawing mode.
     */
    fun createModeMultipoint(view: View) {
        setCurrentSelectionText("Multipoint")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.multipointButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Multipoint)
    }

    /**
     * When the polyline button is clicked, reset other buttons, show the polyline button as
     * selected, and start polyline drawing mode.
     */
    fun createModePolyline(view: View) {
        setCurrentSelectionText("Polyline")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.polylineButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Polyline)
    }

    /**
     * When the polygon button is clicked, reset other buttons, show the polygon button as
     * selected, and start polygon drawing mode.
     */
    fun createModePolygon(view: View) {
        setCurrentSelectionText("Polygon")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.polygonButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.Polygon)
    }

    /**
     * When the freehand line button is clicked, reset other buttons, show the freehand line
     * button as selected, and start freehand line drawing mode.
     */
    fun createModeFreehandLine(view: View) {
        setCurrentSelectionText("FreehandPolyline")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.freehandLineButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.FreehandPolyline)
    }

    /**
     * When the freehand polygon button is clicked, reset other buttons, show the freehand
     * polygon button as selected, and enable freehand polygon drawing mode.
     */
    fun createModeFreehandPolygon(view: View) {
        setCurrentSelectionText("FreehandPolygon")
        resetButtons()
        activityMainBinding.pointLinePolygonToolbar.freehandPolygonButton.isSelected = true
        geometryEditor?.start(GeometryEditorCreationMode.FreehandPolygon)
    }

    /**
     * When the undo button is clicked, undo the last event on the SketchEditor.
     */
    fun undo(view: View) {
        if (geometryEditor?.canUndo?.value == true) {
            geometryEditor?.undo()
            setCurrentSelectionText(getString(R.string.undo))
        }
    }

    /**
     * When the redo button is clicked, redo the last undone event on the SketchEditor.
     */
    fun redo(view: View) {
        if (geometryEditor?.canRedo?.value == true) {
            geometryEditor?.redo()
            setCurrentSelectionText(getString(R.string.redo))
        }
    }

    /**
     * When the stop button is clicked, check that sketch is valid. If so, get the geometry from
     * the sketch, set its symbol and add it to the graphics overlay.
     */
    fun stop(view: View) {
        // get the geometry from sketch editor
        val sketchGeometry = geometryEditor?.geometry?.value
            ?: return showError("Error retrieving geometry")

        if(GeometryBuilder.builder(sketchGeometry)?.isSketchValid == false){
            return reportNotValid()
        }

        // stops the editing session
        geometryEditor?.stop()

        // clear button selection
        resetButtons()

        // create a graphic from the sketch editor geometry
        val graphic = Graphic(sketchGeometry)
        // assign a symbol based on geometry type
        when (sketchGeometry) {
            is Polygon -> graphic.symbol = fillSymbol
            is Polyline -> graphic.symbol = lineSymbol
            is Point, is Multipoint -> graphic.symbol = pointSymbol
            else -> {}
        }

        // add the graphic to the graphics overlay
        graphicsOverlay.graphics.add(graphic)
        setCurrentSelectionText("Added graphic to map")
    }

    /**
     * De-selects all buttons.
     */
    private fun resetButtons() {
        activityMainBinding.pointLinePolygonToolbar.apply {
            pointButton.isSelected = false
            multipointButton.isSelected = false
            polylineButton.isSelected = false
            polygonButton.isSelected = false
            freehandLineButton.isSelected = false
            freehandPolygonButton.isSelected = false
        }
    }

    /**
     * Clear the MapView of all the graphics and reset selections
     */
    fun clear(view: View) {
        resetButtons()
        geometryEditor?.clearGeometry()
        geometryEditor?.clearSelection()
        setCurrentSelectionText("Cleared")
    }

    /**
     * Called if sketch is invalid. Reports to user why the sketch was invalid.
     */
    private fun reportNotValid() {
        val validIfText: String =
            when (geometryEditor?.creationMode) {
                is GeometryEditorCreationMode.Point -> {
                    getString(R.string.invalid_point_message)
                }
                is GeometryEditorCreationMode.Multipoint -> {
                    getString(R.string.invalid_multipoint_message)
                }
                is GeometryEditorCreationMode.Polyline, is GeometryEditorCreationMode.FreehandPolyline -> {
                    getString(R.string.invalid_polyline_message)
                }
                is GeometryEditorCreationMode.Polygon, is GeometryEditorCreationMode.FreehandPolygon -> {
                    getString(R.string.invalid_polygon_message)
                }
                else -> {
                    getString(R.string.none_selected_message)
                }
            }
        setCurrentSelectionText(validIfText)
    }

    /**
     * Set the current selection text to the [selectedItem]
     */
    private fun setCurrentSelectionText(selectedItem: String) {
        activityMainBinding.currentSelection.text =
            getString(R.string.current_selection, selectedItem)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
