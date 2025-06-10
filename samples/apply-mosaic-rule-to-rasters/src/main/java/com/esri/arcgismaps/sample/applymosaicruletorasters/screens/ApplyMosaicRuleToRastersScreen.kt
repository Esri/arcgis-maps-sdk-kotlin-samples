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

package com.esri.arcgismaps.sample.applymosaicruletorasters.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.applymosaicruletorasters.components.ApplyMosaicRuleToRastersViewModel
import com.esri.arcgismaps.sample.applymosaicruletorasters.components.MosaicRuleType
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SamplePreviewSurface
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

@Composable
fun ApplyMosaicRuleToRastersScreen(sampleName: String) {
    val mapViewModel: ApplyMosaicRuleToRastersViewModel = viewModel()
    val selectedRuleType by mapViewModel.selectedRuleType.collectAsStateWithLifecycle()
    val isLoading = mapViewModel.isLoading

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MapView(
                        modifier = Modifier.fillMaxSize(),
                        arcGISMap = mapViewModel.arcGISMap
                    )
                    if (isLoading) {
                        LoadingDialog(loadingMessage = "Loading...")
                    }
                }
                MosaicRuleOptionsBar(
                    ruleTypes = mapViewModel.mosaicRuleTypes,
                    selectedRuleType = selectedRuleType,
                    onRuleTypeSelected = mapViewModel::updateMosaicRule
                )
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

@Composable
fun MosaicRuleOptionsBar(
    ruleTypes: List<MosaicRuleType>,
    selectedRuleType: MosaicRuleType,
    onRuleTypeSelected: (MosaicRuleType) -> Unit
) {
    var selectedIndex by remember(selectedRuleType) {
        mutableIntStateOf(ruleTypes.indexOf(selectedRuleType))
    }
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mosaic Rule:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            DropDownMenuBox(
                textFieldValue = ruleTypes[selectedIndex].label,
                textFieldLabel = "Select a mosaic rule",
                dropDownItemList = ruleTypes.map { it.label },
                onIndexSelected = {
                    selectedIndex = it
                    onRuleTypeSelected(ruleTypes[it])
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MosaicRuleOptionsBarPreview() {
    val ruleTypes = MosaicRuleType.entries
    SamplePreviewSurface {
        MosaicRuleOptionsBar(
            ruleTypes = ruleTypes,
            selectedRuleType = ruleTypes[0],
            onRuleTypeSelected = {}
        )
    }
}
