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

package com.esri.arcgismaps.sample.filterfeaturesinscene.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.PolygonBuilder
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.layers.SceneLayerPolygonFilter
import com.arcgismaps.mapping.layers.SceneLayerPolygonFilterSpatialRelationship
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import kotlinx.coroutines.runBlocking

class FilterFeaturesInSceneViewModel(application: Application) : AndroidViewModel(application) {

    val sceneViewProxy = SceneViewProxy()

    // Create an OSM Buildings ArcGISSceneLayer from a portal item
    private val osmBuildingsSceneLayer = ArcGISSceneLayer(
        PortalItem(
            portal = Portal("https://www.arcgis.com"), itemId = "ca0470dbbddb4db28bad74ed39949e25"
        )
    )

    // Create a San Francisco Buildings ArcGISSceneLayer from a url
    private val sanFranciscoBuildingsSceneLayer =
        ArcGISSceneLayer(uri = "https://tiles.arcgis.com/tiles/z2tnIkrLQ2BRzr6P/arcgis/rest/services/SanFrancisco_Bldgs/SceneServer")

    // Create a new ArcGISScene and set a basemap from a portal item with a vector tile layer
    val arcGISScene: ArcGISScene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        // Add an elevation source to the scene's base surface.
        baseSurface.elevationSources.add(ArcGISTiledElevationSource(uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"))
        // OSM building layer
        operationalLayers.add(osmBuildingsSceneLayer)
        // Add the buildings scene layer to the operational layers
        operationalLayers.add(sanFranciscoBuildingsSceneLayer)
        // Set the initial viewpoint of the scene
        initialViewpoint = Viewpoint(
            Point(0.0, 0.0), Camera(
                latitude = 37.7041, longitude = -122.421, altitude = 207.0, heading = 60.0, pitch = 70.0, roll = 0.0
            )
        )
    })

    // Create a boundary polygon that will be populated with the extent of the San Francisco buildings layer once loaded
    private val sanFranciscoBuildingsBoundary = runBlocking { createBoundaryPolygon() }

    // Set up graphic overlay and graphic for the San Francisco buildings layer extent. It is assigned to the SceneView
    // in the composable function
    val graphicsOverlay = GraphicsOverlay().apply {
        graphics.add(
            Graphic(
                sanFranciscoBuildingsBoundary, SimpleFillSymbol(
                    style = SimpleFillSymbolStyle.Solid, color = Color.transparent, outline = SimpleLineSymbol(
                        style = SimpleLineSymbolStyle.Solid, color = Color.red, width = 5.0f
                    )
                )
            )
        )
    }

    /**
     * One the San Francisco buildings layer is loaded, create and return a boundary [Polygon].
     */
    private suspend fun createBoundaryPolygon(): Polygon? {
        // Load the San Francisco buildings layer
        sanFranciscoBuildingsSceneLayer.load().onSuccess {
            // Create a polygon boundary using the San Francisco buildings layer extent
            return sanFranciscoBuildingsSceneLayer.fullExtent?.let {
                PolygonBuilder().apply {
                    addPoint(it.xMin, it.yMin)
                    addPoint(it.xMax, it.yMin)
                    addPoint(it.xMax, it.yMax)
                    addPoint(it.xMin, it.yMax)
                }.toGeometry()
            }
        }
        // Return null if the layer doesn't load or the extent is not available
        return null
    }

    /**
     * Filter the OSM buildings layer to only show buildings that are disjoint from the San Francisco buildings layer
     * extent.
     */
    fun filterScene() {
        // Check that the San Francisco buildings layer extent is available
        sanFranciscoBuildingsBoundary?.let { boundary ->
            // Create a polygon filter with the San Francisco buildings layer boundary polygon and set it to be disjoint
            val sceneLayerPolygonFilter = SceneLayerPolygonFilter(listOf(boundary), SceneLayerPolygonFilterSpatialRelationship.Disjoint)
            // Set the polygon filter to the OSM buildings layer
            osmBuildingsSceneLayer.polygonFilter = sceneLayerPolygonFilter

        }
    }
}
