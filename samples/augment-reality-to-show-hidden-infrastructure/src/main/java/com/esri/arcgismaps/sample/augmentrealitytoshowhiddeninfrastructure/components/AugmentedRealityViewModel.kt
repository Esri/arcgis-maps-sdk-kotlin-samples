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

package com.esri.arcgismaps.sample.augmentrealitytoshowhiddeninfrastructure.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ElevationSource
import com.arcgismaps.mapping.NavigationConstraint
import com.arcgismaps.mapping.symbology.MultilayerPolylineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SolidStrokeSymbolLayer
import com.arcgismaps.mapping.symbology.StrokeSymbolLayerLineStyle3D
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import kotlinx.coroutines.launch

class AugmentedRealityViewModel(app: Application) : AndroidViewModel(app) {

    // Graphics overlay for the 3D pipes
    val pipeGraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    // Graphics overlay for the shadow of pipes underground
    val pipeShadowGraphicsOverlay = GraphicsOverlay().apply {
        opacity = 0.6f
    }

    // Graphics overlay for the leaders
    val leaderGraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    // Create a scene with an elevation source and grid and surface hidden
    val arcGISScene = ArcGISScene().apply {
        baseSurface.apply {
            elevationSources.add(ElevationSource.fromTerrain3dService())
            backgroundGrid.isVisible = false
            opacity = 0.0f
            navigationConstraint = NavigationConstraint.None
        }
    }

    // Define a red 3D stroke symbol to show the pipe
    private val pipeStrokeSymbol = SolidStrokeSymbolLayer(
        width = 0.3,
        color = Color.red,
        lineStyle3D = StrokeSymbolLayerLineStyle3D.Tube
    )
    val pipeSymbol = MultilayerPolylineSymbol(listOf(pipeStrokeSymbol))

    // Define a red 2D stroke symbol to show the pipe shadow
    private val pipeShadowSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.red,
        width = 0.3f
    )

    val leaderSymbol = SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Dash,
        color = Color.red,
        width = 0.1f
    )

    init {
        // For each pipe in the shared repository
        SharedRepository.pipeInfoList.forEach {
            viewModelScope.launch {
                // Add Z values to the polyline using the base surface elevation
                val polylineWithZ = densifyAndAddZValues(it)
                // Add the 3D pipe to the pipe graphics overlay
                pipeGraphicsOverlay.graphics.add(Graphic(polylineWithZ, pipeSymbol))
                // Only add the shadow if the pipe is underground
                if (it.elevationOffset < 0) {
                    // Add the 2D pipe shadow to the shadow graphics overlay
                    pipeShadowGraphicsOverlay.graphics.add(Graphic(it.polyline, pipeShadowSymbol))

                    // Add leader lines connecting pipe vertices to shadow vertices
                    addLeaderLines(polylineWithZ, it.elevationOffset)
                }
            }
        }
    }

    /**
     * Adds Z values to the geometry by getting the elevation from the base surface.
     */
    private suspend fun densifyAndAddZValues(pipeInfo: PipeInfo): Polyline {
        // Densify the polyline to ensure it has enough points for elevation sampling
        val densifiedPolyline = GeometryEngine.densifyGeodeticOrNull(
            geometry = pipeInfo.polyline,
            maxSegmentLength = 1.0,
            lengthUnit = LinearUnit.meters,
            curveType = GeodeticCurveType.Geodesic
        ) as Polyline
        // Create a new polyline builder to construct the polyline with Z values
        val polylineBuilder = PolylineBuilder(SpatialReference(3857))
        // For each point in each part of the densified polyline
        densifiedPolyline.parts.forEach { part ->
            part.points.forEach { point ->
                arcGISScene.baseSurface.elevationSources.first().load().onSuccess {
                    arcGISScene.baseSurface.getElevation(point).let { elevationResult ->
                        // Get the elevation at the point
                        elevationResult.getOrNull()?.let { elevation ->
                            // Add the point with the elevation offset to the polyline builder
                            polylineBuilder.addPoint(
                                GeometryEngine.createWithZ(
                                    point,
                                    elevation + pipeInfo.elevationOffset
                                )
                            )
                        }
                    }
                }
            }
        }
        return polylineBuilder.toGeometry()
    }

    /**
     * Adds leader lines from the pipe vertices to the shadow vertices.
     */
    private fun addLeaderLines(pipePolyline: Polyline, elevationOffset: Float) {
        // For each point in each part of the densified polyline
        pipePolyline.parts.forEach { part ->
            part.points.forEach { point ->
                // Create a line from the 3D pipe vertex to a pont offset by the elevation offset
                val offsetPoint = GeometryEngine.createWithZ(
                    point,
                    point.z?.minus(elevationOffset)
                )
                val leaderLine = Polyline(listOf(point, offsetPoint))
                leaderGraphicsOverlay.graphics.add(Graphic(leaderLine, leaderSymbol))
            }
        }
    }
}
