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
import com.arcgismaps.mapping.BasemapStyle
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

    val pipeGraphicsOverlay = GraphicsOverlay().apply {
        sceneProperties.surfacePlacement = SurfacePlacement.Absolute
    }

    val pipeShadowGraphicsOverlay = GraphicsOverlay().apply {
        opacity = 0.5f
    }

    // Create a scene with an elevation source and grid and surface hidden
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISHumanGeography).apply {
        baseSurface.elevationSources.add(ElevationSource.fromTerrain3dService())
        baseSurface.backgroundGrid.isVisible = false
        baseSurface.opacity = 0.0f
        baseSurface.navigationConstraint = NavigationConstraint.None
    }

    init {
        SharedRepository.pipeInfoList.forEach {

            val strokeSymbolLayer = SolidStrokeSymbolLayer(
                width = 0.3,
                color = Color.yellow,
                lineStyle3D = StrokeSymbolLayerLineStyle3D.Tube
            )

            val pipeSymbol = MultilayerPolylineSymbol(listOf(strokeSymbolLayer))

            viewModelScope.launch {

                pipeShadowGraphicsOverlay.graphics.add(
                    Graphic(
                        it.polyline,
                        SimpleLineSymbol(
                            style = SimpleLineSymbolStyle.Solid,
                            color = Color.red,
                            width = 0.3f
                        )
                    )
                )


                val polylineWithZ = densifyAndAddZValues(it)
                pipeGraphicsOverlay.graphics.add(
                    Graphic(
                        polylineWithZ, pipeSymbol
                    )
                )
            }
        }
    }

    /**
     * Adds Z values to the geometry by getting the elevation from the base surface.
     */
    private suspend fun densifyAndAddZValues(pipeInfo: PipeInfo): Polyline {
        val densifiedPolyline = GeometryEngine.densifyGeodeticOrNull(
            geometry = pipeInfo.polyline,
            maxSegmentLength = 1.0,
            lengthUnit = LinearUnit.meters,
            curveType = GeodeticCurveType.Geodesic
        ) as Polyline
        val polylineBuilder = PolylineBuilder(SpatialReference(3857))
        densifiedPolyline.parts.forEach { part ->
            part.points.forEach { point ->
                arcGISScene.baseSurface.elevationSources.first().load().onSuccess {
                    arcGISScene.baseSurface.getElevation(point).let { elevationResult ->
                        elevationResult.getOrNull()?.let { elevation ->
                            polylineBuilder.addPoint(
                                GeometryEngine.createWithZ(point, elevation + pipeInfo.offset)
                            )
                        }
                    }
                }
            }

        }
        return polylineBuilder.toGeometry()
    }
}
