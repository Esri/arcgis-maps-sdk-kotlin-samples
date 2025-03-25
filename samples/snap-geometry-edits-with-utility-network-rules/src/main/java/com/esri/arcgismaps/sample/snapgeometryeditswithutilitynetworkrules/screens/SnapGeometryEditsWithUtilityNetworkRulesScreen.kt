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

package com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.screens

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.BottomSheet
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import com.esri.arcgismaps.sample.sampleslib.theme.SampleAppTheme
import com.esri.arcgismaps.sample.snapgeometryeditswithutilitynetworkrules.components.SnapGeometryEditsWithUtilityNetworkRulesViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun SnapGeometryEditsWithUtilityNetworkRulesScreen(sampleName: String) {
    val mapViewModel = viewModel<SnapGeometryEditsWithUtilityNetworkRulesViewModel>()

    // Collect latest UI state changes
    val isEditButtonEnabled by mapViewModel.isEditButtonEnabled.collectAsStateWithLifecycle()
    val assetGroupNameState by mapViewModel.assetGroupNameState.collectAsStateWithLifecycle()
    val assetTypeNameState by mapViewModel.assetTypeNameState.collectAsStateWithLifecycle()
    val snapSourcePropertyList by mapViewModel.snapSourcePropertyList.collectAsStateWithLifecycle()
    val isGeometryEditorStarted by mapViewModel.geometryEditor.isStarted.collectAsStateWithLifecycle()
    val canGeometryEditorUndo by mapViewModel.geometryEditor.canUndo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    if (snapSourcePropertyList.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            text = "Tap a point feature to edit",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        if (isEditButtonEnabled) {
                            TopHeader(assetGroupNameState, assetTypeNameState)
                        }
                    }
                }
                MapView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    arcGISMap = mapViewModel.arcGISMap,
                    geometryEditor = mapViewModel.geometryEditor,
                    graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
                    mapViewProxy = mapViewModel.mapViewProxy,
                    onSingleTapConfirmed = mapViewModel::identify
                )
            }

            BottomSheet(isVisible = snapSourcePropertyList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .animateContentSize()
                ) {
                    SnapSourcesPanel(
                        snapSourcePropertyList = snapSourcePropertyList,
                        onSnapSourcePropertyChanged = mapViewModel::setSnapSourceCheckedValue,
                        isGeometryEditorStarted = isGeometryEditorStarted,
                        canGeometryEditorUndo = canGeometryEditorUndo,
                        onDiscardGeometryChanges = mapViewModel::discardGeometryChanges,
                        onSaveGeometryChanges = mapViewModel::saveGeometryChanges,
                    )
                }
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
fun TopHeader(
    assetGroupNameState: String,
    assetTypeNameState: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Feature selected:",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AssetGroup: ",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = assetGroupNameState,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AssetType: ",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = assetTypeNameState,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun TopHeaderPreview() {
    SampleAppTheme {
        Surface {
            TopHeader(
                assetGroupNameState = "TestGroupName",
                assetTypeNameState = "TestTypeName"
            )
        }
    }
}
