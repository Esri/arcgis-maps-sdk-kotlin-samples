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

package com.esri.arcgismaps.sample.browsebuildingfloors

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.floor.FloorLevel
import com.arcgismaps.mapping.floor.FloorManager
import com.arcgismaps.portal.Portal
import com.arcgismaps.portal.PortalItem
import com.esri.arcgismaps.sample.browsebuildingfloors.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val currentFloorTV by lazy {
        activityMainBinding.selectedFloorTV
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // load the portal and create a map from the portal item
        val portalItem = PortalItem(
            Portal("https://www.arcgis.com/"),
            "f133a698536f44c8884ad81f80b6cfc7"
        )

        // set the map to be displayed in the layout's MapView
        val map = ArcGISMap(portalItem)
        mapView.map = map

        lifecycleScope.launch {
            //load the portal item on the map
            map.load().getOrElse {
                showError("Error loading portal item" + it.message.toString())
                return@launch
            }

            // load the web map's floor manager
            val floorManager =
                map.floorManager ?: return@launch showError("Map is not a floor aware web-map")
            floorManager.load().getOrElse {
                showError("Error loading floor manager" + it.message.toString())
                return@launch
            }

            // set up dropdown and initial floor level to ground floor
            initializeFloorDropdown(floorManager)
        }
    }

    /**
     * Set and update the floor dropdown. Shows the currently selected floor
     * and hides the other floors using [floorManager].
     */
    private fun initializeFloorDropdown(floorManager: FloorManager) {
        // enable the dropdown view
        activityMainBinding.dropdownMenu.isEnabled = true

        currentFloorTV.apply {
            // set the dropdown adapter for the floor selection
            setAdapter(
                FloorsAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    floorManager.levels
                )
            )

            // handle on dropdown item selected
            onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    // set all the floors to invisible to reset the floorManager
                    floorManager.levels.forEach { floorLevel ->
                        floorLevel.isVisible = false
                    }
                    // set the currently selected floor to be visible
                    floorManager.levels[position].isVisible = true

                    val floor = floorManager.levels[position].longName
                    Log.e("Floor",floor)

                    // set the floor name
                    currentFloorTV.setText(floorManager.levels[position].longName)
                }

            // Select the ground floor using `verticalOrder`.
            // The floor at index 0 might not have a vertical order of 0 if,
            // for example, the building starts with basements.
            // To select the ground floor, we can search for a level with a
            // `verticalOrder` of 0. You can also use level ID, number or name
            // to locate a floor.
            setSelection(floorManager.levels.indexOf(
                floorManager.levels.first { it.verticalOrder == 0 }
            ))
        }
    }

    /**
     * Adapter to display a list [floorLevels]
     */
    private class FloorsAdapter(
        context: Context,
        @LayoutRes private val layoutResourceId: Int,
        private val floorLevels: List<FloorLevel>
    ) : ArrayAdapter<FloorLevel>(context, layoutResourceId, floorLevels) {

        private val mLayoutInflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getCount(): Int {
            return floorLevels.size
        }

        override fun getItem(position: Int): FloorLevel {
            return floorLevels[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // bind the view to the layout inflater
            val view = convertView ?: mLayoutInflater.inflate(layoutResourceId, parent, false)
            val dropdownItemTV = view.findViewById<TextView>(android.R.id.text1)
            // bind the long name of the floor to it's respective text view
            dropdownItemTV.text = floorLevels[position].longName
            return view
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
