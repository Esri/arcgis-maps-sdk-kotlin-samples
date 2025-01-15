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

package com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.components.AugmentRealityToShowTabletopSceneViewModel
import com.esri.arcgismaps.sample.augmentrealitytoshowtabletopscene.screens.DisplaySceneInTabletopARScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.google.ar.core.ArCoreApk

class MainActivity : ComponentActivity() {

    private var userRequestedInstall = true

    private var isGooglePlayServicesArInstalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

        setContent {
            SampleAppTheme {
                DisplaySceneInTabletopARApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkGooglePlayServicesArInstalled()
    }

    /**
     *  Check if Google Play Services for AR is installed on the device. If it's not installed, this method should get
     *  called twice: once to request the installation and once to ensure it was installed when the activity resumes.
     */
    private fun checkGooglePlayServicesArInstalled() {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    userRequestedInstall = false
                    return
                }

                ArCoreApk.InstallStatus.INSTALLED -> {
                    isGooglePlayServicesArInstalled = true
                    return
                }
            }
        } catch (e: Exception) {
            val sceneViewModel: AugmentRealityToShowTabletopSceneViewModel by viewModels()
            sceneViewModel.messageDialogVM.showMessageDialog(
                "Error checking Google Play Services for AR",
                e.message.toString()
            )
        }
    }

    @Composable
    private fun DisplaySceneInTabletopARApp() {
        Surface(color = MaterialTheme.colorScheme.background) {
            DisplaySceneInTabletopARScreen(
                sampleName = getString(R.string.augment_reality_to_show_tabletop_scene_app_name)
            )
        }
    }
}
