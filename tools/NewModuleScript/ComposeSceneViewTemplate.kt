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

package com.esri.arcgismaps.sample.displaycomposablemapview.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

/**
 * Wraps the MapView in a Composable function.
 */
@Composable
fun ComposeSceneView(
    // TODO: Modify this function to meet sample requirements
    modifier: Modifier = Modifier,
    arcGISMap: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight),
    viewpoint: Viewpoint = Viewpoint(39.8, -98.6, 10e7),
    onSingleTap: (mapPoint: Point?) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).also { mapView ->
                // add the MapView to the lifecycle observer
                lifecycleOwner.lifecycle.addObserver(mapView)
                // set the map
                mapView.map = arcGISMap
                // launch a coroutine to collect map taps
                lifecycleOwner.lifecycleScope.launch {
                    mapView.onSingleTapConfirmed.collect {
                        onSingleTap(it.mapPoint)
                    }
                }
            }
        },

        update = { view ->
            view.map = arcGISMap
            lifecycleOwner.lifecycleScope.launch {
                view.setViewpointAnimated(viewpoint)
            }
        }
    )
}
