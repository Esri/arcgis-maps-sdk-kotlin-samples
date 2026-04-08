/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.showlineofsightanalysisinmap.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.showlineofsightanalysisinmap.components.ShowLineOfSightAnalysisInMapViewModel

@Composable
fun ShowLineOfSightAnalysisInMapScreen(sampleName: String) {
    val viewModel: ShowLineOfSightAnalysisInMapViewModel = viewModel()

    // Collect UI states
    val showVisibleOnly by viewModel.showVisibleTargetsOnly.collectAsStateWithLifecycle()
    val observerSummaries by viewModel.observerSummaries.collectAsStateWithLifecycle()

    // Initialize analysis when first composed
    LaunchedEffect(Unit) { viewModel.initializeAnalysis() }

    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        arcGISMap = viewModel.arcGISMap,
                        graphicsOverlays = listOf(
                            viewModel.resultsGraphicsOverlay,
                            viewModel.losPositionsGraphicsOverlay
                        ),
                        mapViewProxy = viewModel.mapViewProxy,
                        onDown = { isBottomSheetVisible = false }
                    )
                }

                // Bottom-right raster attribution label
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = "Raster data Copyright Scottish Government and SEPA (2014)",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic)
                    )
                }

                BottomSheet(
                    isVisible = isBottomSheetVisible,
                    sheetTitle = "Line of sight options",
                    onDismissRequest = { isBottomSheetVisible = false }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showVisibleOnly,
                            onCheckedChange = { viewModel.updateShowVisibleTargetsOnly(it) }
                        )
                        Text(
                            text = "Show results where the target (circle) is visible from the observer (triangle)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Text(
                        text = "Observer results",
                        style = MaterialTheme.typography.titleMedium
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(observerSummaries) { _, summary ->
                            Text(text = summary)
                        }
                    }
                }

                viewModel.messageDialogVM.apply {
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
