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

package com.esri.arcgismaps.sample.showmobilemappackageexpirationdate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showmobilemappackageexpirationdate.components.ShowMobileMapPackageExpirationDateViewModel

/**
 * Main screen layout for the sample app which displays the map from a mobile map package
 * and shows expiration information if the mobile map package has expired.
 */
@Composable
fun ShowMobileMapPackageExpirationDateScreen(sampleName: String) {
    val mapViewModel: ShowMobileMapPackageExpirationDateViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap
                )

                // Display expiration information overlay if the mobile map package is expired
                if (mapViewModel.isExpired) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 16.dp, bottom = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mapViewModel.expirationMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text(
                                text = "Expiration date: ${mapViewModel.expirationDateText ?: "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Display error dialog when the sample encounters an error
                mapViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        }
    )
}
