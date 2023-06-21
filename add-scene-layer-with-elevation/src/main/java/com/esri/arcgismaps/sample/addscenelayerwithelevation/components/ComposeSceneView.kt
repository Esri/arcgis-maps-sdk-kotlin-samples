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

package com.esri.arcgismaps.sample.addscenelayerwithelevation.components

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import kotlinx.coroutines.launch

/**
 * Wraps the MapView in a Composable function.
 */
@Composable
fun ComposeSceneView(
    modifier: Modifier = Modifier,
    sceneViewModel: SceneViewModel,
    onSingleTap: (SingleTapConfirmedEvent) -> Unit = {}
) {
    // get an instance of the current lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current
    // collect the latest state of the MapViewState
    val sceneViewState = sceneViewModel.sceneViewState
    // create and add MapView to the activity lifecycle
    val sceneView = createSceneViewInstance(lifecycleOwner)

    // wrap the MapView as an AndroidView
    AndroidView(
        modifier = modifier,
        factory = { sceneView },
        // recomposes the MapView on changes in the MapViewState
        update = { sceneView ->
            sceneView.apply {
                scene = sceneViewState.arcGISScene
                setViewpointCamera(sceneViewState.camera)
            }
        }
    )

    // launch coroutine functions in the composition's CoroutineContext
    LaunchedEffect(Unit) {
        launch {
            sceneView.onSingleTapConfirmed.collect {
                onSingleTap(it)
            }
        }
    }
}

/**
 * Create the MapView instance and add it to the Activity lifecycle
 */
@Composable
fun createSceneViewInstance(lifecycleOwner: LifecycleOwner): SceneView {
    // create the MapView
    val sceneView = SceneView(LocalContext.current)
    // add the side effects for MapView composition
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(sceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(sceneView)
        }
    }
    return sceneView
}