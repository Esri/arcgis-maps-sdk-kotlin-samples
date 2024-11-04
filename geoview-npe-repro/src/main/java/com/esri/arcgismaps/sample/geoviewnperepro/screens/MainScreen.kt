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
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
                    var i = 0L
                    for ((mvm, map) in maps) {
                        item { TripItemCard(mvm, map, i) }
                        i+=1
                    }
                }
            }
        }
    )
}

@Composable
fun TripItemCard(mvm: MapViewModel, arcGISMap: ArcGISMap, tripId: Long) {
    val coroutineScope = rememberCoroutineScope()
    Card(
        Modifier
            .height(250.dp)
            .padding(horizontal = 25.dp, vertical = 10.dp)
            .fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth()) {
//            MapView(
//                modifier = Modifier.width(250.dp),
//                arcGISMap = arcGISMap,
//                mapViewProxy = mvm.mapViewProxy,
//                graphicsOverlays = listOf(mvm.staticGraphicsOverlay, mvm.geometryEditorGraphicsOverlay),
//                mapViewInteractionOptions = MapViewInteractionOptions(
//                    isEnabled = false,
//                    isMagnifierEnabled = false,
//                    isZoomEnabled = false,
//                    isRotateEnabled = false,
//                    isFlingEnabled = false,
//                    allowMagnifierToPan = false,
//                    isPanEnabled = false
//                ),
//                isAttributionBarVisible = false,
//                viewpointPersistence = ViewpointPersistence.None,
//                onSingleTapConfirmed = {},
//                onDrawStatusChanged = {
//                    if (it == DrawStatus.Completed){
//                        coroutineScope.launch {
//                            mvm.mapViewProxy.exportImage().onSuccess {image ->
//                            }
//                        }
//                    }
//                }
//            )
            MapItemCached(
                Modifier.width(250.dp),
                arcGISMap,
                mvm,
                startPoint = Point(-90.0,50.0, SpatialReference.wgs84()),
                endPoint = Point(-95.0,50.0, SpatialReference.wgs84()),
                startPictureMarkerSymbol = mvm.pms,
                endPictureMarkerSymbol = mvm.pms,
                tripId = tripId,
                cachedMapImage = mvm.mapImages[tripId.toInt()]
            ) {

            }
            Text(text = "lorem ipsum dolor sit amet")
        }
    }
}

@Composable
fun MapItemCached(
    modifier: Modifier = Modifier,
    map: ArcGISMap,
    mvm: MapViewModel,
    startPoint: Point,
    endPoint: Point,
    startPictureMarkerSymbol: PictureMarkerSymbol,
    endPictureMarkerSymbol: PictureMarkerSymbol,
    tripId: Long = 0,
    cachedMapImage: Bitmap? = null,
    onTap: () -> Unit,
) {

    //val graphicsOverlay = remember { GraphicsOverlay() }

    val graphicStart = Graphic(startPoint, startPictureMarkerSymbol)
    val graphicEnd = Graphic(endPoint, endPictureMarkerSymbol)

    mvm.staticGraphicsOverlay.graphics.add(graphicStart)
    mvm.staticGraphicsOverlay.graphics.add(graphicEnd)

    // Create a list of graphics overlays used by the MapView
    //val graphicsOverlays = remember { listOf(graphicsOverlay) }
    val coroutineScope = rememberCoroutineScope()
//    val mapViewProxy = remember {
//        MapViewProxy()
//    }

    // display map image instead of map if it is available
    if (cachedMapImage == null) {
        MapView(
            modifier = modifier.fillMaxSize(),
            arcGISMap = map,
            mapViewProxy = mvm.mapViewProxy,
            graphicsOverlays = listOf(mvm.staticGraphicsOverlay),
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
                        mvm.mapViewProxy.exportImage().onSuccess { image ->
                            // save this image in viewModel and retrieve back as cachedMapImage.
                            mvm.mapImages[tripId.toInt()] = image.bitmap
                            //event(Event.CacheMapImage(tripId, image.bitmap))
                        }
                    }
                }
            }
        )
    } else {
        Image(
            bitmap = cachedMapImage.asImageBitmap(),
            contentDescription = "Map Image"
        ).also { println("Rendering image!") }
    }
}
