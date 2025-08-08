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

package com.esri.arcgismaps.sample.applyrendererstoscenelayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.ClassBreak
import com.arcgismaps.mapping.symbology.ClassBreaksRenderer
import com.arcgismaps.mapping.symbology.ColorMixMode
import com.arcgismaps.mapping.symbology.MaterialFillSymbolLayer
import com.arcgismaps.mapping.symbology.MultilayerMeshSymbol
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.SymbolLayerEdges3D
import com.arcgismaps.mapping.symbology.UniqueValue
import com.arcgismaps.mapping.symbology.UniqueValueRenderer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the ApplyRenderersToSceneLayer sample.
 * Displays a 3D buildings scene layer and allows switching between different renderers.
 */
class ApplyRenderersToSceneLayerViewModel(app: Application) : AndroidViewModel(app) {
    // URLs for the Helsinki buildings scene and world elevation
    private val helsinkiBuildingsSceneLayerUrl =
        "https://services.arcgis.com/V6ZHFr6zdgNZuVG0/arcgis/rest/services/Helsinki_buildings/SceneServer"
    private val worldElevationServiceUrl =
        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"

    // ArcGISTiledElevationSource for world elevation
    private val elevationSource: ArcGISTiledElevationSource by lazy {
        ArcGISTiledElevationSource(worldElevationServiceUrl)
    }

    // ArcGISSceneLayer for Portland buildings
    val sceneLayer: ArcGISSceneLayer by lazy {
        ArcGISSceneLayer(helsinkiBuildingsSceneLayerUrl)
    }

    // Camera location for Helsinki
    private val cameraLocation : Point by lazy {
        Point(
            x = 2778453.800861455,
            y = 8436451.388274617,
            z = 387.45244609192014,
            spatialReference = SpatialReference.webMercator()
        )
    }

    // Camera to view the scene
    private val camera : Camera by lazy {
        Camera(
            locationPoint = cameraLocation,
            heading = 308.9,
            pitch = 50.7,
            roll = 0.0
        )
    }

    // Create the ArcGISScene with light gray basemap
    val arcGISScene by mutableStateOf(
        ArcGISScene(BasemapStyle.ArcGISLightGray).apply {
            // Add the Portland buildings scene layer
            operationalLayers.add(sceneLayer)
            // Add the elevation source to the base surface
            baseSurface = Surface().apply {
                elevationSources.add(elevationSource)
            }
            // Set the viewpoint camera at Portland
            initialViewpoint = Viewpoint(
                boundingGeometry = cameraLocation,
                camera = camera
            )
        }
    )

    // Renderer selection state
    val rendererTypes = listOf(
        RendererType.SimpleRenderer,
        RendererType.UniqueValueRenderer,
        RendererType.ClassBreaksRenderer,
        RendererType.NullRenderer
    )
    private val _selectedRendererType = MutableStateFlow(RendererType.SimpleRenderer)
    val selectedRendererType = _selectedRendererType.asStateFlow()

    // Simple renderer created from a multilayer mesh symbol with a material fill symbol layer
    // The colorMixMode of the material fill symbol layer is Replace which will replace the texture with the new color.
    // Edges are also set.
    private val simpleRenderer : SimpleRenderer by lazy {
        SimpleRenderer(
            symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.yellow).apply {
                colorMixMode= ColorMixMode.Replace
                edges = SymbolLayerEdges3D(color = Color.black, width = 0.5)
            })
        )
    }

    // Unique value renderer using multilayer mesh symbols based on usage of the building
    // The material fill symbol layers use the default colorMixMode which is Multiply.
    // Multiply colorMixMode will multiply the initial texture color with the new color.
    private val uniqueValueRenderer : UniqueValueRenderer by lazy {
        UniqueValueRenderer(
            fieldNames = listOf("usage"),
            defaultSymbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(230, 230, 230, 255))),
            uniqueValues = listOf(
                UniqueValue(
                    description = "commercial buildings",
                    label = "commercial buildings",
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(245, 213, 169, 200))),
                    values = listOf("general or commercial")
                ),
                UniqueValue(
                    description = "residential buildings",
                    label = "residential buildings",
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(210, 254, 208, 255))),
                    values = listOf("residential")
                ),
                UniqueValue(
                    description = "other",
                    label = "other",
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(253, 198, 227, 150))),
                    values = listOf("other")
                )
            )
        )
    }

    // Class breaks renderer using multilayer mesh symbols based on year completed of the building
    // The colorMixMode used in the material fill symbol layer is Tint which will set the new color on the desaturated texture.
    private val classBreaksRenderer : ClassBreaksRenderer by lazy {
        ClassBreaksRenderer(
            fieldName = "yearCompleted",
            classBreaks = listOf(
                ClassBreak(
                    description = "before 1900",
                    label = "before 1900",
                    minValue = 1725.0,
                    maxValue = 1899.0,
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(230, 238, 207, 255)).apply {
                        colorMixMode = ColorMixMode.Tint
                    })
                ),
                ClassBreak(
                    description = "1900 - 1956",
                    label = "1900 - 1956",
                    minValue = 1900.0,
                    maxValue = 1956.0,
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(155, 196, 193, 255)).apply {
                        colorMixMode = ColorMixMode.Tint
                    })
                ),
                ClassBreak(
                    description = "1957 - 2000",
                    label = "1957 - 2000",
                    minValue = 1957.0,
                    maxValue = 2000.0,
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(105, 168, 183, 255)).apply {
                        colorMixMode = ColorMixMode.Tint
                    })
                ),
                ClassBreak(
                    description = "after 2000",
                    label = "after 2000",
                    minValue = 2001.0,
                    maxValue = 3000.0,
                    symbol = MultilayerMeshSymbol(MaterialFillSymbolLayer(Color.fromRgba(75, 126, 152, 255)).apply {
                        colorMixMode = ColorMixMode.Tint
                    })
                )
            )
        )
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the scene
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Switches the renderer according to the selected type.
     */
    fun updateSceneLayerRenderer(rendererType: RendererType) {
        _selectedRendererType.value = rendererType
        sceneLayer.renderer = when (rendererType) {
            RendererType.SimpleRenderer -> {
                simpleRenderer
            }
            RendererType.UniqueValueRenderer -> {
                uniqueValueRenderer
            }
            RendererType.ClassBreaksRenderer -> {
                classBreaksRenderer
            }
            RendererType.NullRenderer -> {
                null
            }
        }
    }

    /**
     * Enum representing the different renderer types.
     */
    enum class RendererType(val label: String) {
        SimpleRenderer("SimpleRenderer - Buildings without texture"),
        UniqueValueRenderer("UniqueValueRenderer - Buildings by usage"),
        ClassBreaksRenderer("ClassBreaksRenderer - Buildings by year completed"),
        NullRenderer("Null renderer - Buildings with original texture")
    }
}
