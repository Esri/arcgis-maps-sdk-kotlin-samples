/* Copyright 2023 Esri
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryBuilder
import com.arcgismaps.geometry.GeometryType
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
import com.arcgismaps.mapping.view.geometryeditor.FreehandTool
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.VertexTool
import com.esri.arcgismaps.sample.sketchonmap.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val selectedGeometryDropdown by lazy {
        activityMainBinding.pointLinePolygonToolbar.selectGeometryDropdown
    }

    // create a symbol for the point graphic
    private val pointSymbol: SimpleMarkerSymbol by lazy {
        SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Square,
            Color(getColor(R.color.point_symbol_color)),
            20f)
    }

    // create a symbol for a line graphic
    private val lineSymbol: SimpleLineSymbol by lazy {
        SimpleLineSymbol(
            SimpleLineSymbolStyle.Solid,
            Color(getColor(R.color.line_symbol_color)),
            4f
        )
    }

    // create a symbol for the fill graphic
    private val fillSymbol: SimpleFillSymbol by lazy {
        SimpleFillSymbol(
            SimpleFillSymbolStyle.Cross,
            Color(getColor(R.color.fill_symbol_color)),
            lineSymbol
        )
    }

    // keep the instance graphic overlay to add graphics on the map
    private var graphicsOverlay: GraphicsOverlay = GraphicsOverlay()

    // keep the instance of the freehand tool
    private val freehandTool: FreehandTool = FreehandTool()

    // keep the instance of the vertex tool
    private val vertexTool: VertexTool = VertexTool()

    // keep the instance to create new geometries, and change existing geometries
    private var geometryEditor: GeometryEditor = GeometryEditor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        mapView.apply {
            map = ArcGISMap(BasemapStyle.ArcGISLightGray)
            setViewpoint(Viewpoint(34.056295, -117.195800, 100000.0))
            graphicsOverlays.add(graphicsOverlay)
        }

        // set MapView's geometry editor to sketch on map
        mapView.geometryEditor = geometryEditor

        // enable/disable the undo button if last event can be undone
        lifecycleScope.launch {
            geometryEditor.canUndo.collect { value ->
                activityMainBinding.pointLinePolygonToolbar.undoButton.isEnabled = value
            }
        }

        // enable/disable the redo button if the last event can be redone
        lifecycleScope.launch {
            geometryEditor.canRedo.collect { value ->
                activityMainBinding.pointLinePolygonToolbar.redoButton.isEnabled = value
            }
        }

        // set up the geometry list dropdown
        selectedGeometryDropdown.apply {
            // set the adapter to the list of geometries
            setAdapter(
                ArrayAdapter(
                    applicationContext,
                    android.R.layout.simple_list_item_1,
                    resources.getStringArray(R.array.geometry_list)
                )
            )

            // set the dropdown click listener
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                // set the GeometryEditorTool and then start the editing process
                geometryEditor.apply {
                    when (position) {
                        0 -> {
                            tool = freehandTool
                            start(GeometryType.Polygon)
                        }
                        1 -> {
                            tool = freehandTool
                            start(GeometryType.Polyline)
                        }
                        2 -> {
                            tool = vertexTool
                            start(GeometryType.Multipoint)
                        }
                        3 -> {
                            tool = vertexTool
                            start(GeometryType.Point)
                        }
                        4 -> {
                            tool = vertexTool
                            start(GeometryType.Polygon)
                        }
                        5 -> {
                            tool = vertexTool
                            start(GeometryType.Polyline)
                        }
                    }
                }
            }
        }
    }

    /**
     * Undo the last event on the GeometryEditor.
     */
    fun undo(view: View) {
        geometryEditor.undo()
    }

    /**
     * Redo the last undone event on the GeometryEditor.
     */
    fun redo(view: View) {
        geometryEditor.redo()
    }

    /**
     * When the stop button is clicked, check that sketch is valid. If so, get the geometry from
     * the sketch, set its symbol and add it to the graphics overlay.
     */
    fun stop(view: View) {
        // get the geometry from sketch editor
        val sketchGeometry = geometryEditor.geometry.value
            ?: return showMessage("Error retrieving geometry")

        if (GeometryBuilder.builder(sketchGeometry)?.isSketchValid == false) {
            return reportNotValid()
        }

        // stops the editing session
        geometryEditor.stop()

        // create a graphic from the sketch editor geometry
        val graphic = Graphic(sketchGeometry).apply {
            // assign a symbol based on geometry type
            symbol = when (sketchGeometry) {
                is Polygon -> fillSymbol
                is Polyline -> lineSymbol
                is Point, is Multipoint -> pointSymbol
                else -> null
            }
        }

        // add the graphic to the graphics overlay
        graphicsOverlay.graphics.add(graphic)
    }

    /**
     * Clear the MapView of all the graphics and reset selections
     */
    fun clear(view: View) {
        geometryEditor.clearGeometry()
        geometryEditor.clearSelection()
        geometryEditor.stop()
        selectedGeometryDropdown.setText("")
        selectedGeometryDropdown.clearFocus()
        showMessage(getString(R.string.cleared_message))
    }

    /**
     * Clear all editing and applied graphics on the map
     */
    fun restart(view: View) {
        graphicsOverlay.graphics.clear()
        geometryEditor.clearGeometry()
        geometryEditor.clearSelection()
        geometryEditor.stop()
        selectedGeometryDropdown.setText("")
        selectedGeometryDropdown.clearFocus()
        showMessage(getString(R.string.restart_message))
    }

    /**
     * Called if sketch is invalid. Reports to user why the sketch was invalid.
     */
    private fun reportNotValid() {
        // get the geometry currently being added to map
        val geometry = geometryEditor.geometry.value ?: return showMessage("Geometry not found")
        // find the geometry type, and set the valid message
        val validIfText: String = when (geometry) {
            is Point -> getString(R.string.invalid_point_message)
            is Multipoint -> getString(R.string.invalid_multipoint_message)
            is Polyline -> getString(R.string.invalid_polyline_message)
            is Polygon -> getString(R.string.invalid_polygon_message)
            else -> getString(R.string.none_selected_message)
        }
        // set the invalid message to the TextView.
        showMessage(validIfText)
    }

    private fun showMessage(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
