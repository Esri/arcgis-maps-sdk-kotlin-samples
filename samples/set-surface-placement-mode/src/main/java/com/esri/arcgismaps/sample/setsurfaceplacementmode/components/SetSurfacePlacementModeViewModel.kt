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

package com.esri.arcgismaps.sample.setsurfaceplacementmode.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetSurfacePlacementModeViewModel(application: Application) : AndroidViewModel(application) {

    private val arcGISSceneLayerUrl =
        "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Brest/SceneServer"
    private val elevationSourceUrl =
        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"

    val zMin = 0.0
    val zMax = 140.0
    val zMid = (zMin + zMax) / 2.0

    // Create ArcGISScene with an imagery basemap, elevation, and buildings in Brest, France
    val arcGISScene: ArcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        baseSurface = Surface().apply {
            elevationSources.add(ArcGISTiledElevationSource(elevationSourceUrl))
        }
        operationalLayers.add(ArcGISSceneLayer(uri = arcGISSceneLayerUrl))

        val viewpointPoint = Point(
            x = -4.4595,
            y = 48.3889,
            z = 80.0,
            spatialReference = SpatialReference.wgs84()
        )
        val camera = Camera(
            locationPoint = viewpointPoint,
            heading = 330.0,
            pitch = 97.0,
            roll = 0.0
        )
        initialViewpoint = Viewpoint(
            boundingGeometry = viewpointPoint,
            camera = camera
        )
    }

    // Graphics overlays for each surface placement mode
    private val overlayAbsolute = createGraphicsOverlay(
        surfacePlacement = SurfacePlacement.Absolute,
        label = "Absolute",
        offset = 0.0
    )
    private val overlayDrapedBillboarded = createGraphicsOverlay(
        surfacePlacement = SurfacePlacement.DrapedBillboarded,
        label = "Draped Billboarded",
        offset = 0.0
    )
    private val overlayDrapedFlat = createGraphicsOverlay(
        surfacePlacement = SurfacePlacement.DrapedFlat,
        label = "Draped Flat",
        offset = 0.0
    )
    private val overlayRelative = createGraphicsOverlay(
        surfacePlacement = SurfacePlacement.Relative,
        label = "Relative",
        offset = 0.0
    )
    private val overlayRelativeToScene = createGraphicsOverlay(
        surfacePlacement = SurfacePlacement.RelativeToScene,
        label = "Relative to Scene",
        offset = 2e-4
    )

    // Expose overlays as a list for the SceneView composable
    val graphicsOverlays: List<GraphicsOverlay> = listOf(
        overlayAbsolute,
        overlayDrapedBillboarded,
        overlayDrapedFlat,
        overlayRelative,
        overlayRelativeToScene
    )

    var drapedMode by mutableStateOf(DrapedMode.Billboarded)
        private set

    var zValue by mutableDoubleStateOf(zMid)
        private set

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Set initial draped overlay visibility
        updateDrapedOverlayVisibility(DrapedMode.Billboarded)
        // Load the scene and handle any errors
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // Update the draped mode and overlay visibility
    fun updateDrapedMode(mode: DrapedMode) {
        drapedMode = mode
        updateDrapedOverlayVisibility(mode)
    }

    // Update z-value (expects meters)
    fun updateZValue(valueMeters: Float) {
        val newZ = valueMeters.toDouble()
        zValue = newZ
        updateGraphicsZValue(newZ)
    }

    private fun updateDrapedOverlayVisibility(mode: DrapedMode) {
        overlayDrapedBillboarded.isVisible = mode == DrapedMode.Billboarded
        overlayDrapedFlat.isVisible = mode == DrapedMode.Flat
    }

    private fun updateGraphicsZValue(zMeters: Double) {
        graphicsOverlays.forEach { overlay ->
            overlay.graphics.forEach { graphic ->
                graphic.geometry?.let { geometry ->
                    graphic.geometry = GeometryEngine.createWithZ(geometry, zMeters)
                }
            }
        }
    }

    private fun createGraphicsOverlay(
        surfacePlacement: SurfacePlacement,
        label: String,
        offset: Double
    ): GraphicsOverlay {
        // Create symbols
        val markerSymbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Triangle,
            color = Color.red,
            size = 20f
        )
        val textSymbol = TextSymbol(
            text = label,
            color = Color.fromRgba(0, 0, 255, 255),
            size = 20f,
            horizontalAlignment = HorizontalAlignment.Left,
            verticalAlignment = VerticalAlignment.Middle
        ).apply {
            haloColor = Color.cyan
            haloWidth = 2f
            offsetY = 20f
        }

        // Create a point with a tiny offset for RelativeToScene to avoid overlap
        val point = Point(
            x = -4.4609257 + offset,
            y = 48.3903965 + offset,
            z = zMid,
            spatialReference = SpatialReference.wgs84()
        )

        // Create graphics and overlay
        return GraphicsOverlay().apply {
            graphics.addAll(
                listOf(
                    Graphic(geometry = point, symbol = markerSymbol),
                    Graphic(geometry = point, symbol = textSymbol)
                )
            )
            sceneProperties.surfacePlacement = surfacePlacement
        }
    }

    enum class DrapedMode { Billboarded, Flat }
}
