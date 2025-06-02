/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.showdevicelocation.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.LocationDisplay
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class ShowDeviceLocationViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // available options in dropdown menu when the location tracking is on
    private val dropDownMenuOptionsWhenOn = arrayListOf(
        "Off",
        "On",
        "Re-center",
        "Navigation",
        "Compass",
    )

    // available options in dropdown menu when the location tracking is off
    private val dropDownMenuOptionsWhenOff = arrayListOf(
        "Off",
        "On",
    )

    // This variable holds the currently selected item from the dropdown menu
    val selectedItem = mutableStateOf(dropDownMenuOptionsWhenOn[1])

    // This property returns the appropriate dropdown menu options based on the selected item
    val dropDownMenuOptions
        get() = if (selectedItem.value == "Off")
            dropDownMenuOptionsWhenOff else dropDownMenuOptionsWhenOn

    // This function handles the selection of an item from the dropdown menu
    fun onItemSelected(itemText: String, locationDisplay: LocationDisplay){
        selectedItem.value = itemText
        when (itemText) {
            "Off" ->  // stop location display
                viewModelScope.launch {
                    locationDisplay.dataSource.stop()
                }
            "On" -> { // start location display
                viewModelScope.launch {
                    locationDisplay.dataSource.start()
                }
                // display location without any changes to MapView
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off)
            }
            "Re-center" -> {
                // re-center MapView on location
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            }
            "Navigation" -> {
                // start navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Navigation)
            }
            "Compass" -> {
                // start compass navigation mode
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
            }
        }
    }
}
