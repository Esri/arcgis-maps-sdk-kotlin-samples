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

package com.esri.arcgismaps.sample.authenticatewithoauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.esri.arcgismaps.sample.authenticatewithoauth.components.MapViewModel
import com.esri.arcgismaps.sample.authenticatewithoauth.screens.MainScreen
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        setContent {
            SampleAppTheme {
                AuthenticateWithOAuthApp()
            }
        }
    }

    @Composable
    private fun AuthenticateWithOAuthApp() {

        // create a ViewModel to handle interactions
        val mapViewModel: MapViewModel = viewModel()

        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                sampleName = getString(R.string.app_name)
            )
            // Displays appropriate Authentication UI when an authentication challenge is issued.
            // Because the authenticatorState has an oAuthUserConfiguration set, authentication
            // challenges will happen via OAuth.
            DialogAuthenticator(authenticatorState = mapViewModel.authenticatorState)
        }
    }
}
