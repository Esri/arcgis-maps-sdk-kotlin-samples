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
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.view.DrawStatus
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapViewInteractionOptions
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.ViewpointPersistence
import com.esri.arcgismaps.sample.geoviewnperepro.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val application = LocalContext.current.applicationContext as Application

    // create a viewmodel to back each mapview in the list
    val mvms = List(10){MapViewModel(application)}

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    // put item cards, each with their own MapView and model, into the column
                    for (i in mvms.indices){
                        item { TripItemCard(mvms[i], i.toLong()) }
                    }
                }
            }
        }
    )
}

@Composable
fun TripItemCard(mvm: MapViewModel, tripId: Long) {
    Card(
        Modifier
            .height(250.dp)
            .padding(horizontal = 25.dp, vertical = 10.dp)
            .fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth()) {
            MapItemCached(
                modifier = Modifier.width(250.dp),
                startPoint = mvm.startPoint,
                endPoint = mvm.endPoint,
                startPictureMarkerSymbol = mvm.pms,
                endPictureMarkerSymbol = mvm.pms,
                tripId = tripId,
                cachedMapImage = mvm.mapImage,
                onTap = {},
                onSaveEvent = {image: BitmapDrawable -> mvm.mapImage = image.bitmap}
            )
            Text(text = "Trip #$tripId")
        }
    }
}

@Composable
fun MapItemCached(
    modifier: Modifier = Modifier,
    startPoint: Point,
    endPoint: Point,
    startPictureMarkerSymbol: PictureMarkerSymbol,
    endPictureMarkerSymbol: PictureMarkerSymbol,
    tripId: Long = 0,
    cachedMapImage: Bitmap? = null,
    onTap: () -> Unit,
    onSaveEvent: (BitmapDrawable) -> Unit,
) {
    val map by remember {
        mutableStateOf(ArcGISMap(BasemapStyle.ArcGISImagery))
    }.apply { this.value.initialViewpoint = Viewpoint(39.8, -98.6, 10e7) }

    val graphicsOverlay = remember { GraphicsOverlay() }

    val graphicStart = Graphic(startPoint, startPictureMarkerSymbol)
    val graphicEnd = Graphic(endPoint, endPictureMarkerSymbol)

    graphicsOverlay.graphics.add(graphicStart)
    graphicsOverlay.graphics.add(graphicEnd)

    // Create a list of graphics overlays used by the MapView
    val graphicsOverlays = remember { listOf(graphicsOverlay) }
    val coroutineScope = rememberCoroutineScope()
    val mapViewProxy = remember {
        MapViewProxy()
    }

    // display map image instead of map if it is available
    if (cachedMapImage == null) {
        MapView(
            modifier = modifier.fillMaxSize(),
            arcGISMap = map,
            mapViewProxy = mapViewProxy,
            graphicsOverlays = graphicsOverlays,
            mapViewInteractionOptions = MapViewInteractionOptions(
                isEnabled = false,
                isMagnifierEnabled = false,
                isZoomEnabled = false,
                isRotateEnabled = false,
                isFlingEnabled = false,
                allowMagnifierToPan = false,
                isPanEnabled = false
            ),
            isAttributionBarVisible = false,
            viewpointPersistence = ViewpointPersistence.None,
            onSingleTapConfirmed = { onTap() },
            onDrawStatusChanged = {
                if (it == DrawStatus.Completed) {
                    coroutineScope.launch {
                        mapViewProxy.exportImage().onSuccess { image ->
                            // save this image in viewModel and retrieve back as cachedMapImage.
                            onSaveEvent(image)
                        }
                    }
                }
            }
        )
    } else {
        Image(
            bitmap = cachedMapImage.asImageBitmap(),
            contentDescription = "Map Image"
        )
    }
}
