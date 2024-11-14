/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.geoviewnperepro

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.geoviewnperepro.components.MapViewModel
import com.esri.arcgismaps.sample.geoviewnperepro.screens.DetailsScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.geoviewnperepro.screens.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {
            SampleAppTheme {
                SampleApp()
            }
        }
    }

    @Composable
    private fun SampleApp() {
        val application = LocalContext.current.applicationContext as Application
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "main_screen",
            ) {

                val mapViewModel = MapViewModel(application)
                composable("main_screen") {
                    MainScreen(
                        mapViewModel = mapViewModel,
                        sampleName = getString(R.string.app_name),
                        onTap = {
                            navController.navigate(
                                route = "details_screen/$it"
                            )
                        }
                    )

                }
                composable("details_screen/{trip_details}") { entry ->
                    val details = entry.arguments?.getString("trip_details") ?: ""
                    DetailsScreen(
                        sampleName = getString(R.string.app_name),
                        tripDetails = details
                    )
                }
            }
        }
    }
}
