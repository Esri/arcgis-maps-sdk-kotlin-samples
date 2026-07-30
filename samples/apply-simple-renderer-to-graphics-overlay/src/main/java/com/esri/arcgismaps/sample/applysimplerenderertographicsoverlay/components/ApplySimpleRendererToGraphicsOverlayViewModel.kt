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

package com.esri.arcgismaps.sample.applysimplerenderertographicsoverlay.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.arcgisservices.LabelingPlacement
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.labeling.LabelDefinition
import com.arcgismaps.mapping.labeling.SimpleLabelExpression
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.GraphicsRenderingMode
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplySimpleRendererToGraphicsOverlayViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
        initialViewpoint = Viewpoint(latitude = 34.0529, longitude = -118.2437, scale = 350_000.0)
    }

    val messageDialogVM = MessageDialogViewModel()

    private val pointGraphicsOverlay = GraphicsOverlay(renderingMode = GraphicsRenderingMode.Static).apply {
        renderer = SimpleRenderer(createSymbol(GeometryKind.Point, Color.red))
        labelDefinitions.add(createLabelDefinition())
    }

    private val polygonGraphicsOverlay = GraphicsOverlay(renderingMode = GraphicsRenderingMode.Static).apply {
        renderer = SimpleRenderer(createSymbol(GeometryKind.Polygon, Color.blue))
        labelDefinitions.add(createLabelDefinition())
    }

    val graphicsOverlays = listOf(pointGraphicsOverlay, polygonGraphicsOverlay)

    val graphicCountOptions = listOf(10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000)

    var selectedGraphicCountIndex by mutableIntStateOf(0)
        private set

    val selectedGraphicCount: Int
        get() = graphicCountOptions[selectedGraphicCountIndex]

    var showOverlays by mutableStateOf(false)
        private set

    var status by mutableStateOf("Select a graphic count and tap Reproduce.")
        private set

    var isWorking by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    fun updateGraphicCount(index: Int) {
        selectedGraphicCountIndex = index.coerceIn(graphicCountOptions.indices)
        status = "Selected ${selectedGraphicCount.formatCount()} graphics."
    }

    fun reproduceIssue() {
        if (isWorking) return
        viewModelScope.launch {
            isWorking = true
            showOverlays = false
            clearOverlayGraphics()
            status = "Building ${selectedGraphicCount.formatCount()} graphics on Dispatchers.IO."
            val graphics = withContext(Dispatchers.IO) { buildUserSubmittedFeatureGraphics(selectedGraphicCount) }
            status = "Adding graphics to overlays on Dispatchers.IO."
            withContext(Dispatchers.IO) {
                pointGraphicsOverlay.graphics.addAll(graphics.pointGraphics)
                polygonGraphicsOverlay.graphics.addAll(graphics.polygonGraphics)
            }
            status = "Registering overlays with MapView."
            showOverlays = true
            status = "Registered ${selectedGraphicCount.formatCount()} graphics in 2 overlays."
            isWorking = false
        }
    }

    fun clearOverlays() {
        showOverlays = false
        clearOverlayGraphics()
        isWorking = false
        status = "Cleared overlays."
    }

    private fun clearOverlayGraphics() {
        pointGraphicsOverlay.graphics.clear()
        polygonGraphicsOverlay.graphics.clear()
    }

    private fun buildUserSubmittedFeatureGraphics(totalCount: Int): UserSubmittedFeatureGraphics {
        val pointCount = totalCount * 8 / 15
        val polygonCount = totalCount - pointCount
        return UserSubmittedFeatureGraphics(
            pointGraphics = buildGraphics(GeometryKind.Point, pointCount),
            polygonGraphics = buildGraphics(GeometryKind.Polygon, polygonCount)
        )
    }

    private fun buildGraphics(geometryKind: GeometryKind, count: Int): List<Graphic> {
        return List(count) { graphicIndex ->
            Graphic(
                geometry = createGeometry(geometryKind, graphicIndex),
                attributes = mapOf("sample_id" to "${geometryKind.label}-$graphicIndex")
            )
        }
    }

    private fun createGeometry(geometryKind: GeometryKind, graphicIndex: Int): Geometry {
        val column = graphicIndex % 120
        val row = graphicIndex / 120
        val longitude = -118.62 + (column * 0.006)
        val latitude = 33.78 + (row * 0.004)
        return when (geometryKind) {
            GeometryKind.Point -> Point(longitude, latitude, SpatialReference.wgs84())
            GeometryKind.Polygon -> PolygonBuilder(SpatialReference.wgs84()) {
                addPoint(Point(longitude - 0.0015, latitude - 0.0015))
                addPoint(Point(longitude + 0.0015, latitude - 0.0015))
                addPoint(Point(longitude + 0.0015, latitude + 0.0015))
                addPoint(Point(longitude - 0.0015, latitude + 0.0015))
            }.toGeometry()
        }
    }

    private fun createSymbol(geometryKind: GeometryKind, color: Color): Symbol {
        return when (geometryKind) {
            GeometryKind.Point -> SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, color, 5f)
            GeometryKind.Polygon -> SimpleFillSymbol(
                SimpleFillSymbolStyle.Solid,
                color.withAlpha(96),
                SimpleLineSymbol(SimpleLineSymbolStyle.Solid, color, 0.5f)
            )
        }
    }

    private fun Color.withAlpha(alpha: Int): Color = Color.fromRgba(red, green, blue, alpha)

    private fun createLabelDefinition(): LabelDefinition {
        return LabelDefinition(
            SimpleLabelExpression("[sample_id]"),
            TextSymbol().apply {
                color = Color.white
                size = 8f
            }
        ).apply {
            placement = LabelingPlacement.PointAboveCenter
        }
    }

    private fun Int.formatCount(): String = "% ,d".replace(" ", "").format(this)
}

private data class UserSubmittedFeatureGraphics(
    val pointGraphics: List<Graphic>,
    val polygonGraphics: List<Graphic>
)

private enum class GeometryKind(val label: String) {
    Point("Point"),
    Polygon("Polygon")
}
