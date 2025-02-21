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

package com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.components

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.LocationDisplay
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch


class DisplayDeviceLocationWithFusedLocationDataSourceViewModel(application: Application) :
    AndroidViewModel(application) {

    // Create an ArcGIS map
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigation)

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Create a fused location provider to get location updates from the fused location API
    private val fusedLocationProvider = FusedLocationOrientationProvider(getApplication())

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map", error.message.toString()
                )
            }
        }
    }

    /**
     * Pass changes in priority to the fused location provider.
     */
    fun onPriorityChanged(priority: Int) {
        fusedLocationProvider.onPriorityChanged(priority)
    }

    /**
     * Pass changes in interval to the fused location provider.
     */
    fun onIntervalChanged(interval: Long) {
        fusedLocationProvider.onIntervalChanged(interval)
    }

    /**
     * Initialize the location display with a custom location data source using the fused location provider.
     */
    fun initialize(locationDisplay: LocationDisplay) {

        // Set the location display to be used by this view model
        locationDisplay.apply {
            // Set the location display's data source to a Custom Location DataSource which implements a location
            // provider interface on the Fused Location API
            dataSource = CustomLocationDataSource { fusedLocationProvider }.also {
                viewModelScope.launch {
                    it.locationChanged.collect { location ->
                        Log.d("Location", "Location: ${location.position.x}, ${location.position.y}")
                    }
                }
                viewModelScope.launch {
                    it.headingChanged.collect { heading ->
                        Log.d("Heading", "Heading: $heading")
                    }
                }
            }
            // Keep track of the job so it can be canceled elsewhere
            viewModelScope.launch {
                // Start the data source
                dataSource.start()
                // Start emitting fused locations into the data source
                fusedLocationProvider.start()
            }
            // Set the AutoPan mode to recenter around the location display
            setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)
        }
    }
}
