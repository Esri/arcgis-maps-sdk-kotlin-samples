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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.R
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.screens.AugmentedRealityScreen
import com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.screens.MapScreen

@Composable
fun AugmentRealityToShowHiddenInfrastructureNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isLocationPermissionGranted: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = "route_screen",
        modifier = modifier
    ) {
        composable("route_screen") {
            MapScreen(
                sampleName = stringResource(R.string.augment_reality_to_show_hidden_infrastructure_app_name),
                locationPermissionGranted = isLocationPermissionGranted,
                onNavigateToARScreen = { navController.navigate("ar_screen") }
            )

        }
        composable("ar_screen") {
            AugmentedRealityScreen(
                sampleName = stringResource(R.string.augment_reality_to_show_hidden_infrastructure_app_name)
            )
        }
    }
}
