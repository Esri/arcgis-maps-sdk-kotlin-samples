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

package com.esri.arcgismaps.sample.addmapimagelayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.addmapimagelayer.screens.AddMapImageLayerScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        enableEdgeToEdge()
        setContent {
            SampleAppTheme {
                AddMapImageLayerApp()
            }
        }
    }

    @Composable
    private fun AddMapImageLayerApp() {
        Surface(color = MaterialTheme.colorScheme.background) {
            AddMapImageLayerScreen(
                sampleName = getString(R.string.add_map_image_layer_app_name)
            )
        }
    }
}
