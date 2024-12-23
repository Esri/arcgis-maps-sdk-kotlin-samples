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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.animateimageswithimageoverlay.components.AnimateImagesWithImageOverlayViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlin.math.roundToInt
import kotlin.reflect.KFunction1


/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimateImagesWithImageOverlayScreen(sampleName: String) {
    val sceneViewModel: AnimateImagesWithImageOverlayViewModel = viewModel()
    // Set up the bottom sheet controls
    val controlsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isBottomSheetVisible) {
        when {
            isBottomSheetVisible -> controlsBottomSheetState.show()
            else -> controlsBottomSheetState.hide()
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show ImageOverlayMenu") }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                SceneView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISScene = sceneViewModel.arcGISScene,
                    imageOverlays = listOf(sceneViewModel.imageOverlay)
                )
                if (isBottomSheetVisible) {
                    ModalBottomSheet(
                        modifier = Modifier.wrapContentHeight(),
                        sheetState = controlsBottomSheetState,
                        onDismissRequest = { isBottomSheetVisible = false }
                    ) {
                        ImageOverlayMenu(
                            isStarted = sceneViewModel.isStarted,
                            fps = sceneViewModel.fps,
                            opacity = sceneViewModel.opacity,
                            fpsOptions = sceneViewModel.fpsOptions,
                            updateFpsOption = sceneViewModel::updateFpsOption,
                            updateOpacity = sceneViewModel::updateOpacity,
                            updateIsStarted = sceneViewModel::updateIsStarted
                        )
                    }
                }
                sceneViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageOverlayMenu(
    isStarted: Boolean,
    fps: Int,
    opacity: Float,
    fpsOptions: List<Int>,
    updateFpsOption: KFunction1<Int, Unit>,
    updateOpacity: KFunction1<Float, Unit>,
    updateIsStarted: KFunction1<Boolean, Unit>
) {

    var selectedFps by remember { mutableIntStateOf(60) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.Absolute.SpaceBetween
    ) {
        Text("Opacity:")
        Text(text = (opacity * 100).roundToInt().toString() + "%")
    }
    // Opacity Slider
    Slider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        value = opacity,
        onValueChange = { updateOpacity(it) },
        valueRange = 0f..1.0f
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Start/Stop Button
        Button(onClick = {
            updateIsStarted(!isStarted)
        }) {
            Text(if (isStarted) "Stop" else "Start")
        }
        // FPS Dropdown Menu
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            modifier = Modifier.width(150.dp),
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = "${fps} fps",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fpsOptions.forEachIndexed { index, fps ->
                    DropdownMenuItem(
                        text = { Text("$fps fps") },
                        onClick = {
                            updateFpsOption(index)
                            selectedFps = fps
                            expanded = false
                        })
                    // show a divider between dropdown menu options
                    if (index < fpsOptions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
