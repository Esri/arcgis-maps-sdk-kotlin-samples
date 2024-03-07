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

package com.esri.arcgismaps.sample.showmagnifier.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geocompose.MapView
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    // Create an ArcGISMap and Viewpoint
    val californiaViewpoint = Viewpoint(34.056295, -117.195800, 1000000.0)
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = californiaViewpoint
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            MapView(
                modifier = Modifier.fillMaxSize().padding(it),
                arcGISMap = arcGISMap,
                mapViewInteractionOptions = MapViewInteractionOptions(isMagnifierEnabled = true)
            )
        }
    )
}
