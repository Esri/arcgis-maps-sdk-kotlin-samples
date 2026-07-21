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

package com.esri.arcgismaps.sample.displaylocalscene.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneViewingMode
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun DisplayLocalSceneScreen(
    sampleName: String,
    messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()
) {
    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                val sceneLayer = remember {
                    ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=61da8dc1a7bc4eea901c20ffb3f8b7af")
                }

                val elevationSource = remember {
                    ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
                }

                val arcGISScene = remember {
                    ArcGISScene(
                        viewingMode = SceneViewingMode.Local,
                        basemapStyle = BasemapStyle.ArcGISTopographic
                    ).apply {
                        operationalLayers.add(sceneLayer)
                        baseSurface.elevationSources.add(elevationSource)

                        initialViewpoint = Viewpoint(
                            center = Point(
                                x = 19455026.8116,
                                y = -5054995.7415,
                                spatialReference = SpatialReference.webMercator()
                            ),
                            scale = 8314.6991,
                            camera = Camera(
                                locationPoint = Point(
                                    x = 19455578.6821,
                                    y = -5056336.2227,
                                    z = 1699.3366,
                                    spatialReference = SpatialReference.webMercator()),
                                heading = 338.7410,
                                pitch = 40.3763,
                                roll = 0.0,
                            )
                        )
                        clippingArea = Envelope(
                            xMin = 19454578.8235,
                            yMin = -5055381.4798,
                            xMax = 19455518.8814,
                            yMax = -5054888.4150,
                            spatialReference = SpatialReference.webMercator()
                        )
                        isClippingEnabled = true
                    }
                }

                LocalSceneView(
                    modifier = Modifier.fillMaxSize(),
                    scene = arcGISScene,
                    onCriticalErrorChanged = messageDialogVM::showMessageDialog,
                    onGeoModelErrorChanged = messageDialogVM::showMessageDialog
                )
            }

            messageDialogVM.apply {
                if (dialogStatus) {
                    MessageDialog(
                        title = messageTitle,
                        description = messageDescription,
                        onDismissRequest = ::dismissDialog
                    )
                }
            }
        }
    )
}
