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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.data.Feature
import com.arcgismaps.mapping.layers.buildingscene.BuildingComponentSublayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingGroupSublayer
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneViewProxy
import com.arcgismaps.toolkit.popup.Popup
import com.esri.arcgismaps.sample.filterbuildingscenelayer.components.FilterBuildingSceneLayerViewModel
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.DropDownMenuBox
import com.esri.arcgismaps.sample.sampleslib.components.LoadingDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBuildingSceneLayerScreen(sampleName: String) {
    val viewModel: FilterBuildingSceneLayerViewModel = viewModel()
    
var isBottomSheetVisible by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    var showIdentifyProgress by remember { mutableStateOf(false)}

    val localSceneViewProxy = remember { LocalSceneViewProxy() }

    val coroutineScope = rememberCoroutineScope()

    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()

    val popupState by viewModel.popupState.collectAsStateWithLifecycle()

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
            // display a progress dialog to indicate the map loading status
            if (showLoadingDialog) {
                LoadingDialog(loadingMessage = "Loading layer...")
            }

            // display a progress dialog when an identify is take longer than expected
            if (showIdentifyProgress) {
                LoadingDialog("Identifying...")
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
                    scene = viewModel.scene,
                    localSceneViewProxy = localSceneViewProxy,
                    onSingleTapConfirmed = { singleTapConfirmedEvent ->
                        coroutineScope.launch {
                            viewModel.sublayerWithSelection?.clearSelection()

                            // only show identify progress if it has been more than one second
                            val identifyInProgress = coroutineScope.launch {
                                delay(1000.milliseconds)
                                showIdentifyProgress = true
                            }

                            localSceneViewProxy.identify(
                                layer = viewModel.buildingSceneLayer!!,
                                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                                tolerance = 12.dp,
                                returnPopupsOnly = false,
                                maximumResults = 1
                            ).onSuccess { identifyLayerResult ->
                                identifyInProgress.cancel()
                                showIdentifyProgress = false

                                val results = identifyLayerResult.sublayerResults

                                if (results.isNotEmpty()) {
                                    val element = results.first().geoElements.first()
                                    val popup = com.arcgismaps.mapping.popup.Popup(element)
                                    viewModel.createPopupState(popup)

                                    val sublayer =
                                        results.first().layerContent as BuildingComponentSublayer
                                    sublayer.selectFeature(element as Feature)
                                    viewModel.sublayerWithSelection = sublayer
                                }
                            }.onFailure { throwable ->
                                identifyInProgress.cancel()
                                showIdentifyProgress = false

                                viewModel.messageDialogVM.showMessageDialog(throwable)
                            }
                        }
                    }
                )
            }

            BottomSheet(
                isVisible = isBottomSheetVisible,
                sheetTitle = "Settings",
                onDismissRequest = { isBottomSheetVisible = false },
            ) {
                Column(modifier = Modifier
                    .fillMaxHeight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloorSelector()
                    HorizontalDivider()
                    CategorySelector()
                }
            }

            popupState?.let { popupState ->
                ModalBottomSheet(modifier = Modifier.wrapContentSize(),
                    onDismissRequest = viewModel::dismissPopup,
                    sheetState = sheetState) {
                    Popup(
                        popupState = popupState,
                        onDismiss = viewModel::dismissPopup,
                        modifier = Modifier.fillMaxSize()
                    )
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

/**
 * A menu to select floors
 */
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

/**
 * Check boxes to select building categories and sub-categories
 */
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
