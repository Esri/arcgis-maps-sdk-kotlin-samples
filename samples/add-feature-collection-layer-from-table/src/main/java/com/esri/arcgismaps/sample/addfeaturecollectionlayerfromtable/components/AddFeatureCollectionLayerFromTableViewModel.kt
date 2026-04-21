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

package com.esri.arcgismaps.sample.addfeaturecollectionlayerfromtable.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.data.FeatureCollection
import com.arcgismaps.data.FeatureCollectionTable
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureCollectionLayer
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel which creates three in-memory FeatureCollectionTables (point, polyline, polygon),
 * adds features to them, creates a FeatureCollection and displays it on the map as a
 * FeatureCollectionLayer.
 */
class AddFeatureCollectionLayerFromTableViewModel(app: Application) : AndroidViewModel(app) {

    // ArcGIS map exposed for Compose screen.
    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISOceans).apply {
        initialViewpoint = Viewpoint(latitude = 8.849289, longitude = -79.497238, scale = 1e6)
    }

    // Message dialog view model for error handling
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and then build the feature collection layer
        viewModelScope.launch {
            createAndAddFeatureCollectionLayer()
            arcGISMap.load()
                .onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    private suspend fun createAndAddFeatureCollectionLayer() {

        // Create each feature collection table
        val pointsTable = createPointsCollectionTable()
        val linesTable = createLinesCollectionTable()
        val polygonsTable = createPolygonsCollectionTable()

        // Create a FeatureCollection from the tables
        val featureCollection = FeatureCollection(featureCollectionTables = listOf(pointsTable, linesTable, polygonsTable))

        // Create a layer from the feature collection and add it to the map
        val featureCollectionLayer = FeatureCollectionLayer(featureCollection = featureCollection)
        arcGISMap.operationalLayers.add(featureCollectionLayer)
    }

    /**
     * Create an in-memory points [FeatureCollectionTable], define a simple schema and renderer,
     * add a single point feature and return the table.
     */
    private suspend fun createPointsCollectionTable(): FeatureCollectionTable {

        // Define a simple text field for a place name
        val placeField = Field(
            fieldType = FieldType.Text,
            name = "Place",
            alias = "Place name",
            length = 40,
            isEditable = true,
            isNullable = false
        )

        // Create the feature collection table for points using WGS84 spatial reference
        val pointsCollectionTable = FeatureCollectionTable(
            fields = listOf(placeField),
            geometryType = GeometryType.Point,
            spatialReference = SpatialReference.wgs84()
        )

        // Set a simple renderer using a triangle marker symbol
        pointsCollectionTable.renderer = SimpleRenderer(
            symbol = SimpleMarkerSymbol(
                style = SimpleMarkerSymbolStyle.Triangle,
                color = Color.red,
                size = 18f
            )
        )

        // Create a point geometry (x = longitude, y = latitude) and a feature with attributes
        val pointGeometry = Point(x = -79.497238, y = 8.849289, spatialReference = SpatialReference.wgs84())
        val placeAttributes = mapOf("Place" to "Current location")

        // Create and add the feature to the table
        val pointFeature = pointsCollectionTable.createFeature(placeAttributes, pointGeometry)
        pointsCollectionTable.addFeature(pointFeature).onFailure { messageDialogVM.showMessageDialog(it) }

        return pointsCollectionTable
    }

    /**
     * Create an in-memory polyline FeatureCollectionTable with a dash renderer and a single line
     * feature between two points.
     */
    private suspend fun createLinesCollectionTable(): FeatureCollectionTable {
        val boundaryField = Field(
            fieldType = FieldType.Text,
            name = "Boundary",
            alias = "Boundary name",
            length = 40,
            isEditable = true,
            isNullable = false
        )

        val linesCollectionTable = FeatureCollectionTable(
            fields = listOf(boundaryField),
            geometryType = GeometryType.Polyline,
            spatialReference = SpatialReference.wgs84()
        )

        linesCollectionTable.renderer = SimpleRenderer(
            symbol = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Dash,
                color = Color.green,
                width = 3f
            )
        )

        val lineGeometry = Polyline(
            points = listOf(
                Point(x = -79.497238, y = 8.849289, spatialReference = SpatialReference.wgs84()),
                Point(x = -80.035568, y = 9.432302, spatialReference = SpatialReference.wgs84())
            )
        )

        val lineAttributes = mapOf("Boundary" to "AManAPlanACanalPanama")
        val lineFeature = linesCollectionTable.createFeature(lineAttributes, lineGeometry)
        linesCollectionTable.addFeature(lineFeature).onFailure { messageDialogVM.showMessageDialog(it) }

        return linesCollectionTable
    }

    /**
     * Create an in-memory polygon and add it to the FeatureCollectionTable as a single polygon feature.
     * A simple fill renderer is added to the FeatureCollectionTable to render the polygon.
     */
    private suspend fun createPolygonsCollectionTable(): FeatureCollectionTable {
        val areaField = Field(
            fieldType = FieldType.Text,
            name = "Area",
            alias = "Area name",
            length = 40,
            isEditable = true,
            isNullable = false
        )

        val polygonsCollectionTable = FeatureCollectionTable(
            fields = listOf(areaField),
            geometryType = GeometryType.Polygon,
            spatialReference = SpatialReference.wgs84()
        )

        // Create a simple fill symbol using a blue outline and cyan diagonal cross fill
        val outlineSymbol = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.blue,
            width = 2f
        )
        val fillSymbol = SimpleFillSymbol(
            style = SimpleFillSymbolStyle.DiagonalCross,
            color = Color.cyan,
            outline = outlineSymbol
        )
        polygonsCollectionTable.renderer = SimpleRenderer(symbol = fillSymbol)

        val polygonGeometry = Polygon(points = listOf(
            Point(x = -79.497238, y = 8.849289, spatialReference = SpatialReference.wgs84()),
            Point(x = -79.337936, y = 8.638903, spatialReference = SpatialReference.wgs84()),
            Point(x = -79.11409, y = 8.895422, spatialReference = SpatialReference.wgs84())
        ))

        val polygonAttributes = mapOf("Area" to "Restricted area")
        val polygonFeature = polygonsCollectionTable.createFeature(polygonAttributes, polygonGeometry)
        polygonsCollectionTable.addFeature(polygonFeature).onFailure { messageDialogVM.showMessageDialog(it) }

        return polygonsCollectionTable
    }
}
