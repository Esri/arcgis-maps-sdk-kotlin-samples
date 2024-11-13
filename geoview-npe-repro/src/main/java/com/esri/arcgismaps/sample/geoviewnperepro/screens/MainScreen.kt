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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.geometry.Envelope
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
fun MainScreen(mapViewModel: MapViewModel, sampleName: String, onTap: (String) -> Unit) {

    val cachedMapImageState by mapViewModel.cacheMapImageStateFlow.collectAsStateWithLifecycle()
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
                    for (i in 0..10) {
                        item {
                            TripItemCard(
                                mvm = mapViewModel,
                                mapImage = cachedMapImageState.getOrDefault(i.toLong(), null),
                                tripId = i.toLong(),
                                onTap = onTap
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TripItemCard(mvm: MapViewModel, mapImage: Bitmap?, tripId: Long, onTap: (String) -> Unit) {
    Card(
        Modifier
            .height(250.dp)
            .padding(horizontal = 25.dp, vertical = 10.dp)
            .clickable {
                onTap("Trip : $tripId")
            }
            .fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth()) {
            MapItemCached(
                modifier = Modifier.fillMaxWidth(0.4f),
                startPoint = mvm.startPoint,
                endPoint = mvm.endPoint,
                startPictureMarkerSymbol = mvm.pms,
                endPictureMarkerSymbol = mvm.pms,
                tripId = tripId,
                cachedMapImage = mapImage,
                onTap = {
                    onTap("Trip : $tripId")
                },
                onSaveEvent = { tripId, image -> mvm.cacheMapImage(tripId, image) }
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
    onSaveEvent: (Long, Bitmap) -> Unit,
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
    val envelope = Envelope(startPoint, endPoint)
    setupBaseMap(map = map, envelope = envelope)
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
                            onSaveEvent(tripId, image.bitmap)
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

private fun setupBaseMap(
    map: ArcGISMap,
    envelope: Envelope,
) {
    try {
        map.initialViewpoint = Viewpoint(envelope)
        map.maxScale = 1500.00
        map.minScale = 3.7E7
    } catch (e: ArcGISException) {
        e.printStackTrace()
    }
}
