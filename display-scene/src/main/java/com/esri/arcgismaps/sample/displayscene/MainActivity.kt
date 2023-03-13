/* Copyright 2022 Esri
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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.displayscene.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val sceneView by lazy {
        activityMainBinding.sceneView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(sceneView)

        // create an elevation source, and add this to the base surface of the scene
        val elevationSource = ArcGISTiledElevationSource(
            resources.getString(R.string.elevation_image_service)
        )

        // create a scene with a imagery basemap style
        val imageryScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // add the elevation source to the base surface
            baseSurface.elevationSources.add(elevationSource)
        }

        // add a camera and initial camera position
        val camera = Camera(
            latitude = 28.4,
            longitude = 83.9,
            altitude = 10010.0,
            heading = 10.0,
            pitch = 80.0,
            roll = 0.0
        )
        sceneView.apply {
            scene = imageryScene
            setViewpointCamera(camera)
        }
    }
}
