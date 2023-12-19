/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.queryfeaturetable.screens

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.MapViewpointOperation
import com.arcgismaps.toolkit.geocompose.ViewpointChangedState
import com.arcgismaps.toolkit.geocompose.rememberViewpointChangedStateForBoundingGeometry
import com.esri.arcgismaps.sample.queryfeaturetable.R
import com.esri.arcgismaps.sample.queryfeaturetable.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {
    // coroutineScope that will be cancelled when this call leaves the composition
    val sampleCoroutineScope = rememberCoroutineScope()
    // create a ViewModel to handle MapView interactions
    val mapViewModel = remember { MapViewModel(application, sampleCoroutineScope) }
    // map used to display the feature layer
    val map = ArcGISMap(BasemapStyle.ArcGISTopographic)
    val usaViewpoint = Viewpoint(
        center = Point(-11e6, 5e6, SpatialReference.webMercator()),
        scale = 1e8
    )


    // use symbols to show U.S. states with a black outline and yellow fill
    val lineSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.black,
        width = 1.0f
    )
    val fillSymbol = SimpleFillSymbol(
        style = SimpleFillSymbolStyle.Solid,
        color = Color.fromRgba(255, 255, 0, 255),
        outline = lineSymbol
    )

    // create the feature layer using the service feature table
    val featureLayer by lazy {
        FeatureLayer.createWithFeatureTable(mapViewModel.serviceFeatureTable)
    }

    featureLayer.apply {
        // set renderer for the feature layer
        renderer = SimpleRenderer(fillSymbol)
        opacity = 0.8f
        maxScale = 10000.0
    }

    // add the feature layer to the map's operational layers
    map.operationalLayers.add(featureLayer)
//    var mapViewpointOperation: MapViewpointOperation? by remember { mutableStateOf(null) }
//    mapViewpointOperation = MapViewpointOperation.Set(usaViewpoint)

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(it)) {
                // composable function that wraps the MapView
                MapView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    arcGISMap = map,
                    viewpointOperation = MapViewpointOperation.Set(viewpoint = usaViewpoint),
                    viewpointChangedState = rememberViewpointChangedStateForBoundingGeometry(
                        onViewpointChanged = { mapViewModel.stateGeometry }
                    )

                )
                SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    onQuerySubmit = { searchQuery ->
                        mapViewModel.searchForState(searchQuery, featureLayer) }
                )
            }

            mapViewModel.messageDialogVM.apply {
                if (dialogStatus) {
                    // display a dialog if the sample encounters an error
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
