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

package com.esri.arcgismaps.sample.changecameracontroller

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
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
import com.arcgismaps.mapping.view.*
import com.esri.arcgismaps.sample.changecameracontroller.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val toolbar by lazy {
        activityMainBinding.toolbar.apply {
            overflowIcon = ContextCompat.getDrawable(context, R.drawable.ic_camera)
        }
    }

    private val sceneView by lazy {
        activityMainBinding.sceneView
    }

    private val graphicsOverlay by lazy {
        GraphicsOverlay().apply {
            sceneProperties.surfacePlacement = SurfacePlacement.Absolute
        }
    }

    private val assetFiles by lazy {
        resources.getStringArray(R.array.asset_files).toList()
    }

    private val plane3DGraphic by lazy {
        val point = Point(-109.937516, 38.456714, 5000.0, SpatialReference.wgs84())
        Graphic(point)
    }

    private val orbitLocationCameraController by lazy {
        val point = Point(-109.929589, 38.437304, 1700.0, SpatialReference.wgs84())
        OrbitLocationCameraController(point, 5000.0).apply {
            setCameraPitchOffset(3.0)
            setCameraHeadingOffset(150.0)
        }
    }

    private val orbitPlaneCameraController by lazy {
        OrbitGeoElementCameraController(plane3DGraphic, 100.0).apply {
            setCameraPitchOffset(3.0)
            setCameraHeadingOffset(150.0)
        }
    }

    private val globeCameraController = GlobeCameraController()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.camera_controller_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

        setSupportActionBar(toolbar)
        invalidateOptionsMenu()

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(sceneView)

        // create and add a scene with a imagery basemap style
        val scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface.elevationSources.add(
                ArcGISTiledElevationSource(getString(R.string.elevation_service_url))
            )
        }

        graphicsOverlay.graphics.add(plane3DGraphic)

        sceneView.apply {
            this.scene = scene
            graphicsOverlays.add(graphicsOverlay)
        }

        lifecycleScope.launch {
            scene.load().onFailure {
                showError("Failed to load the scene: ${it.message}")
                return@launch
            }

            val camera = Camera(
                38.459291,
                -109.937576,
                5500.0,
                150.0,
                20.0,
                0.0
            )

            sceneView.setViewpointCamera(camera)

            copyAssetsToCache(assetFiles, cacheDir)
            loadModel()
        }
    }

    private fun loadModel() {
        val modelFilePath =
            cacheDir.absolutePath + File.separator + getString(R.string.bristol_model_file)
        val plane3DSymbol = ModelSceneSymbol(modelFilePath).apply {
            heading = 45f
        }
        plane3DGraphic.symbol = plane3DSymbol
    }

    private suspend fun copyAssetsToCache(assets: List<String>, cache: File) =
        withContext(Dispatchers.IO) {
            val assetManager = applicationContext.assets ?: return@withContext
            assets.forEach { assetName ->
                val file = File(cache, assetName)
                if (!file.exists()) {
                    try {
                        assetManager.open(assetName).use { inputStream ->
                            FileOutputStream(file).use { outputStream ->
                                val buffer = ByteArray(1024)
                                var read: Int
                                while (inputStream.read(buffer, 0, 1024)
                                        .also { bytesTransferred -> read = bytesTransferred } >= 0
                                ) {
                                    outputStream.write(buffer, 0, read)
                                }
                            }
                        }
                    } catch (exception: IOException) {
                        showError("Error caching asset :${exception.message}")
                    }
                } else {
                    Log.i(TAG, "$assets already in cache, skipping copy.")
                }
            }
        }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(sceneView, message, Snackbar.LENGTH_SHORT).show()
    }
}
