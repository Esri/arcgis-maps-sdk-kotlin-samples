/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.screens

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.Color
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SelectionProperties
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.R
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components.AREA_OF_INTEREST_SIZE
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components.NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components.numberKeyToFeatureIndex
import com.esri.arcgismaps.sample.navigatemapviewandidentifyfeatureswithkeyboard.components.selectionHalo
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.flow.first

/**
 * Main screen layout for the sample app
 */
@Composable
fun NavigateMapViewAndIdentifyFeaturesWithKeyboardScreen(
    mapViewModel: NavigateMapViewAndIdentifyFeaturesWithKeyboardViewModel = viewModel()
) {
    val loadStatus by mapViewModel.arcGISMap.loadStatus.collectAsStateWithLifecycle()
    val drawStatus by mapViewModel.mapViewDrawStatus.collectAsStateWithLifecycle()
    val areaOfInterestSize = with(LocalDensity.current) { AREA_OF_INTEREST_SIZE.toDp() }
    val selectedOrderedFeature = mapViewModel.selectedFeatureIndex
        ?.let(mapViewModel.orderedFeatures::getOrNull)

    val sampleHostView = LocalView.current
    // Await the ArcGISMap and MapView to be fully loaded and drawn
    // then request focus on the MapView to enable keyboard navigation.
    LaunchedEffect(Unit) {
        snapshotFlow { loadStatus }
            .first { it is LoadStatus.Loaded }

        snapshotFlow { drawStatus }
            .first { it == DrawStatus.Completed }

        sampleHostView.findDescendantMapView()?.requestFocus()
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = stringResource(R.string.navigate_map_view_and_identify_features_with_keyboard_app_name)) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Box containing the MapView and centered area of interest indicator.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .animateContentSize()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                            when (keyEvent.key) {
                                Key.C -> {
                                    // Dismiss the callout when C is pressed, if displayed.
                                    mapViewModel.dismissCallout()
                                    true
                                }

                                else -> {
                                    // Show callout for the feature corresponding to number keys 1-9.
                                    numberKeyToFeatureIndex(keyEvent.key)?.let { index ->
                                        mapViewModel.selectFeatureForCallout(index)
                                    } ?: false
                                }
                            }
                        }
                ) {
                    MapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged(mapViewModel::updateMapViewSizeAndIdentify),
                        canFocus = true,
                        arcGISMap = mapViewModel.arcGISMap,
                        mapViewProxy = mapViewModel.mapViewProxy,
                        graphicsOverlays = listOf(mapViewModel.labelsOverlay),
                        selectionProperties = SelectionProperties(color = Color.selectionHalo),
                        onDrawStatusChanged = mapViewModel::handleDrawStatusChanged,
                        onNavigationChanged = mapViewModel::refreshFeaturesAfterNavigation,
                        content = {
                            selectedOrderedFeature?.let { orderedFeature ->
                                Callout(location = orderedFeature.point) {
                                    Column {
                                        Text(
                                            text = orderedFeature.name ?: "Restaurant",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = orderedFeature.formatedFeatureDetails(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (selectedOrderedFeature == null) {
                        // Area of interest rounded box indicator for feature selection.
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(areaOfInterestSize)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                // Bottom text instructions for the sample.
                SampleInstructions(isOverflowMessageVisible = mapViewModel.isOverflowMessageVisible)
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
private fun SampleInstructions(
    modifier: Modifier = Modifier,
    isOverflowMessageVisible: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Pan (Arrow Keys) and zoom (+ and - ) to bring restaurants into the area of interest. Press 1-9 for details, C to close Callout.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (isOverflowMessageVisible) {
            Text(
                text = "Too many features in the area. Zoom in to see fewer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Recursively search the view hierarchy for a [MapView] instance.
 */
private fun View.findDescendantMapView(): MapView? = when (this) {
    is MapView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findDescendantMapView() }
    else -> null
}
