package com.esri.arcgismaps.sample.analyzehotspots.screens

import android.content.res.Configuration
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetScreen(
    modifier: Modifier = Modifier,
    onBottomSheetDismiss: () -> Unit,
    runAnalysisClicked: (Long?, Long?) -> Unit,
    bottomSheetState: SheetState,
) {
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
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
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
                            onBottomSheetDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewBottomSheetScreen() {
    SampleAppTheme {
        BottomSheetScreen(
            onBottomSheetDismiss = {},
            runAnalysisClicked = { _, _ -> },
            bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }
}
