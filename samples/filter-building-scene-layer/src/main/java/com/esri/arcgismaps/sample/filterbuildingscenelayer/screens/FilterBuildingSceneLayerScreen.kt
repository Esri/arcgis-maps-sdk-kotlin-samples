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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
                        localSceneViewModel.floors.sort()
                    }
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

                val componentSublayerGroups = categorySublayers.map { categorySublayer ->
                    categorySublayer as BuildingGroupSublayer
                }
                componentSublayerGroups.forEach { buildingGroupSublayer ->
                    buildingGroupSublayer.sublayers.forEach { buildingSublayer ->
                        Log.d("LSV", "${buildingGroupSublayer.name} - ${buildingSublayer.name}")
                    }
                }
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
                    scene = localSceneViewModel.scene,
                    //onVisibleAreaChanged = { isBottomSheetVisible = false }
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Settings",
                onDismissRequest = { isBottomSheetVisible = false }
            ) {
                FloorSelector(localSceneViewModel.floors)
                HorizontalDivider()
                CategorySelector()
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val localSceneViewModel: FilterBuildingSceneLayerViewModel = viewModel()

        DropDownMenuBox(
            textFieldValue = localSceneViewModel.selectedFloor,
            textFieldLabel = "Floor",
            dropDownItemList = floors,
            onIndexSelected = localSceneViewModel::selectFloor
        )
    }
}

@Composable
fun CategorySelector() {
    val localSceneViewModel: FilterBuildingSceneLayerViewModel = viewModel()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Categories:")
        LazyColumn(
            //verticalArrangement = Arrangement.spacedBy(8.dp),
            //horizontalAlignment = Alignment.CenterHorizontally
        ) {
            localSceneViewModel.categories.forEach { buildingSublayer ->
                item {
                    var checked by remember { mutableStateOf(buildingSublayer.isVisible) }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(buildingSublayer.name)
                            Checkbox(checked = checked, onCheckedChange = {
                                checked = it
                                localSceneViewModel.checkCategory(buildingSublayer, it)
                            })
                            if (checked) {
                                // show drop down list of component sublayers
                                Text(">>>")
                            }
                        }
                    }
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
//@Composable
//fun BottomSheetPreview() {
//    SamplePreviewSurface {
//        BottomSheet(
//            isVisible = true,
//            sheetTitle = "Bottom sheet options",
//        ) {
//            SampleOptions()
//        }
//    }
//}
