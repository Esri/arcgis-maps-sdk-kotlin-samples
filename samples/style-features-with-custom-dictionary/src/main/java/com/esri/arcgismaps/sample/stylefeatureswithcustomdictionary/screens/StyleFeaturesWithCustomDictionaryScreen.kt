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

package com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary.screens

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary.components.CustomDictionaryStyle
import com.esri.arcgismaps.sample.stylefeatureswithcustomdictionary.components.StyleFeaturesWithCustomDictionaryViewModel

/**
 * Main screen showcasing the dictionary renderer toggle between style file and web style.
 */
@Composable
fun StyleFeaturesWithCustomDictionaryScreen(sampleName: String) {
    val mapViewModel: StyleFeaturesWithCustomDictionaryViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap
                )

                DictionaryStyleToggle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onStyleSelected = mapViewModel::updateSelectedStyle
                )
            }

            // Displays a dialog when sample encounters an error
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

/**
 * A segmented control to switch between Style File and Web Style dictionary renderers.
 */
@Composable
private fun DictionaryStyleToggle(
    modifier: Modifier = Modifier,
    onStyleSelected: (CustomDictionaryStyle) -> Unit
) {
    var currentSelectionIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Dictionary Symbol Style", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf("Style File", "Web Style")
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = {
                        currentSelectionIndex = index
                        val newStyle =
                            if (index == 0) CustomDictionaryStyle.StyleFile else CustomDictionaryStyle.WebStyle
                        onStyleSelected(newStyle)
                    },
                    selected = (index == currentSelectionIndex)
                ) {
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = label,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
