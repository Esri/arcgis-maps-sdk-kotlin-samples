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

package com.esri.arcgismaps.sample.showdevicelocationusingfusedlocationdatasource

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.displaydevicelocationwithfusedlocationdatasource.R
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.showdevicelocationusingfusedlocationdatasource.screens.ShowDeviceLocationUsingFusedLocationDataSource

class MainActivity : ComponentActivity() {

    private var isLocationPermissionGranted = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLocationPermissionGranted = true
        } else {
            Toast.makeText(this, "Location permission is required to run this sample!", Toast.LENGTH_SHORT).show()
        }
        setContent {
            SampleAppTheme {
                ShowDeviceLocationUsingFusedLocationDataSource()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        ArcGISEnvironment.applicationContext = applicationContext

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    @Composable
    private fun ShowDeviceLocationUsingFusedLocationDataSource() {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShowDeviceLocationUsingFusedLocationDataSource(
                sampleName = getString(R.string.show_device_location_using_fused_location_data_source),
                locationPermissionGranted = isLocationPermissionGranted
            )
        }
    }
}
