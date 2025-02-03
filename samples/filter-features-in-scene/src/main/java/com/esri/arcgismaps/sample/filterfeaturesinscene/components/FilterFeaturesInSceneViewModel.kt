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
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
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
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class FilterFeaturesInSceneViewModel(application: Application) : AndroidViewModel(application) {

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

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
        val tiledElevationSource =
            ArcGISTiledElevationSource(uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
        baseSurface.elevationSources.add(tiledElevationSource)
        // OSM building layer
        operationalLayers.add(osmBuildingsSceneLayer)
        // Add the buildings scene layer to the operational layers
        operationalLayers.add(sanFranciscoBuildingsSceneLayer)
        // Set the initial viewpoint of the scene
        initialViewpoint = Viewpoint(
            latitude = 37.7041,
            longitude = -122.421,
            1000.0,
            Camera(
                latitude = 37.7041,
                longitude = -122.421,
                altitude = 207.0,
                heading = 60.0,
                pitch = 70.0,
                roll = 0.0
            )
        )
    })

    // Define a red boundary graphic
    private val boundaryGraphic = Graphic(
        symbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.Solid,
            color = Color.transparent,
            outline = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Solid,
                color = Color.red,
                width = 5.0f
            )
        )
    )

    // Set up graphic overlay and graphic for the San Francisco buildings layer extent.
    val graphicsOverlay = GraphicsOverlay(listOf(boundaryGraphic))

    init {
        loadBuildingsLayer()
    }

    /**
     * Load the San Francisco buildings layer and create a polygon boundary using the layer extent.
     */
    private fun loadBuildingsLayer() {
        viewModelScope.launch {
            // Load the San Francisco buildings layer
            sanFranciscoBuildingsSceneLayer.load().onFailure {
                messageDialogVM.showMessageDialog(it.message.toString(), it.cause.toString())
            }
            // Create a polygon boundary using the San Francisco buildings layer extent
            sanFranciscoBuildingsSceneLayer.fullExtent?.let {
                boundaryGraphic.geometry = PolygonBuilder().apply {
                    addPoint(it.xMin, it.yMin)
                    addPoint(it.xMax, it.yMin)
                    addPoint(it.xMax, it.yMax)
                    addPoint(it.xMin, it.yMax)
                }.toGeometry()
            }
        }
    }

    /**
     * Filter the OSM buildings layer to only show buildings that are disjoint from the San Francisco buildings layer
     * extent.
     */
    fun filterScene() {
        // Check that the San Francisco buildings layer extent is available
        (boundaryGraphic.geometry as? Polygon)?.let { boundary ->
            // Create a polygon filter with the San Francisco buildings layer boundary polygon and set it to be disjoint
            val sceneLayerPolygonFilter =
                SceneLayerPolygonFilter(
                    polygons = listOf(boundary),
                    spatialRelationship = SceneLayerPolygonFilterSpatialRelationship.Disjoint
                )
            // Set the polygon filter to the OSM buildings layer
            osmBuildingsSceneLayer.polygonFilter = sceneLayerPolygonFilter
        }
    }

    /**
     * Reset the OSM buildings layer filter to show all buildings.
     */
    fun resetFilter() {
        // Clear all polygon filters
        osmBuildingsSceneLayer.polygonFilter?.polygons?.clear()
    }
}
