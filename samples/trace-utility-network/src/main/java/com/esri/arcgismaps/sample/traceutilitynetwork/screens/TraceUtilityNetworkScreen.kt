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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.esri.arcgismaps.sample.traceutilitynetwork.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.Surface
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.traceutilitynetwork.components.TraceUtilityNetworkViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun TraceUtilityNetworkScreen(sampleName: String) {

    val coroutineScope = rememberCoroutineScope()
    val mapViewModel: TraceUtilityNetworkViewModel = viewModel()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Column(
                modifier = Modifier.padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Status: ",
                    fontWeight = FontWeight.Bold
                )
                MapView(
                    modifier = Modifier
                        .padding()
                        .weight(1f)
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    onSingleTapConfirmed = { tapEvent ->

                    }
                )
                TraceOptions()
            }
        })
}

@Composable
fun TraceOptions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = "fps",
                label = { Text("Select Trace Option") },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )


            val traceOptions = listOf("Downstream", "Upstream", "Subnetwork", "Connected")
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                traceOptions.forEachIndexed { index, traceOptionName ->
                    DropdownMenuItem(
                        text = { Text(traceOptionName) },
                        onClick = {
                            expanded = false
                        })
                    // Show a divider between dropdown menu options
                    if (index < traceOptions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            var selectedIndex by remember { mutableIntStateOf(0) }
            val options = listOf(
                "Add starting location(s)",
                "Add barrier(s)"
            )
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex
                ) { Text(label) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(onClick = {}) { Text("Reset") }
            Button(onClick = {}) { Text("Trace") }
        }

        Text("Fraction along edge: 0.17")
    }
}


@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewSampleViewerTopAppBar() {
    SampleAppTheme {
        Surface {
            TraceOptions()
        }
    }
}
