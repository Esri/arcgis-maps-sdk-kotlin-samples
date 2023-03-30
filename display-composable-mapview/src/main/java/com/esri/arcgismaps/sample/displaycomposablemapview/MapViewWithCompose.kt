package com.esri.arcgismaps.sample.displaycomposablemapview

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
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
    arcGISMap: ArcGISMap,
    viewpoint: Viewpoint,
    onSingleTap: (mapPoint: Point?) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        // modifiers are used to set layout parameters
        modifier = Modifier.fillMaxSize(),
        // the factory parameter provides a context to create a classic Android view
        // called when the composable is created, but not when it's recomposed
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

        // update block runs every time this view is recomposed which only occurs
        // when a `State<T>` or `MutableState<T>` parameter is changed.
        update = { view -> // view is automatically cast to a MapView
            lifecycleOwner.lifecycleScope.launch {
                view.setViewpointAnimated(viewpoint) // called only if the viewpoint parameter is changed
                view.map = arcGISMap // called only if the arcGISMap parameter is changes
            }
        }
    )
}
