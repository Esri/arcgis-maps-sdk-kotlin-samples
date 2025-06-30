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

package com.esri.arcgismaps.sample.takescreenshot.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.takescreenshot.components.TakeScreenshotViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun TakeScreenshotScreen(sampleName: String) {
    val mapViewModel: TakeScreenshotViewModel = viewModel()

    val context = LocalContext.current

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    mapViewProxy = mapViewModel.mapViewProxy
                )

                Button(
                    onClick = mapViewModel::takeScreenshot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RectangleShape
                ) {
                    Text(text = "Take Screenshot")
                }
            }

            // Show DialogWithImage if screenshotImage is not null
            mapViewModel.screenshotImage?.let { screenshotImage ->
                DialogWithImage(
                    onConfirmation = {
                        mapViewModel.clearScreenshotImage()
                        mapViewModel.saveBitmapToFile(context, screenshotImage.bitmap)?.let { uri ->
                            shareImage(context, uri)
                        }
                    },
                    onDismissRequest = mapViewModel::clearScreenshotImage,
                    imageBitmap = screenshotImage.bitmap.asImageBitmap(),
                    imageDescription = "Screenshot",
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


/**
 * Displays a dialog with an image and a share button.
 */
@Composable
fun DialogWithImage(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    imageBitmap: ImageBitmap,
    imageDescription: String,
) {
    SampleDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = imageDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
            )
            TextButton(onClick = onConfirmation) {
                Icon(Icons.Default.Share, "Share icon", Modifier.padding(horizontal = 4.dp))
                Text("Share", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Function to display a pop-up for sharing an image file
fun shareImage(context: Context, imageUri: Uri) {
    // Create an intent to share the image
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, imageUri)
        type = "image/png"
    }
    // Start the sharing activity with a chooser
    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
}
