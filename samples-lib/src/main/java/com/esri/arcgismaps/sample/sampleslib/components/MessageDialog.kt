package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

/**
 * Composable component to display a dialog with a message
 */
@Composable
fun MessageDialog(
    title: String,
    description: String = "",
    onDismissRequest: () -> Unit
) {
    Log.e("SampleAlertMessage", "$title: $description")
    // display a dialog with a description text
    if (description.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text(title) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text("Dismiss")
                }
            },
        )
    } else {
        // display a dialog without a description text
        AlertDialog(
            onDismissRequest = { onDismissRequest() },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text(title) },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text("Dismiss")
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMessageDialog() {
    SampleAppTheme {
        MessageDialog(
            title = "Message title",
            description = "Dialog loading message here",
            onDismissRequest = { }
        )
    }
}
