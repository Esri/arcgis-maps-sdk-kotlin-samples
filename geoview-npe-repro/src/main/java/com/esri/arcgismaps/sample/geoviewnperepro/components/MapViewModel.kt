/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.geoviewnperepro.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val viewpointAmerica = Viewpoint(39.8, -98.6, 10e7)
    var viewpoint =  mutableStateOf(viewpointAmerica)
    val mapViewProxy = MapViewProxy()
    val geometryEditor = GeometryEditor()

    val point = Graphic(
        geometry = Point(-90.0, 45.0, SpatialReference.wgs84()),
        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Color.red, 8.0f)
    )

    val multipoint = Graphic(
        geometry = Multipoint(
            points = listOf(
                Point(-90.0, 40.0, SpatialReference.wgs84()),
                Point(-95.0, 40.0, SpatialReference.wgs84()),
                Point(-100.0, 40.0, SpatialReference.wgs84()),
                Point(-105.0, 40.0, SpatialReference.wgs84()),
                Point(-110.0, 40.0, SpatialReference.wgs84()),
                Point(-115.0, 40.0, SpatialReference.wgs84()),
                ),
            spatialReference = SpatialReference.wgs84()
        ),
        symbol = SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Diamond, Color.cyan, 9.0f)
    )

    val polygon = Graphic(
        geometry = Polygon(
            points = listOf(
                Point(-90.0, 35.0, SpatialReference.wgs84()),
                Point(-80.0, 35.0, SpatialReference.wgs84()),
                Point(-80.0, 30.0, SpatialReference.wgs84()),
                Point(-90.0, 30.0, SpatialReference.wgs84()),
                ),
            spatialReference = SpatialReference.wgs84()
        ),
        symbol = SimpleLineSymbol(
            style = SimpleLineSymbolStyle.Solid,
            color = Color.green,
            width = 5.0f)
    )

    val staticGraphicsOverlay = GraphicsOverlay(listOf(point, multipoint, polygon))
    val geometryEditorGraphicsOverlay = GraphicsOverlay()
}