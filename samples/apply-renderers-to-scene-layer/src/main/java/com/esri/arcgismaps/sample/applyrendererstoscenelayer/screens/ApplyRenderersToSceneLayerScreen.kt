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

package com.esri.arcgismaps.sample.applyrendererstoscenelayer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.esri.arcgismaps.sample.applyrendererstoscenelayer.components.ApplyRenderersToSceneLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.applyrendererstoscenelayer.components.ApplyRenderersToSceneLayerViewModel.RendererType

/**
 * Main screen for the ApplyRenderersToSceneLayer sample.
 */
@Composable
fun ApplyRenderersToSceneLayerScreen(sampleName: String) {
    val viewModel: ApplyRenderersToSceneLayerViewModel = viewModel()
    val selectedRendererType by viewModel.selectedRendererType.collectAsStateWithLifecycle()
    val rendererTypes = viewModel.rendererTypes

    var isRendererTypeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 3D SceneView
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = viewModel.arcGISScene
                )
                // Floating controls at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Renderer selection dropdown
                    RendererDropdown(
                        modifier = Modifier.wrapContentWidth(),
                        rendererTypes = rendererTypes,
                        selectedRenderer = selectedRendererType,
                        isExpanded = isRendererTypeMenuExpanded,
                        onExpandedChange = { isRendererTypeMenuExpanded = it },
                        onRendererSelected = {
                            isRendererTypeMenuExpanded = false
                            viewModel.updateSceneLayerRenderer(it)
                        }
                    )
                }
            }
            // Error dialog
            viewModel.messageDialogVM.apply {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RendererDropdown(
    modifier: Modifier = Modifier,
    rendererTypes: List<RendererType>,
    selectedRenderer: RendererType,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRendererSelected: (RendererType) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = onExpandedChange
    ) {
        TextField(
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = selectedRenderer.label,
            onValueChange = {},
            label = { Text("Select Renderer") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            rendererTypes.forEach { rendererType ->
                DropdownMenuItem(
                    text = { Text(rendererType.label) },
                    onClick = { onRendererSelected(rendererType) }
                )
            }
        }
    }
}
