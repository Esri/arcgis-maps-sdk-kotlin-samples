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

package com.esri.arcgismaps.sample.showextrudedfeatures.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.LoadStatus
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.FeatureRenderingMode
import com.arcgismaps.mapping.symbology.ExtrusionMode
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

private const val CENSUS_STATES_FEATURE_LAYER_URL =
    "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Census/MapServer/3"

/**
 * ViewModel for the ShowExtrudedFeatures sample.
 */
class ShowExtrudedFeaturesViewModel(application: Application) : AndroidViewModel(application) {

    // The scene shown in the SceneView composable.
    var arcGISScene = ArcGISScene(basemapStyle = BasemapStyle.ArcGISTopographic)

    // The service feature table and feature layer for the US states (used for extrusion)
    private val serviceFeatureTable = ServiceFeatureTable(uri = CENSUS_STATES_FEATURE_LAYER_URL)
    private val statesFeatureLayer: FeatureLayer =
        FeatureLayer.createWithFeatureTable(featureTable = serviceFeatureTable).apply {
            // Feature layer must be rendered dynamically for 3D extrusion to work.
            renderingMode = FeatureRenderingMode.Dynamic
        }

    // A simple renderer and its scene properties used to extrude features
    private val fillSymbol = SimpleFillSymbol(
        style = SimpleFillSymbolStyle.Solid,
        color = Color.blue,
        outline = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.white, // white with 50% alpha
            width = 1f
        )
    )

    private val renderer = SimpleRenderer(symbol = fillSymbol)

    // Message dialog view model for surfacing errors
    val messageDialogVM = MessageDialogViewModel()

    // Keep track of the currently selected statistic for extrusion
    enum class Statistic(val label: String, val expression: String) {
        TotalPopulation("Total Population", "[POP2007] / 10"),
        PopulationDensity("Population Density", "([POP07_SQMI] * 5000) + 100000")
    }

    // Public snapshot of the current statistic
    var currentStatistic by mutableStateOf(Statistic.TotalPopulation)

    init {
        // Build the scene initial viewpoint and add the feature layer.
        // Set a viewpoint using a camera that frames the United States
        val centerPoint = Point(x = -99.659448, y = 20.513652, z = 12_940_924.0)
        val camera = Camera(
            lookAtPoint = centerPoint,
            distance = 0.0,
            heading = 0.0,
            pitch = 15.0,
            roll = 0.0
        )
        arcGISScene.initialViewpoint = Viewpoint(center = centerPoint, camera = camera, scale = 5000.0)

        // Add the feature layer that will be extruded
        arcGISScene.operationalLayers.add(statesFeatureLayer)

        // Configure renderer scene properties (extrusion mode & expression) and apply to the layer.
        renderer.sceneProperties.apply {
            // Use base height extrusion so polygons extrude from the ground up
            extrusionMode = ExtrusionMode.BaseHeight
            // Default extrusion expression
            extrusionExpression = currentStatistic.expression
        }
        statesFeatureLayer.renderer = renderer

        // Load the scene and the feature layer, handle any errors by showing a message dialog
        viewModelScope.launch {
            arcGISScene.load().onFailure { throwable ->
                messageDialogVM.showMessageDialog(throwable.message.toString())
            }
            statesFeatureLayer.loadStatus.collect { loadStatus ->
                if(loadStatus is LoadStatus.FailedToLoad) {
                    messageDialogVM.showMessageDialog(loadStatus.error.message.toString())
                }
            }
        }
    }

    /**
     * Update the extrusion expression used by the renderer based on the selected [statistic].
     */
    fun updateExtrusionStatistic(statistic: Statistic) {
        currentStatistic = statistic
        // Update the renderer's extrusion expression
        renderer.sceneProperties.extrusionExpression = statistic.expression
        // If the layer is loaded, trigger a redraw by reassigning the renderer
        statesFeatureLayer.renderer = renderer
    }

    /**
     * Define a blue color.
     */
    private val Color.Companion.blue: Color
        get() {
            return fromRgba(0, 0, 255, 255)
        }
}

