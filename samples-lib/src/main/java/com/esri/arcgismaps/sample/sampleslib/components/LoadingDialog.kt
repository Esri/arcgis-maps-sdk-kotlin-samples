package com.esri.arcgismaps.sample.sampleslib.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme

@Composable
fun LoadingDialog(
    title: String,
    showDialog: Boolean
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = { }
        ) {
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 30.dp)
                    )

                    // Custom Text
                    Text( 
                        modifier = Modifier
                            .padding(30.dp),
                        text = title,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoadingDialog(){
    SampleAppTheme {
        LoadingDialog(title = "Dialog loading message here", showDialog = true)
    }
}

