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

package com.esri.arcgismaps.sample.createandsavemap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.createandsavemap.screens.MainScreen
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.arcgismaps.toolkit.authentication.signOut
import com.esri.arcgismaps.sample.createandsavemap.components.MapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This sample uses an ArcGIS Online login to be able to save a map as an ArcGIS portal item
        // No need for license strings or an API key
        ArcGISEnvironment.apiKey = null

        // Sign out of any portals which are already authenticated
        lifecycleScope.launch(Dispatchers.Main) {
            ArcGISEnvironment.authenticationManager.signOut()

            setContent {
                SampleAppTheme {
                    CreateAndSaveMapApp()
                }
            }
        }
    }

    @Composable
    private fun CreateAndSaveMapApp() {
        val mapViewModel: MapViewModel = viewModel()
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                sampleName = getString(R.string.app_name)
            )

            // authenticator at bottom can draw over the top of the sample
            DialogAuthenticator(authenticatorState = mapViewModel.authenticatorState)
        }
    }
}
