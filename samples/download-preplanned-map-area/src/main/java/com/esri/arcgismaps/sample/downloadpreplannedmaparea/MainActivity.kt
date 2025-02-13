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

package com.esri.arcgismaps.sample.downloadpreplannedmaparea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.downloadpreplannedmaparea.screens.DownloadPreplannedMapAreaScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val offlineMapDirectory by lazy {
        File(application.externalCacheDir?.path + application.getString(R.string.download_preplanned_map_area_app_name))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        // Delete any existing offline maps, to reset sample state
        offlineMapDirectory.deleteRecursively()

        setContent {
            SampleAppTheme {
                DownloadPreplannedMapAreaApp()
            }
        }
    }

    @Composable
    private fun DownloadPreplannedMapAreaApp() {
        Surface(color = MaterialTheme.colorScheme.background) {
            DownloadPreplannedMapAreaScreen(
                sampleName = getString(R.string.download_preplanned_map_area_app_name)
            )
        }
    }
}
