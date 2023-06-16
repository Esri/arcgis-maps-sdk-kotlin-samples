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

package com.esri.arcgismaps.sample.analyzehotspots.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Bottom app content with a button to toggle the bottom sheet layout.
 * Returns a lambda for the [analyzeHotspotsRange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppContent(
    analyzeHotspotsRange: (Long?, Long?) -> Unit,
) {
    // boolean to toggle the state of the bottom sheet layout
    var showAnalysisOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // button to display analyze hotspot options
        Button(onClick = { showAnalysisOptions = true }) {
            Text(text = "Analyze")
            // expands the bottom sheet if true
            if (showAnalysisOptions) {
                // modal to control the bottom sheet state
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    sheetState = bottomSheetState,
                    onDismissRequest = { showAnalysisOptions = false },
                ) {
                    // displays a date range picker using a bottom sheet
                    BottomSheetScreen(
                        bottomSheetState = bottomSheetState,
                        onBottomSheetDismiss = {
                            // hide the bottom sheet
                            showAnalysisOptions = false
                        },
                        onRunAnalysisClicked = { from, to ->
                            analyzeHotspotsRange(from, to)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewBottomAppContent() {
    SampleAppTheme {
        BottomAppContent(analyzeHotspotsRange = { _, _ -> })
    }
}
