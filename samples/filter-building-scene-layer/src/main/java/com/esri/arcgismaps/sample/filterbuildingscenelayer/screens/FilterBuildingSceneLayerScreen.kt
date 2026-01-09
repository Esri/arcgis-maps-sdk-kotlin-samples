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

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.esri.arcgismaps.sample.filterbuildingscenelayer.components.FilterBuildingSceneLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun FilterBuildingSceneLayerScreen(sampleName: String) {
    val localSceneViewModel: FilterBuildingSceneLayerViewModel = viewModel()
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        localSceneViewModel.scene.load().onSuccess {
            localSceneViewModel.buildingSceneLayer =
                localSceneViewModel.scene.operationalLayers.first { layer ->
                    layer is BuildingSceneLayer
                } as BuildingSceneLayer

            localSceneViewModel.buildingSceneLayer?.let { buildingSceneLayer ->
                // Get the floor listing from the statistics
                buildingSceneLayer.fetchStatistics().onSuccess { statistics ->
                    statistics["BldgLevel"]?.mostFrequentValues?.forEach {
                        localSceneViewModel.floors.add(it)
                    }
                    localSceneViewModel.floors.sort()
                    localSceneViewModel.floors.forEach {
                        Log.d("LSV", "Sorted: $it")
                    }
                }

                val fullModesSublayer =
                    buildingSceneLayer.sublayers.find { sublayer ->
                        sublayer.name == "Full Model"
                    } as BuildingGroupSublayer

                val categorySublayers = fullModesSublayer.sublayers
                categorySublayers.forEach { buildingSublayer ->
                    Log.d("LSV", buildingSublayer.name)
                    localSceneViewModel.categories.add(buildingSublayer)
                }
                localSceneViewModel.categories.sortBy { buildingSublayer -> buildingSublayer.name }
            }
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                LocalSceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    scene = localSceneViewModel.scene
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Settings",
                onDismissRequest = { isBottomSheetVisible = false },
            ) {
                Column(modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloorSelector(floors = localSceneViewModel.floors)
                    HorizontalDivider()
                    CategorySelector(categories = localSceneViewModel.categories)
                }
            }

            localSceneViewModel.messageDialogVM.apply {
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
fun FloorSelector(
    floors: List<String>
) {
    val localSceneViewModel: FilterBuildingSceneLayerViewModel = viewModel()

    DropDownMenuBox(
        textFieldValue = localSceneViewModel.selectedFloor,
        textFieldLabel = "Floor",
        dropDownItemList = floors,
        onIndexSelected = localSceneViewModel::selectFloor
    )
}

@Composable
fun CategorySelector(categories: List<BuildingSublayer>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Categories:", modifier = Modifier.padding(8.dp))

        Column {
            categories.forEach { buildingSublayer ->
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
                        val imageVector = when {
                            showSubCategories -> Icons.Default.ArrowDropUp
                            else -> Icons.Default.ArrowDropDown
                        }
                        Icon(
                            imageVector = imageVector,
                            contentDescription = "Show sub-categories",
                            modifier = Modifier
                        )
                    }
                }
                if (showSubCategories) {
                    val buildingGroupSublayer = buildingSublayer as BuildingGroupSublayer
                    buildingGroupSublayer.sublayers.sortedBy { it.name }.forEach {
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
