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

package com.esri.arcgismaps.sample.downloadpreplannedmaparea.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.downloadpreplannedmaparea.R
import com.esri.arcgismaps.sample.downloadpreplannedmaparea.components.DownloadPreplannedMapAreaViewModel
import com.esri.arcgismaps.sample.downloadpreplannedmaparea.components.PreplannedMapAreaInfo
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.flow.StateFlow

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPreplannedMapAreaScreen(sampleName: String) {
    val mapViewModel: DownloadPreplannedMapAreaViewModel = viewModel()

    // Set up the bottom sheet controls
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(snackbarHost = { SnackbarHost(hostState = mapViewModel.snackbarHostState) },
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.currentMap,
                )
                // Show bottom sheet with override parameter options
                if (showBottomSheet) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentSize(), onDismissRequest = {
                            showBottomSheet = false
                        }, sheetState = sheetState
                    ) {
                        DownloadPreplannedMap(
                            mapViewModel::showOnlineMap,
                            mapViewModel::downloadOrShowOfflineMap,
                            mapViewModel.preplannedMapAreaInfoFlow
                        )
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
        },
        floatingActionButton = {
            if (!showBottomSheet) {
                FloatingActionButton(modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { showBottomSheet = true }) {
                    Icon(
                        Icons.Filled.Settings, contentDescription = "Select map"
                    )
                }
            }
        })
}

@Composable
fun DownloadPreplannedMap(
    showOnlineMap: () -> Unit,
    downloadOrShowMap: (PreplannedMapAreaInfo) -> Unit,
    preplannedMapAreaInfoFlow: StateFlow<List<PreplannedMapAreaInfo>>
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            text = "Select map",
            style = MaterialTheme.typography.titleLarge
        )
        Card(modifier = Modifier.wrapContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { showOnlineMap() },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = CenterVertically,
            ) {
                Text("Web map (online)")
            }
        }
        Text(text = "Preplanned map areas", modifier = Modifier.padding(top = 16.dp, start = 8.dp, bottom = 4.dp))
        Card(modifier = Modifier.wrapContentSize()) {
            preplannedMapAreaInfoFlow.collectAsState().value.forEach {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { downloadOrShowMap(it) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = CenterVertically,
                ) {
                    if (!it.isDownloaded) {
                        if (it.progress <= 0) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_download_for_offline_24),
                                contentDescription = "Download"
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.requiredSize(18.dp),
                                progress = { it.progress })
                        }
                    }
                    Text(
                        text = it.name, color = if (it.isDownloaded) {
                            Color.Black
                        } else {
                            Color.Gray
                        }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
