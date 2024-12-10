/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.animateimageswithimageoverlay.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.view.ImageOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.animateimageswithimageoverlay.components.AnimateImagesWithImageOverlayViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar


/**
 * Main screen layout for the sample app
 */
@Composable
fun AnimateImagesWithImageOverlayScreen(sampleName: String) {
    val mapViewModel: AnimateImagesWithImageOverlayViewModel = viewModel()
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            var showControls by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap
                )



                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = { showControls = !showControls },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Menu")
                    }
                }
            }
            if (showControls) {
                ImageOverlayMenu()
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

@Composable
private fun imageOverlayMenu() {
    var expanded by remember { mutableStateOf(false) }
    var opacity by remember { mutableFloatStateOf(100f) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableStateOf("60 fps") }
    Column {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Opacity Slider
            Text("Opacity")
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 1f..100f
            )
            Text((opacity / 100).toString())

            // Start/Stop Button
            Button(onClick = {
                isRunning = !isRunning
            }) {
                Text(if (isRunning) "Stop" else "Start")
            }

            // FPS Dropdown Menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("60 fps", "30 fps", "15 fps").forEach { fps ->
                    DropdownMenuItem(
                        text = { Text(fps) },
                        onClick = { selectedFps = fps })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFloatingActionButtonMenu() {
    AnimateImagesWithImageOverlayScreen("Animate Images With Image Overlay")
}