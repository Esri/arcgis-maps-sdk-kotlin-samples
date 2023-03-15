/*
 * Copyright 2023 Esri
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
package com.esri.arcgismaps.sample.changecameracontroller

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.ModelSceneSymbol
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.OrbitLocationCameraController
import com.arcgismaps.mapping.view.OrbitGeoElementCameraController
import com.arcgismaps.mapping.view.GlobeCameraController
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.changecameracontroller.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val sceneView by lazy {
        activityMainBinding.sceneView
    }

    // custom toolbar to show camera controller options menu
    private val toolbar by lazy {
        activityMainBinding.toolbar.apply {
            // set the overflow icon to the camera icon
            overflowIcon = ContextCompat.getDrawable(context, R.drawable.ic_camera)
        }
    }

    // graphics overlay for the scene to draw the 3d graphics on
    private val graphicsOverlay by lazy {
        GraphicsOverlay().apply {
            // set the altitude values in the scene to be absolute
            sceneProperties.surfacePlacement = SurfacePlacement.Absolute
        }
    }

    // list of available asset files
    private val assetFiles by lazy {
        resources.getStringArray(R.array.asset_files).toList()
    }

    // the graphic representing the airplane 3d model
    private val airplane3DGraphic by lazy {
        // location for the target graphic
        val point = Point(-109.937516, 38.456714, 5000.0, SpatialReference.wgs84())
        // create the graphic with the target location
        Graphic(point)
    }

    // camera controller which orbits a target location
    private val orbitLocationCameraController by lazy {
        // target location for the camera controller
        val point = Point(-109.929589, 38.437304, 1700.0, SpatialReference.wgs84())
        // instantiate a new camera controller with a distance from the target
        OrbitLocationCameraController(point, 5000.0).apply {
            // set a relative pitch to the target
            setCameraPitchOffset(3.0)
            // set a relative heading to the target
            setCameraHeadingOffset(150.0)
        }
    }

    // camera controller which orbits the plane graphic
    private val orbitPlaneCameraController by lazy {
        // instantiate a new camera controller with a distance from airplane graphic
        OrbitGeoElementCameraController(airplane3DGraphic, 100.0).apply {
            // set a relative pitch to the target
            setCameraPitchOffset(3.0)
            // set a relative heading to the target
            setCameraHeadingOffset(150.0)
        }
    }

    // camera controller for free roam navigation
    private val globeCameraController = GlobeCameraController()

    // camera looking at the Upheaval Dome crater in Utah
    private val defaultCamera = Camera(
        38.459291,
        -109.937576,
        5500.0,
        150.0,
        20.0,
        0.0
    )

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // inflate the camera controller options menu
        menuInflater.inflate(R.menu.camera_controller_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // select the camera controller based on the menu option item clicked
        sceneView.cameraController = when (item.itemId) {
            R.id.action_camera_controller_plane -> orbitPlaneCameraController
            R.id.action_camera_controller_crater -> orbitLocationCameraController
            R.id.action_camera_controller_globe -> globeCameraController
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set the custom toolbar for the activity
        setSupportActionBar(toolbar)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(sceneView)

        // create and add a scene with an imagery basemap style
        val scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            // add an elevation data source to the base surface
            baseSurface.elevationSources.add(
                ArcGISTiledElevationSource(getString(R.string.elevation_service_url))
            )
        }

        // add the airplane 3d graphic to the graphics overlay
        graphicsOverlay.graphics.add(airplane3DGraphic)

        sceneView.apply {
            // set the scene to the SceneView
            this.scene = scene
            // add the graphics overlay to the SceneView
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            // if the map load failed show an error and return
            scene.load().onFailure {
                showError("Failed to load the scene: ${it.message}")
                return@launch
            }
            // set the sceneView viewpoint to the default camera
            sceneView.setViewpointCamera(defaultCamera)
            // copy assets to the cache directory if needed
            copyAssetsToCache(assetFiles, cacheDir, false)
            // load the airplane model file and update the the airplane3DGraphic
            loadModel(getString(R.string.bristol_model_file), airplane3DGraphic)
        }
    }

    /**
     * Copies the list of [assets] files from the assets folder to a given [cache] directory. This
     * suspending function runs on the [Dispatchers.IO] context. If [overwrite] is true, any assets
     * already in the [cache] directory are overwritten, otherwise copy is skipped
     */
    private suspend fun copyAssetsToCache(
        assets: List<String>,
        cache: File,
        overwrite: Boolean
    ) = withContext(Dispatchers.IO) {
        // get the AssetManager
        val assetManager = applicationContext.assets ?: return@withContext
        assets.forEach { assetName ->
            // check for cancellation before reading/writing the asset files
            ensureActive()
            try {
                // create an output file in the cache directory
                val outFile = File(cache, assetName)
                // if the output file doesn't exist or overwrite is enabled
                if (!outFile.exists() || overwrite) {
                    // create an input stream to the asset
                    assetManager.open(assetName).use { inputStream ->
                        // create an file output stream to the output file
                        FileOutputStream(outFile).use { outputStream ->
                            // copy the input file stream to the output file stream
                            inputStream.copyTo(outputStream)
                        }
                        Log.i(TAG, "$assetName copied to cache.")
                    }
                } else {
                    Log.i(TAG, "$assetName already in cache, skipping copy.")
                }
            } catch (exception: Exception) {
                showError("Error caching asset :${exception.message}")
            }
        }
    }

    /**
     * Loads a [ModelSceneSymbol] from the [filename] in the [getCacheDir] and updates the [graphic]
     */
    private suspend fun loadModel(filename: String, graphic: Graphic) {
        val modelFilePath = File(cacheDir, filename)
        if (modelFilePath.exists()) {
            // create a new ModelSceneSymbol with the file
            val modelSceneSymbol = ModelSceneSymbol(modelFilePath.absolutePath).apply {
                heading = 45f
            }
            // if the symbol load failed show and error and return
            modelSceneSymbol.load().onFailure {
                showError("Error loading airplane model: ${it.message}")
                return
            }
            // update the graphic's symbol
            graphic.symbol = modelSceneSymbol
        } else {
            showError("Error loading airplane model: file does not exist.")
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(sceneView, message, Snackbar.LENGTH_SHORT).show()
    }
}
