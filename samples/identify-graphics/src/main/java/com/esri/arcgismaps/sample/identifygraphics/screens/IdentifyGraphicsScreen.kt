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

package com.esri.arcgismaps.sample.identifygraphics.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.identifygraphics.components.IdentifyGraphicsViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

@Composable
fun IdentifyGraphicsScreen(sampleName: String) {
    val viewModel: IdentifyGraphicsViewModel = viewModel()

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
                        .fillMaxSize(),
                    arcGISMap = viewModel.arcGISMap,
                    mapViewProxy = viewModel.mapViewProxy,
                    graphicsOverlays = viewModel.graphicsOverlays,
                    onSingleTapConfirmed = { tapEvent ->
                        viewModel.identifyGraphics(tapEvent.screenCoordinate)
                    }
                )
            }

            // Show a dialog if the sample needs to show a message or error.
            MessageDialog(
                viewModel.messageDialogState,
                onDismiss = viewModel.messageDialogState::hide
            )
        }
    )
}


@Composable
fun MessageDialog(
    state: MessageDialogState,
    icon: ImageVector = Icons.Filled.Info,
    onDismiss: (() -> Unit)? = null,
) {
    if (!state.dialogStatus) {
        return
    }
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(state.title) },
        text = { Text(state.description) },
        confirmButton = {
            TextButton(onClick = { onDismiss?.invoke() }) {
                Text("Dismiss")
            }
        },
    )
}

class MessageDialogState() {
    var dialogStatus by mutableStateOf(false)

    var title by mutableStateOf("")

    var description by mutableStateOf("")

    fun showError(error: Throwable) {
        title = error.message ?: "Unknown error"
        description = error.cause.toString()
        dialogStatus = true
    }

    fun showMessage(title: String, description: String = "") {
        this@MessageDialogState.title = title
        this@MessageDialogState.description = description
        dialogStatus = true
    }

    fun hide() {
        dialogStatus = false
    }
}
