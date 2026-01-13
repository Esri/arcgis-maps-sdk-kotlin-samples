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

package com.esri.arcgismaps.sample.filterbuildingscenelayer.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.esri.arcgismaps.sample.filterbuildingscenelayer.components.FilterBuildingSceneLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun FilterBuildingSceneLayerScreen(sampleName: String) {
    val viewModel: FilterBuildingSceneLayerViewModel = viewModel()
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        floatingActionButton = {
            if (!isBottomSheetVisible) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 36.dp, end = 12.dp),
                    onClick = { isBottomSheetVisible = true }
                ) { Icon(Icons.Filled.Settings, contentDescription = "Show options") }
            }
        },
        content = {
            // display a LoadingDialog to indicate the map loading status
            if (viewModel.showLoadingDialog.value) {
                LoadingDialog(loadingMessage = "Loading layer...")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                LocalSceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    scene = viewModel.scene
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Settings",
                onDismissRequest = { isBottomSheetVisible = false },
            ) {
                Column(modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloorSelector()
                    HorizontalDivider()
                    CategorySelector()
                }
            }

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

@Composable
fun FloorSelector() {
    val viewModel: FilterBuildingSceneLayerViewModel = viewModel()

    DropDownMenuBox(
        textFieldValue = viewModel.selectedFloor,
        textFieldLabel = "Floor",
        dropDownItemList = viewModel.floors,
        onIndexSelected = viewModel::selectFloor
    )
}

@Composable
fun CategorySelector() {
    val viewModel: FilterBuildingSceneLayerViewModel = viewModel()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Categories:", modifier = Modifier.padding(8.dp))

        Column {
            viewModel.categories.forEach { buildingSublayer ->
                var categoryChecked by remember { mutableStateOf(buildingSublayer.isVisible) }
                var showSubCategories by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = buildingSublayer.name, modifier = Modifier.padding(8.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Checkbox(checked = categoryChecked, onCheckedChange = {
                        categoryChecked = it
                        buildingSublayer.isVisible = categoryChecked
                    })
                    IconButton(
                        onClick = { showSubCategories = !showSubCategories }
                    ) {
                        Icon(
                            imageVector = when {
                                showSubCategories -> Icons.Default.ArrowDropUp
                                else -> Icons.Default.ArrowDropDown
                            },
                            contentDescription = "Show sub-categories",
                            modifier = Modifier
                        )
                    }
                }
                if (showSubCategories) {
                    remember {
                        val buildingGroupSublayer = buildingSublayer as BuildingGroupSublayer
                        buildingGroupSublayer.sublayers.sortedBy { it.name }
                    }.forEach {
                        var subCategoryChecked by remember { mutableStateOf(it.isVisible) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = it.name, modifier = Modifier.padding(8.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(checked = subCategoryChecked, onCheckedChange = { isChecked ->
                                subCategoryChecked = isChecked
                                it.isVisible = isChecked
                            })
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
