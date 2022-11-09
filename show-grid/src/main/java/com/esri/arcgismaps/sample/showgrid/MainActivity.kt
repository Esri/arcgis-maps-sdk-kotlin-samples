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

package com.esri.arcgismaps.sample.showgrid

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.LineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Grid
import com.arcgismaps.mapping.view.GridLabelPosition
import com.arcgismaps.mapping.view.LatitudeLongitudeGrid
import com.arcgismaps.mapping.view.LatitudeLongitudeGridLabelFormat
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.MgrsGrid
import com.arcgismaps.mapping.view.UsngGrid
import com.arcgismaps.mapping.view.UtmGrid
import com.esri.arcgismaps.sample.showgrid.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.showgrid.databinding.PopupMenuBinding

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val menuButton: Button by lazy {
        activityMainBinding.menuButton
    }

    // create a point to focus the map on in Quebec province
    private val center: Point by lazy {
        Point(
            -7702852.905619, 6217972.345771, SpatialReference(3857)
        )
    }

    private var lineColor: Color = Color.white
    private var labelColor: Color = Color.white
    private var labelPosition = GridLabelPosition.AllSides
    private var isLabelVisible = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        mapView.apply {
            // create a map with imagery basemap
            map = ArcGISMap(BasemapStyle.ArcGISImagery)
            // set the initial viewpoint of the map
            setViewpoint(Viewpoint(center, 23227.0))
            // set defaults on grid
            grid = LatitudeLongitudeGrid()
        }

        // set up a popup menu to manage grid settings
        val builder = AlertDialog.Builder(this@MainActivity)
        val popUpMenuBinding = PopupMenuBinding.inflate(layoutInflater)

        builder.setView(popUpMenuBinding.root)
        val dialog = builder.create()

        // set up options in popup menu
        // create drop-down list of different layer types
        setupLayerSpinner(popUpMenuBinding)

        // create drop-down list of different line colors
        setupLineColorSpinner(popUpMenuBinding)

        // create drop-down list of different label colors
        setupLabelColorSpinner(popUpMenuBinding)

        // create drop-down list of different label positions
        setupLabelPositionSpinner(popUpMenuBinding)

        // setup the checkbox to change the visibility of the labels
        setupLabelsCheckbox(popUpMenuBinding)

        // display pop-up box when button is clicked
        menuButton.setOnClickListener { dialog.show() }
    }

    /**
     * Sets up the spinner for selecting a grid type
     * and handles behavior for when a new grid type is selected.
     *
     * @param popupMenuBinding the popup binding inflated in onCreate()
     */
    private fun setupLayerSpinner(popupMenuBinding: PopupMenuBinding) {
        popupMenuBinding.layerSpinner.apply {
            // create drop-down list of different grids
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.layers_array)
            ).also { gridAdapter ->
                gridAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            // change between different grids on the mapView
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View, position: Int, id: Long
                ) {
                    // set the grid type and move to the starting point over 1 second.
                    when (position) {
                        0 -> {
                            // LatitudeLongitudeGrid can have a label format of DECIMAL_DEGREES or DEGREES_MINUTES_SECONDS
                            mapView.grid = LatitudeLongitudeGrid().apply {
                                labelFormat = LatitudeLongitudeGridLabelFormat.DecimalDegrees
                            }
                            mapView.setViewpoint(Viewpoint(center, 23227.0))
                        }
                        1 -> {
                            mapView.grid = MgrsGrid()
                            mapView.setViewpoint(Viewpoint(center, 23227.0))
                        }
                        2 -> {
                            mapView.grid = UtmGrid()
                            mapView.setViewpoint(Viewpoint(center, 10000000.0))
                        }
                        3 -> {
                            mapView.grid = UsngGrid()
                            mapView.setViewpoint(Viewpoint(center, 23227.0))
                        }
                        else -> Toast.makeText(
                            this@MainActivity, "Unsupported option", Toast.LENGTH_SHORT
                        ).show()
                    }
                    // make sure settings persist on grid type change
                    setLabelVisibility(isLabelVisible)
                    changeGridColor(lineColor)
                    changeLabelColor(labelColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Sets up the spinner for selecting a line color and handles behavior for when a new line color is selected.
     *
     * @param popupMenuBinding the popup binding inflated in onCreate()
     */
    private fun setupLineColorSpinner(popupMenuBinding: PopupMenuBinding) {
        popupMenuBinding.lineColorSpinner.apply {
            // create drop-down list of different line colors
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.colors_array)
            ).also { colorAdapter ->
                colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // change grid lines color
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View, position: Int, id: Long
                ) { // set the color
                    when (position) {
                        0 -> lineColor = Color.red
                        1 -> lineColor = Color.white
                        2 -> lineColor = Color.blue
                        else -> Toast.makeText(
                            this@MainActivity, "Unsupported option", Toast.LENGTH_SHORT
                        ).show()
                    }
                    changeGridColor(lineColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Sets up the spinner for selecting a label color
     * and handles behavior for when a new label color is selected.
     *
     * @param popupMenuBinding the popup binding inflated in onCreate()
     */
    private fun setupLabelColorSpinner(popupMenuBinding: PopupMenuBinding) {
        popupMenuBinding.labelColorSpinner.apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.colors_array)
            ).also { labelColorAdapter ->
                labelColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // change grid labels color
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View, position: Int, id: Long
                ) {
                    // set the color
                    when (position) {
                        0 -> labelColor = Color.red
                        1 -> labelColor = Color.white
                        2 -> labelColor = Color.blue
                        else -> Toast.makeText(
                            this@MainActivity, "Unsupported option", Toast.LENGTH_SHORT
                        ).show()
                    }
                    changeLabelColor(labelColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Sets up the spinner for selecting a label position relative to the grid
     * and handles behavior for when a label position is selected.
     *
     * @param popupMenuBinding the popup binding inflated in onCreate()
     */
    private fun setupLabelPositionSpinner(popupMenuBinding: PopupMenuBinding) {
        popupMenuBinding.labelPositionSpinner.apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.positions_array)
            ).also { positionAdapter ->
                positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View, position: Int, id: Long
                ) {
                    // set the label position
                    when (position) {
                        0 -> labelPosition = GridLabelPosition.AllSides
                        1 -> labelPosition = GridLabelPosition.BottomLeft
                        2 -> labelPosition = GridLabelPosition.BottomRight
                        3 -> labelPosition = GridLabelPosition.Center
                        4 -> labelPosition = GridLabelPosition.Geographic
                        5 -> labelPosition = GridLabelPosition.TopLeft
                        6 -> labelPosition = GridLabelPosition.TopRight
                        else -> Toast.makeText(
                            this@MainActivity, "Unsupported option", Toast.LENGTH_SHORT
                        ).show()
                    }
                    changeLabelPosition(labelPosition)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    /**
     * Sets up the spinner for the checkbox making labels visible or invisible.
     *
     * @param popupMenuBinding the popup binding inflated in onCreate()
     */
    private fun setupLabelsCheckbox(popupMenuBinding: PopupMenuBinding) {
        popupMenuBinding.labelsCheckBox.apply {
            isChecked = true
            // hide and show label visibility when the checkbox is clicked
            setOnClickListener {
                isLabelVisible = isChecked
                setLabelVisibility(isLabelVisible)
            }
        }
    }

    /**
     * Sets the labels as visible or invisible.
     *
     * @param visible whether the labels should be visible
     */
    private fun setLabelVisibility(visible: Boolean) {
        val grid = mapView.grid ?: return
        grid.labelVisibility = visible
    }

    /**
     * Sets the color of the grid lines.
     *
     * @param color the integer color to use
     */
    private fun changeGridColor(color: Color) {
        val grid = mapView.grid ?: return
        val gridLevels = grid.levelCount
        for (gridLevel in 0 until gridLevels) {
            val lineSymbol: LineSymbol =
                SimpleLineSymbol(SimpleLineSymbolStyle.Solid, color, (gridLevel + 1).toFloat())
            grid.setLineSymbol(gridLevel, lineSymbol)
        }
    }

    /**
     * Sets the color of the labels on the grid.
     *
     * @param labelColor the integer color to use
     */
    private fun changeLabelColor(labelColor: Color) {
        val grid = mapView.grid ?: return
        val gridLevels = grid.levelCount
        for (gridLevel in 0 until gridLevels) {
            val textSymbol = TextSymbol().apply {
                color = labelColor
                size = 14f
                horizontalAlignment = HorizontalAlignment.Left
                verticalAlignment = VerticalAlignment.Bottom
                haloColor = Color.white
                haloWidth = gridLevel + 1.toFloat()
            }
            grid.setTextSymbol(gridLevel, textSymbol)
        }
    }

    /**
     * Sets the position of the labels on the grid.
     *
     * @param labelPosition the LabelPosition to use
     */
    private fun changeLabelPosition(labelPosition: GridLabelPosition) {
        val grid = mapView.grid ?: return
        grid.labelPosition = labelPosition
    }

    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }
}
