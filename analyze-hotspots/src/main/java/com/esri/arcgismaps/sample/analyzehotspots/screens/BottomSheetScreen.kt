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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.launch

/**
 * Bottom sheet layout to select a date range
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetScreen(
    onBottomSheetDismiss: () -> Unit,
    onRunAnalysisClicked: (Long?, Long?) -> Unit,
    bottomSheetState: SheetState,
) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val scope = rememberCoroutineScope()

    // From: Dec 31st, 1997
    val fromDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = 883526400000,
        initialDisplayMode = DisplayMode.Picker
    )
    // From: Jan 14th, 1998
    val toDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = 884736000000,
        initialDisplayMode = DisplayMode.Picker
    )
    Column {
        Button(
            modifier = Modifier.wrapContentWidth().padding(12.dp).align(CenterHorizontally),
            onClick = {
                // get the selected date range in millis
                onRunAnalysisClicked(
                    fromDatePickerState.selectedDateMillis,
                    toDatePickerState.selectedDateMillis
                )
                // hide the bottom sheet
                scope.launch {
                    bottomSheetState.hide()
                }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) {
                        onBottomSheetDismiss()
                    }
                }
            }
        ) {
            Text("Run analysis")
        }
        LazyColumn {
            // from date picker
            item {
                DatePicker(
                    state = fromDatePickerState, modifier = Modifier.padding(16.dp),
                    title = { Text(text = "FROM") }
                )
            }
            // to date picker
            item {
                DatePicker(
                    state = toDatePickerState,
                    modifier = Modifier.padding(16.dp),
                    title = { Text(text = "TO") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewBottomSheetScreen() {
    SampleAppTheme {
        BottomSheetScreen(
            onBottomSheetDismiss = {},
            onRunAnalysisClicked = { _, _ -> },
            bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }
}
