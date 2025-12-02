/* Copyright 2023 Esri
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

package com.esri.arcgismaps.sample.displayscene

import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.ArcGISSceneLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SceneViewingMode
import com.esri.arcgismaps.sample.displayscene.databinding.DisplaySceneActivityMainBinding
import com.esri.arcgismaps.sample.sampleslib.EdgeToEdgeCompatActivity
import kotlinx.coroutines.launch

class MainActivity : EdgeToEdgeCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: DisplaySceneActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.display_scene_activity_main)
    }

    private val localSceneView by lazy {
        activityMainBinding.sceneView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)
        lifecycle.addObserver(localSceneView)


        // create an elevation source, and add this to the base surface of the scene
        val elevationSource = ArcGISTiledElevationSource(
       "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
        )

        val buildingSceneLayer = ArcGISSceneLayer("https://www.arcgis.com/home/item.html?id=61da8dc1a7bc4eea901c20ffb3f8b7af")

        // Add the airports point scene layer
        val pointSceneLayer = ArcGISSceneLayer(
            uri = "https://tiles.arcgis.com/tiles/V6ZHFr6zdgNZuVG0/arcgis/rest/services/Airports_PointSceneLayer/SceneServer/layers/0"

        )

        val overtureLabelPointSceneLayer = ArcGISSceneLayer(
            uri = "https://esri.mapsdevext.arcgis.com/home/item.html?id=e28731701c7147a7bd2fb296cee1b8c9"
        )

        val osmLabelPointSceneLayer = ArcGISSceneLayer(
            uri = "https://www.arcgis.com/home/item.html?id=a84404ad39c64c328d0596e361ec459b"
        )
        lifecycleScope.launch {
            osmLabelPointSceneLayer.load().onSuccess {
                Log.i("SceneLayer", "OSM Label Point Scene Layer loaded successfully.")
            }.onFailure {
                Log.e("SceneLayer", "Failed to load OSM Label Point Scene Layer: ${it.message}")
            }
        }

        val localScene =
            ArcGISScene(basemapStyle = BasemapStyle.ArcGISTopographic, viewingMode = SceneViewingMode.Local).apply {
                // add the elevation source to the base surface
                baseSurface.elevationSources.add(elevationSource)
                operationalLayers.add(buildingSceneLayer)
                operationalLayers.add(pointSceneLayer)
                operationalLayers.add(overtureLabelPointSceneLayer)
                operationalLayers.add(osmLabelPointSceneLayer)
                clippingArea = Envelope(
                    xMin = 19454578.8235,
                    yMin = -5055381.4798,
                    xMax = 19455518.8814,
                    yMax = -5054888.4150,
                    spatialReference = SpatialReference.webMercator()
                )
                //isClippingEnabled = true
            }

        // Set the scene's initial viewpoint.
        val camera = Camera(
            locationPoint = Point(
                x = 19455578.6821,
                y = -5056336.2227,
                z = 1699.3366,
                spatialReference = SpatialReference.webMercator(),
            ),
            heading = 338.7410,
            pitch = 40.3763,
            roll = 0.0
        )

        localSceneView.apply {
            scene = localScene
            setViewpointCamera(camera)
        }
    }
}
