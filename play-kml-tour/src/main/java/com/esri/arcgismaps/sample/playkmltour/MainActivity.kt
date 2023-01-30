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

package com.esri.arcgismaps.sample.playkmltour

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.kml.KmlContainer
import com.arcgismaps.mapping.kml.KmlDataset
import com.arcgismaps.mapping.kml.KmlNode
import com.arcgismaps.mapping.kml.KmlTour
import com.arcgismaps.mapping.kml.KmlTourController
import com.arcgismaps.mapping.layers.KmlLayer
import com.esri.arcgismaps.sample.playkmltour.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val provisionPath: String by lazy {
        getExternalFilesDir(null)?.path.toString() + File.separator + getString(R.string.app_name)
    }

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val sceneView by lazy {
        activityMainBinding.sceneView
    }

    private val playPauseButton by lazy {
        activityMainBinding.playPauseButton
    }

    private val resetTourButton by lazy {
        activityMainBinding.resetTourButton
    }

    private val kmlTourController = KmlTourController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(sceneView)


        // add elevation data
        val surface = Surface().apply {
            elevationSources.add(ArcGISTiledElevationSource(getString(R.string.world_terrain_service)))
        }

        // create a scene
        sceneView.scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface = surface
        }

        // add a KML layer from a KML dataset with a KML tour
        val kmlDataset = KmlDataset(provisionPath + getString(R.string.kml_tour_path))
        val kmlLayer = KmlLayer(kmlDataset)
        sceneView.scene?.operationalLayers?.add(kmlLayer)

        lifecycleScope.launch {
            kmlLayer.load().onFailure {
                showError(it.message.toString())
            }.onSuccess {
                val kmlTour = findFirstKMLTour(kmlDataset.rootNodes)
                if(kmlTour == null){
                    showError("Cannot find KML tour in dataset")
                    return@onSuccess
                }

                kmlTourController.tour = kmlTour
                playPauseButton.isEnabled = true
                resetTourButton.isEnabled = true
            }
        }

        resetTourButton.setOnClickListener {
            kmlTourController.reset()
        }

        playPauseButton.setOnClickListener {
            // button was clicked when tour was playing
            if (kmlTourController.tour?.isVisible == true) {
                // pause tour and update button
                kmlTourController.pause()
                playPauseButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_round_play_arrow_24)
            }
            else {
                // play tour and update button
                kmlTourController.play()
                playPauseButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_round_pause_24)
            }
        }

        resetTourButton.setOnClickListener {

        }
    }

    private fun findFirstKMLTour(kmlNodes: List<KmlNode>): KmlTour? {
        kmlNodes.forEach { node ->
            if(node is KmlTour)
                return node
            else if (node is KmlContainer)
                return findFirstKMLTour(node.childNodes)
        }
        return null
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(sceneView, message, Snackbar.LENGTH_SHORT).show()
    }
}
