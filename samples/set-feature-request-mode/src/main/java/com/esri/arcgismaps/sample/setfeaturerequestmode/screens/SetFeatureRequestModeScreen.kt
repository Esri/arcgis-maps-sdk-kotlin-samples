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

package com.esri.arcgismaps.sample.setfeaturerequestmode.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.data.FeatureRequestMode
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.setfeaturerequestmode.components.SetFeatureRequestModeViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun SetFeatureRequestModeScreen(sampleName: String) {

    val mapViewModel: SetFeatureRequestModeViewModel = viewModel()

    val featureRequestModes = listOf(
        FeatureRequestMode.OnInteractionCache, FeatureRequestMode.OnInteractionNoCache, FeatureRequestMode.ManualCache
    )

    Scaffold(topBar = { SampleTopAppBar(title = sampleName) }, content = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                arcGISMap = mapViewModel.arcGISMap,
                onViewpointChangedForBoundingGeometry = { boundingGeometry ->
                    mapViewModel.onViewpointChange(boundingGeometry)
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DropDownMenuBox(
                    textFieldLabel = "Feature request mode:",
                    textFieldValue = mapViewModel.currentFeatureRequestMode.javaClass.simpleName,
                    dropDownItemList = featureRequestModes.map { it.javaClass.simpleName },
                    onIndexSelected = { index -> mapViewModel.onCurrentFeatureRequestModeChanged(featureRequestModes[index]) })
                Button(
                    onClick = { mapViewModel.fetchCacheManually() },
                    // Only enable the button when the feature request mode is set to manual cache
                    enabled = mapViewModel.currentFeatureRequestMode == FeatureRequestMode.ManualCache
                ) {
                    if (mapViewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.requiredSize(24.dp),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    } else {
                        Text(
                            modifier = Modifier,
                            text = "Populate",
                        )
                    }
                }
            }
        }

        mapViewModel.messageDialogVM.apply {
            if (dialogStatus) {
                MessageDialog(
                    title = messageTitle, description = messageDescription, onDismissRequest = ::dismissDialog
                )
            }
        }
    })
}
