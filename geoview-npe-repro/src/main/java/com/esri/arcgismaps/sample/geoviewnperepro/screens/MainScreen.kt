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

package com.esri.arcgismaps.sample.geoviewnperepro.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.geoviewnperepro.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val application = LocalContext.current.applicationContext as Application

    // create a list with a few maps in it
    val maps = List(10) {
        Pair(MapViewModel(application), remember { ArcGISMap(BasemapStyle.ArcGISStreets) })
    }
    for ((mvm, map) in maps) {
        map.apply { initialViewpoint = mvm.viewpoint.value }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    for ((_, map) in maps) {
                        item { TripItemCard(arcGISMap = map) }
                    }
                }
            }
        }
    )
}

@Composable
fun TripItemCard(arcGISMap: ArcGISMap) {
    Card(
        Modifier
            .height(250.dp)
            .padding(horizontal = 25.dp, vertical = 10.dp)
            .fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth()) {
            MapView(
                modifier = Modifier.width(250.dp),
                arcGISMap = arcGISMap,
                mapViewInteractionOptions = MapViewInteractionOptions(
                    isEnabled = false,
                    isMagnifierEnabled = false,
                    isZoomEnabled = false,
                    isRotateEnabled = false,
                    isFlingEnabled = false,
                    allowMagnifierToPan = false,
                    isPanEnabled = false
                ),
                onSingleTapConfirmed = {}
            )
            Text(text = "lorem ipsum dolor sit amet")
        }
    }
}