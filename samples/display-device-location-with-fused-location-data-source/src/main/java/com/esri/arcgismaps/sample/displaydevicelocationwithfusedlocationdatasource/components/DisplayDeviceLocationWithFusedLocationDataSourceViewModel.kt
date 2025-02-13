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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.location.CustomLocationDataSource
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.LocationDisplay
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch


class DisplayDeviceLocationWithFusedLocationDataSourceViewModel(application: Application) :
    AndroidViewModel(application) {

    val arcGISMap by mutableStateOf(
        ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
            initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
        }
    )

    var locationDisplay: LocationDisplay = LocationDisplay()

    val fusedLocationProvider = FusedLocationProvider(getApplication())

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {






        //requestPermissions()

        viewModelScope.launch {
            arcGISMap.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }
        }
    }

    /**
     * Request fine and coarse location permissions for API level 23+.
     */
    private fun requestPermissions() {
        // coarse location permission
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(getApplication(), ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        // fine location permission
        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(getApplication(), ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        // if permissions are not already granted, request permission from the user
        if (!(permissionCheckCoarseLocation && permissionCheckFineLocation)) {
            ActivityCompat.requestPermissions(
                getApplication(),
                arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                2
            )
        } else {
            // permission already granted, so start the location display
            viewModelScope.launch {

            }
        }
    }

    fun onSetLocationDisplay(locationDisplay: LocationDisplay) {
        this.locationDisplay = locationDisplay.apply {
            dataSource = CustomLocationDataSource { fusedLocationProvider }
            showLocation = true

            viewModelScope.launch {

                dataSource.start()

                fusedLocationProvider.startFusedLocationProvider()

               dataSource.locationChanged.collect { location ->

                    Log.d("location", location.position.x.toString())
                }
            }
        }
    }
}
