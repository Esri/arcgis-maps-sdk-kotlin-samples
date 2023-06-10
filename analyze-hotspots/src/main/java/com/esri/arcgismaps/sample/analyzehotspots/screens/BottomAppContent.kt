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
import com.esri.arcgismaps.sample.analyzehotspots.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Bottom app content with a button to toggle the bottom sheet layout.
 * Returns a lambda for the  [analyzeHotspotsRange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppContent(
    analyzeHotspotsRange: (Long?, Long?) -> Unit,
) {
    // boolean to toggle the state of the bottom sheet layout
    var showAnalysisOptions by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // button to display analyze hotspot options
        Button(onClick = {
            // show the bottom sheet
            showAnalysisOptions = true
        }) {
            Text(text = "Analyze")
            if (showAnalysisOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showAnalysisOptions = false },
                    sheetState = bottomSheetState,
                ) {
                    // displays a date range picker using a bottom sheet
                    BottomSheetScreen(
                        onBottomSheetDismiss = {
                            // hide the bottom sheet
                            showAnalysisOptions = false
                        },
                        runAnalysisClicked = { from, to ->
                            analyzeHotspotsRange(from, to)
                        },
                        bottomSheetState = bottomSheetState
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
