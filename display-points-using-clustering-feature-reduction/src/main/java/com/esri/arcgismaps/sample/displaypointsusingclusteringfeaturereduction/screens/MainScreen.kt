/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components.ClusterInfoContent
import com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components.ComposeMapView
import com.esri.arcgismaps.sample.displaypointsusingclusteringfeaturereduction.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleTypography

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {

    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // composable function that wraps the MapView
                ComposeMapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    mapViewModel = mapViewModel
                )
                // Button to enable/disable featureReduction property
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Feature clustering",
                        style = SampleTypography.bodyMedium
                    )
                    Switch(
                        checked = mapViewModel.isFeatureReductionEnabled.value,
                        onCheckedChange = {
                            mapViewModel.toggleFeatureReduction()
                        })
                }

                // display a MessageDialog if the sample encounters an error
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }

                // display a LoadingDialog to indicate the map loading status
                if (mapViewModel.showLoadingDialog.value) {
                    LoadingDialog(loadingMessage = "Loading map...")
                }
            }

            // display a bottom sheet to show popup details
            BottomSheet(isVisible = mapViewModel.showClusterSummaryBottomSheet.value) {
                ClusterInfoContent(
                    popupTitle = mapViewModel.popupTitle.value,
                    clusterInfoList = mapViewModel.clusterInfoList,
                    onDismiss = { mapViewModel.dismissBottomSheet() }
                )
            }
        }
    )
}
