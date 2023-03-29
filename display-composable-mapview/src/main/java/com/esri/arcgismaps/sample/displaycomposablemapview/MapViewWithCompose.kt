package com.esri.arcgismaps.sample.displaycomposablemapview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Wraps the MapView in a Composable function.
 */
@Composable
fun MapViewWithCompose(
    lifecycle: Lifecycle,
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    onSingleTap: (mapPoint: Point?) -> Unit = {},
) {
    AndroidView(
        // modifiers are used to set layout parameters
        modifier = Modifier.fillMaxSize(),
        // the factory parameter provides a context to create a classic Android view
        // called when the composable is created, but not when it's recomposed
        factory = { context ->
            MapView(context).also { mapView ->
                // add the MapView to the lifecycle observer
                lifecycle.addObserver(mapView)
                // set the map
                mapView.map = arcGISMap
                // launch a coroutine to collect map taps
                CoroutineScope(Dispatchers.Default).launch {
                    mapView.onSingleTapConfirmed.collect {
                        onSingleTap(it.mapPoint)
                    }
                }
            }
        },

        // update block runs every time this view is recomposed which only occurs
        // when a `State<T>` or `MutableState<T>` parameter is changed.
        update = { view -> // view is automatically cast to a MapView
            view.setViewpoint(viewpoint) // called only if the viewpoint is changed
        }
    )
}
