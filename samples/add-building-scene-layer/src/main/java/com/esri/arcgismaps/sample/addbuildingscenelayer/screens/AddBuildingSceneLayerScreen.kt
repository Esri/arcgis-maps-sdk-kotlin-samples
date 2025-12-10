/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.addbuildingscenelayer.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.BuildingSceneLayer
import com.arcgismaps.mapping.layers.buildingscene.BuildingSublayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneViewingMode
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun AddBuildingSceneLayerScreen(sampleName: String) {
    // A Boolean value that indicates if the full model of the building scene layer is showing or not
    var isFullModel by remember { mutableStateOf(false) }

    val elevationSource = remember {
        ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
    }

    val buildingSceneLayer = remember {
        BuildingSceneLayer("https://www.arcgis.com/home/item.html?id=669f6279c579486eb4a0acc7eb59d7ca")
            .apply {
                // Sets the altitude offset of the building scene layer.
                // Upon first inspection of the model, it does not line up with the global
                // elevation layer perfectly. To fix this, add an altitude offset to align
                // the model with the ground surface.
                altitudeOffset = 1.0
            }
    }

    val localSceneViewProxy = remember { LocalSceneViewProxy() }

    // The overview sublayer which represents the exterior shell of the building.
    var overviewSublayer: BuildingSublayer? = null

    // The full model sublayer which contains all the features of the building.
    var fullModelSublayer: BuildingSublayer? = null

    val arcGISScene = remember {
        ArcGISScene(
            BasemapStyle.ArcGISTopographic,
            SceneViewingMode.Local
        ).apply {
            baseSurface.elevationSources.add(elevationSource)
            operationalLayers.add(buildingSceneLayer)
        }
    }

    // set a suitable camera to view the building
    LaunchedEffect(Unit) {
        arcGISScene.load().onSuccess {
            localSceneViewProxy.setViewpointCamera(
                Camera(
                    Point(
                        x = -13045114.646632874,
                        y = 4036662.761124578,
                        z = 511.0,
                        spatialReference = SpatialReference.webMercator()
                    ),
                    heading = 343.0,
                    pitch = 64.0,
                    roll = 0.0
                )
            )
        }
    }

    // Get the overview and full model sublayers for the toggle
    LaunchedEffect(Unit) {
        buildingSceneLayer.load().onSuccess {
            val sublayers = buildingSceneLayer.sublayers
            overviewSublayer = sublayers.first { it.name == "Overview" }
            fullModelSublayer = sublayers.first { it.name == "Full Model" }
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                LocalSceneView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    localSceneViewProxy = localSceneViewProxy,
                    scene = arcGISScene,
                )
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Full Model")
                    Switch(checked = isFullModel,
                        onCheckedChange = { isChecked ->
                            isFullModel = isChecked
                            fullModelSublayer?.isVisible = isFullModel
                            overviewSublayer?.isVisible = !isFullModel
                        },
                        modifier = Modifier.padding(10.dp))
                }
            }
        }
    )
}
