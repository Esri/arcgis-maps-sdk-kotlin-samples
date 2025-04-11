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
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class MainActivity : ComponentActivity() {

    private val sceneViewModel: AugmentRealityToShowTabletopSceneViewModel by viewModels()

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
        checkARCoreAvailability()
    }

    /**
     * Checks if ARCore is supported and handles install/update flow if required.
     */
    private fun checkARCoreAvailability() {
        val context = this
        try {
            ArCoreApk.getInstance().checkAvailabilityAsync(context) { availability ->
                when (availability) {
                    ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                        sceneViewModel.messageDialogVM.showMessageDialog(
                            title = "AR Not Supported",
                            description = "This device does not support AR."
                        )
                    }

                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                        try {
                            val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
                            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                                // Installation requested, wait for next resume
                            }
                        } catch (e: UnavailableUserDeclinedInstallationException) {
                            sceneViewModel.messageDialogVM.showMessageDialog(
                                title = "AR Installation Declined",
                                description = "User declined to install ARCore."
                            )
                        } catch (e: UnavailableDeviceNotCompatibleException) {
                            sceneViewModel.messageDialogVM.showMessageDialog(
                                title = "AR Not Compatible",
                                description = "This device is not compatible with ARCore."
                            )
                        } catch (e: Exception) {
                            sceneViewModel.messageDialogVM.showMessageDialog(
                                title = "Installation Error",
                                description = e.localizedMessage ?: "An unknown error occurred."
                            )
                        }
                    }

                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                        // ARCore is ready, proceed to use AR features.
                    }

                    ArCoreApk.Availability.UNKNOWN_CHECKING,
                    ArCoreApk.Availability.UNKNOWN_ERROR,
                    ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                        sceneViewModel.messageDialogVM.showMessageDialog(
                            title = "AR Check Error",
                            description = "Unable to determine ARCore availability. Please try again."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            sceneViewModel.messageDialogVM.showMessageDialog(
                "Error checking AR availability",
                e.localizedMessage ?: "An error occurred."
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
