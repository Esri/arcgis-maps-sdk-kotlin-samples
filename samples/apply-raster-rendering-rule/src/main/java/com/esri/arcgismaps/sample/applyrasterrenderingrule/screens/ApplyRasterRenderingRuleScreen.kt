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

package com.esri.arcgismaps.sample.applyrasterrenderingrule.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applyrasterrenderingrule.components.ApplyRasterRenderingRuleViewModel
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun ApplyRasterRenderingRuleScreen(sampleName: String) {
    val mapViewModel: ApplyRasterRenderingRuleViewModel = viewModel()

    val renderingRuleNames by mapViewModel.renderingRuleNames.collectAsState()
    val selectedRenderingRuleName by mapViewModel.selectedRenderingRuleName.collectAsState()

    // Used to trigger dropdown selection
    var selectedIndex by remember(selectedRenderingRuleName, renderingRuleNames) {
        mutableIntStateOf(renderingRuleNames.indexOf(selectedRenderingRuleName).coerceAtLeast(0))
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (renderingRuleNames.isNotEmpty()) {
                        DropDownMenuBox(
                            textFieldValue = renderingRuleNames.getOrElse(selectedIndex) { "" },
                            textFieldLabel = "Rendering Rule",
                            dropDownItemList = renderingRuleNames,
                            onIndexSelected = { index ->
                                selectedIndex = index
                                val ruleName = renderingRuleNames.getOrNull(index) ?: return@DropDownMenuBox
                                mapViewModel.setRasterLayer(ruleName)
                            }
                        )
                    }
                }
            }

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
    )
}
