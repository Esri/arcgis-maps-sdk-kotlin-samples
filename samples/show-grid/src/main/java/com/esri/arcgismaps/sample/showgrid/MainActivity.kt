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

package com.esri.arcgismaps.sample.showgrid

import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.arcgismaps.mapping.view.GridLabelPosition
import com.arcgismaps.mapping.view.LatitudeLongitudeGrid
import com.arcgismaps.mapping.view.LatitudeLongitudeGridLabelFormat
import com.arcgismaps.mapping.view.MgrsGrid
import com.arcgismaps.mapping.view.UsngGrid
import com.arcgismaps.mapping.view.UtmGrid
import com.esri.arcgismaps.sample.showgrid.databinding.ShowGridActivityMainBinding
import com.esri.arcgismaps.sample.showgrid.databinding.PopupDialogBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ShowGridActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.show_grid_activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val menuButton: MaterialButton by lazy {
        activityMainBinding.menuButton
    }

    // create a point to focus the map on in Quebec province
    private val center: Point = Point(
        -7702852.905619, 6217972.345771, SpatialReference(3857)
    )

    // the selected line color of the grid
    private var lineColor: Color = Color.white

    // the selected label color of the grid
    private var labelColor: Color = Color.white

    // the selected label position of the grid
    private var labelPosition: GridLabelPosition = GridLabelPosition.AllSides

    // boolean set if the layer is visible
    private var isLabelVisible = true

    // create a popup dialog to manage grid settings
    private val popUpDialogBinding by lazy {
        PopupDialogBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(mapView)

        mapView.apply {
            // create a map with imagery basemap
            map = ArcGISMap(BasemapStyle.ArcGISImagery)
            // set the initial viewpoint of the map
            setViewpoint(Viewpoint(center, 23227.0))
            // set defaults on grid
            grid = LatitudeLongitudeGrid()
        }

        val dialog = MaterialAlertDialogBuilder(this@MainActivity).apply {
            setView(popUpDialogBinding.root)
            setTitle(getString(R.string.change_grid_button))
        }.create()

        // set up options in popup menu
        // create drop-down list of different layer types
        setupLayerDropdown(popUpDialogBinding)

        // create drop-down list of different line colors
        setupLineColorDropdown(popUpDialogBinding)

        // create drop-down list of different label colors
        setupLabelColorDropdown(popUpDialogBinding)

        // create drop-down list of different label positions
        setupLabelPositionDropdown(popUpDialogBinding)

        // setup the checkbox to change the visibility of the labels
        setupLabelsCheckbox(popUpDialogBinding)

        // display pop-up box when button is clicked
        menuButton.setOnClickListener {
            dialog.show()
        }
    }

    /**
     * Sets up the [popupDialogBinding] for selecting a grid type
     * and handles behavior for when a new grid type is selected.
     */
    private fun setupLayerDropdown(popupDialogBinding: PopupDialogBinding) {
        popupDialogBinding.gridTypeDropdown.apply {
            // set the grid type adapter
            setAdapter(ArrayAdapter(
                applicationContext,
                com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                resources.getStringArray(R.array.layers_array))
            )

            // set the grid type click listener
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        // LatitudeLongitudeGrid can have a label format of DecimalDegrees or DegreesMinutesSeconds
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
                    else -> return@OnItemClickListener showError("Unsupported option")
                }

                // make sure settings persist on grid type change
                setLabelVisibility(isLabelVisible)
                changeGridColor(lineColor)
                changeLabelColor(labelColor)
            }
        }
    }

    /**
     * Sets up the [popupDialogBinding] for selecting a grid color and
     * handles behavior for when a new line color is selected.
     */
    private fun setupLineColorDropdown(popupDialogBinding: PopupDialogBinding) {
        popupDialogBinding.gridColorDropdown.apply {
            // set the grid color adapter
            setAdapter(ArrayAdapter(
                applicationContext,
                com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                resources.getStringArray(R.array.colors_array))
            )

            // set the grid color click listener
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                lineColor = when (position) {
                    0 -> Color.red
                    1 -> Color.white
                    2 -> Color.blue
                    else -> return@OnItemClickListener showError("Unsupported option")
                }
                changeGridColor(lineColor)
            }
        }
    }

    /**
     * Sets up the [popupDialogBinding] for selecting a label color
     * and handles behavior for when a new label color is selected.
     */
    private fun setupLabelColorDropdown(popupDialogBinding: PopupDialogBinding) {
        popupDialogBinding.labelColorDropdown.apply {
            setAdapter(ArrayAdapter(
                applicationContext,
                com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                resources.getStringArray(R.array.colors_array))
            )
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                // change grid labels color
                labelColor = when (position) {
                    0 -> Color.red
                    1 -> Color.white
                    2 -> Color.blue
                    else -> return@OnItemClickListener showError("Unsupported option")
                }
                changeLabelColor(labelColor)
            }
        }
    }

    /**
     * Sets up the [popupDialogBinding] for selecting a label position relative to the grid
     * and handles behavior for when a label position is selected.
     */
    private fun setupLabelPositionDropdown(popupDialogBinding: PopupDialogBinding) {
        popupDialogBinding.labelPositionDropdown.apply {
            setAdapter(ArrayAdapter(
                applicationContext,
                com.esri.arcgismaps.sample.sampleslib.R.layout.custom_dropdown_item,
                resources.getStringArray(R.array.positions_array))
            )

            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                // set the label position
                labelPosition = when (position) {
                    0 -> GridLabelPosition.AllSides
                    1 -> GridLabelPosition.BottomLeft
                    2 -> GridLabelPosition.BottomRight
                    3 -> GridLabelPosition.Center
                    4 -> GridLabelPosition.Geographic
                    5 -> GridLabelPosition.TopLeft
                    6 -> GridLabelPosition.TopRight
                    else -> return@OnItemClickListener showError("Unsupported option")
                }
                changeLabelPosition(labelPosition)
            }
        }
    }

    /**
     * Sets up the [popupDialogBinding] for the checkbox making labels visible or invisible.
     */
    private fun setupLabelsCheckbox(popupDialogBinding: PopupDialogBinding) {
        popupDialogBinding.labelsCheckBox.apply {
            isChecked = true
            // hide and show label visibility when the checkbox is clicked
            setOnClickListener {
                isLabelVisible = isChecked
                setLabelVisibility(isLabelVisible)
            }
        }
    }

    /**
     * Sets the labels visibility based on [isVisible].
     */
    private fun setLabelVisibility(isVisible: Boolean) {
        val grid = mapView.grid ?: return
        grid.labelVisibility = isVisible
    }

    /**
     * Sets the [color] of the grid lines.
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
     * Sets the [labelColor] of the labels on the grid.
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
                haloColor = Color.black
                haloWidth =  5f
            }
            grid.setTextSymbol(gridLevel, textSymbol)
        }
    }

    /**
     * Sets the [labelPosition] of the labels on the grid.
     */
    private fun changeLabelPosition(labelPosition: GridLabelPosition) {
        val grid = mapView.grid ?: return
        grid.labelPosition = labelPosition
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}

private val Color.Companion.blue: Color
    get() {
        return fromRgba(0, 0, 255, 255)
    }
