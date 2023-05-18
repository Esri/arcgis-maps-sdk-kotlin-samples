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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.launch

/**
 * Wraps the MapView in a Composable function.
 */
@Composable
fun ComposeMapView(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel,
    onSingleTap: (SingleTapConfirmedEvent) -> Unit = {}
) {
    // get an instance of the current lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current
    // collect the latest state of the MapViewState
    val mapViewState by mapViewModel.mapViewState.collectAsState()
    val mapView = createMapViewInstance(lifecycleOwner)

    AndroidView(
        modifier = modifier,
        factory = { _ ->
            mapView.also { mapView ->
                // launch a coroutine to collect map taps
                lifecycleOwner.lifecycleScope.launch {
                    mapView.onSingleTapConfirmed.collect {
                        onSingleTap(it)
                    }
                }
            }
        },

        // recomposes the MapView on changes in the MapViewState
        update = { mapView ->
            mapView.apply {
                map = mapViewState.arcGISMap
                setViewpoint(mapViewState.viewpoint)
            }
        }
    )
}

/**
 * Create the MapView instance and add it to the Activity lifecycle
 */
@Composable
fun createMapViewInstance(lifecycleOwner: LifecycleOwner): MapView {
    // create the MapView
    val mapView = MapView(LocalContext.current)
    // add the side effects for MapView composition
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(mapView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(mapView)
        }
    }
    return mapView
}
