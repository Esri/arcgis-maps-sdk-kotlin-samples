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

@file:OptIn(ExperimentalMaterial3Api::class)
package com.esri.arcgismaps.sample.shubhamtemplatemobilemappackage

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme{
                val application = LocalContext.current.applicationContext as Application
                val coroutineScope = rememberCoroutineScope()

                val mapViewModel = remember { MapViewModel(application, coroutineScope) }

                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.map
                )
            }
        }
    }
}

class MapViewModel(
    private val application: Application,
    private val coroutineScope: CoroutineScope
): AndroidViewModel(application) {
    var map by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISNavigationNight))

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(R.string.app_name)
    }

    // Get the file path of the (.mmpk) file.
    val filePath = provisionPath + File.separator + application.getString(R.string.yellowstone_mmpk)

    init{
        addMobileMapPackage()
    }

    fun addMobileMapPackage() {
        // Create the mobile map package.
        val mapPackage = MobileMapPackage(filePath)
        coroutineScope.launch {
            // @@Start(Main)
            // Load the mobile map package.
            mapPackage.load().getOrElse {
                // Mobile map package failed to load.
                showError("Map package failed to load: ${it.message.toString()}")
            }
            // Assign the loaded map from the mobile map package
            // to the mutable ArcGISMap used by the Composable MapView.
            map = mapPackage.maps.first()
            // @@End(Main)
        }
    }
}

private fun showError(message: String) {
    Log.e("TAG", message)
}

// ****************************************************************************************************
// the following code is Shubham's template.

/*
class MainActivity : ComponentActivity() {

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    private var map by mutableStateOf(ArcGISMap(BasemapStyle.ArcGISNavigationNight))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {
            MaterialTheme {
                // Get the file path of the (.mmpk) file
                val filePath = provisionPath + File.separator + getString(R.string.yellowstone_mmpk)

                // Load and add the mobile map package
                addMobileMapPackage(filePath, rememberCoroutineScope())

                // Compose equivalent of the view-based MapView.
                MapView(
                    arcGISMap = map,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun addMobileMapPackage(filePath: String, coroutineScope: CoroutineScope) {
        // Create the mobile map package
        val mapPackage = MobileMapPackage(filePath)
        coroutineScope.launch {
            // Load the mobile map package.
            mapPackage.load().getOrElse {
                // Mobile map package failed to load.
                showError("Map package failed to load: ${it.message.toString()}")
            }
            // Add the loaded map from the mobile map package
            // to the mutable ArcGISMap used by the Composable MapView.
            map = mapPackage.maps.first()
        }
    }

    private fun showError(message: String) {
        Log.e("TAG", message)
    }

}
*/

