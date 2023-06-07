package com.esri.arcgismaps.sample.analyzehotspots.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppContent(
    runAnalysisClicked: (Long?, Long?) -> Unit = { fromDateInMillis: Long?, toDateInMillis: Long? -> },
) {
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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

    // App content
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { openBottomSheet = !openBottomSheet }) {
            Text(text = "Analyze")

            // Sheet content
            if (openBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { openBottomSheet = false },
                    sheetState = bottomSheetState,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp), horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                // Note: If you provide logic outside of onDismissRequest to remove the sheet,
                                // you must additionally handle intended state cleanup, if any.
                                onClick = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                        runAnalysisClicked(
                                            fromDatePickerState.selectedDateMillis,
                                            toDatePickerState.selectedDateMillis
                                        )
                                    }.invokeOnCompletion {
                                        if (!bottomSheetState.isVisible) {
                                            openBottomSheet = false
                                        }
                                    }
                                }
                            ) {
                                Text("Run analysis")
                            }
                        }

                        LazyColumn {
                            item {
                                DatePicker(
                                    state = fromDatePickerState,
                                    modifier = Modifier.padding(16.dp),
                                    title = {
                                        Text(text = "FROM")
                                    }
                                )
                            }
                            item {
                                DatePicker(
                                    state = toDatePickerState,
                                    modifier = Modifier.padding(16.dp),
                                    title = {
                                        Text(text = "TO")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
