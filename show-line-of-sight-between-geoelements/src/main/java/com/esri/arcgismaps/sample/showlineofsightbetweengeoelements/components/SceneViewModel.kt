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

package com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.analysis.GeoElementLineOfSight
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleRenderer
import com.arcgismaps.mapping.view.AnalysisOverlay
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.LayerSceneProperties
import com.arcgismaps.mapping.view.SurfacePlacement
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showlineofsightbetweengeoelements.R
import kotlinx.coroutines.launch
import java.io.File

class SceneViewModel(private var application: Application) : AndroidViewModel(application) {


    val analysisOverlay = AnalysisOverlay()

    // TODO: graphic
    val graphicsOverlay = GraphicsOverlay()

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()


    // TODO: SURFACE
    // add base surface for elevation data
    val elevationSource = ArcGISTiledElevationSource(R.string.elevation_service_url.toString())
    val surface = Surface().apply {
        elevationSources.add(elevationSource)
    }


    // TODO; BUILDING
    val buildingsURL = getString(application, R.string.new_york_buildings_service_url)
    val buildings: ArcGISSceneLayer = ArcGISSceneLayer(buildingsURL)


    // TODO: 3d
    val renderer3D = SimpleRenderer()
    val renderProperties = renderer3D.sceneProperties.apply {
        headingExpression.plus("[HEADING]")
    }

    // TODO: point
    val observationPoint = Point(-73.9853, 40.7484, 200.0, SpatialReference.wgs84())
    val observer = Graphic(
        geometry = observationPoint,
        symbol = SimpleMarkerSymbol(
            style = SimpleMarkerSymbolStyle.Circle,
            color = Color.fromRgba(255, 255, 0),
            size = 5F
        )
    )

    // TODO; WAYPOINTS
    // create waypoints around a block for the taxi to drive to
    val wayPoints = listOf(
        Point(-73.984513, 40.748469, SpatialReference.wgs84()),
        Point(-73.985068, 40.747786, SpatialReference.wgs84()),
        Point(-73.983452, 40.747091, SpatialReference.wgs84()),
        Point(-73.982961, 40.747762, SpatialReference.wgs84()),
    )

    // TODO: TAXI SYMBOL, FILEPATH, TAXIGRAPHIC


    val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.app_name
        )
    }

    val filePath = provisionPath + application.getString(R.string.new_york_buildings_service_url)

    val taxiSymbol = ModelSceneSymbol(filePath, 1.0F).apply {
        SceneSymbolAnchorPosition.Bottom
        viewModelScope.launch {
            load()
        }
    }


    val taxiGraphic = Graphic(wayPoints[0], taxiSymbol)
    // .attributes.put("HEADING", 0.0)

    val lineOfSight = GeoElementLineOfSight(
        observerGeoElement = observer,
        targetGeoElement = taxiGraphic
    )


    // TODO: CAMERA

    val cameraLocation = Point(
        x = -118.794,
        y = 33.909,
        z = 5330.0,
        spatialReference = SpatialReference.wgs84()
    )


    val camera = Camera(
        observer.geometry as Point,
        distance = 700.0,
        roll = -30.0,
        pitch = 45.0,
        heading = 0.0,
    )


    // create a base scene to be used to load the Taxi Computer-aided Design (CAD)
    var scene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISTopographic).apply {
        baseSurface = surface
        operationalLayers.add(buildings)
        initialViewpoint = Viewpoint(cameraLocation, camera)
    })


    init {


        graphicsOverlay.sceneProperties.surfacePlacement.apply {
            LayerSceneProperties(SurfacePlacement.Relative)
        }
        graphicsOverlay.renderer = renderer3D
        graphicsOverlay.graphics.add(observer)
        graphicsOverlay.graphics.add(taxiGraphic)
        analysisOverlay.analyses.add(lineOfSight)

        /*
    Instantiate an AnalysisOverlay and add it to the SceneView's analysis overlays collection.
Instantiate a GeoElementLineOfSight, passing in observer and target GeoElements (features or graphics). Add the line of sight to the analysis overlay's analyses collection.
To get the target visibility when it changes, react to the target visibility changing on the GeoElementLineOfSight instance.
     */
    }
}
