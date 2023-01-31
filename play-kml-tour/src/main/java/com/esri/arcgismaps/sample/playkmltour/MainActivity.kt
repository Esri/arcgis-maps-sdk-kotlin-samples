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

package com.esri.arcgismaps.sample.playkmltour

import android.os.Bundle
import android.util.Log
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
import com.arcgismaps.mapping.kml.KmlTourStatus
import com.arcgismaps.mapping.layers.KmlLayer
import com.esri.arcgismaps.sample.playkmltour.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt


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

    private val tourStatusTV by lazy {
        activityMainBinding.tourStatusTV
    }

    private val tourProgressBar by lazy {
        activityMainBinding.tourProgressBar
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

        // create a scene and set the surface
        sceneView.scene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
            baseSurface = surface
        }

        // add a KML layer from a KML dataset with a KML tour
        val kmlDataset = KmlDataset(provisionPath + getString(R.string.kml_tour_path))
        val kmlLayer = KmlLayer(kmlDataset)

        // add the layer to the scene view's operational layers
        sceneView.scene?.operationalLayers?.add(kmlLayer)

        // load the KML layer
        lifecycleScope.launch {
            kmlLayer.load().onFailure {
                showError(it.message.toString())
            }.onSuccess {
                // get the first loaded KML tour
                val kmlTour = findFirstKMLTour(kmlDataset.rootNodes)
                if (kmlTour == null) {
                    showError("Cannot find KML tour in dataset")
                    return@onSuccess
                }

                // collect changes in KML tour status
                collectKmlTourStatus(kmlTour)

                // set the KML tour to the controller
                kmlTourController.tour = kmlTour
            }
        }

        resetTourButton.setOnClickListener { kmlTourController.reset() }

        playPauseButton.setOnClickListener {
            // button was clicked when tour was playing
            if (kmlTourController.tour?.status?.value == KmlTourStatus.Playing)
            // pause KML tour
                kmlTourController.pause()
            else
            // play KML tour
                kmlTourController.play()
        }
    }

    /**
     * Recursively searches for the first KML tour in a list of [kmlNodes].
     * Returns the first [KmlTour], or null if there are no tours.
     */
    private fun findFirstKMLTour(kmlNodes: List<KmlNode>): KmlTour? {
        kmlNodes.forEach { node ->
            if (node is KmlTour)
                return node
            else if (node is KmlContainer)
                return findFirstKMLTour(node.childNodes)
        }
        return null
    }

    /**
     * Collects KmlTourStatus events from the [kmlTour] and then calls
     * showKmlTourStatus()
     */
    private fun collectKmlTourStatus(kmlTour: KmlTour) = lifecycleScope.launch {
        kmlTour.status.collect { kmlTourStatus ->
            when (kmlTourStatus) {
                KmlTourStatus.Completed -> {
                    showKmlTourStatus(
                        "Completed",
                        isResetEnabled = false,
                        isPlayingTour = false
                    )
                }
                KmlTourStatus.Initialized -> {
                    showKmlTourStatus(
                        "Initialized",
                        isResetEnabled = false,
                        isPlayingTour = false
                    )
                }
                KmlTourStatus.Paused -> {
                    showKmlTourStatus(
                        "Paused",
                        isResetEnabled = true,
                        isPlayingTour = false
                    )
                }
                KmlTourStatus.Playing -> {
                    showKmlTourStatus(
                        "Playing",
                        isResetEnabled = true,
                        isPlayingTour = true
                    )
                }
                else -> {}
            }
        }
    }

    /**
     * Displays the KML tour status using the [kmlTourStatus], display [resetTourButton]
     * if [isResetEnabled] and set [playPauseButton] based on [isPlayingTour].
     */
    private fun showKmlTourStatus(
        kmlTourStatus: String,
        isResetEnabled: Boolean,
        isPlayingTour: Boolean
    ) {
        // set the KML tour status
        tourStatusTV.text = String.format("Tour status: %s", kmlTourStatus)

        // enable the buttons
        resetTourButton.isEnabled = isResetEnabled
        playPauseButton.isEnabled = true

        // show pause button if true
        if (isPlayingTour) {
            playPauseButton.apply {
                // set button icon
                icon = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_round_pause_24
                )
                // set button text
                text = String.format("Pause")
            }
        } else { // show play button if false
            playPauseButton.apply {
                // set button icon
                icon = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_round_play_arrow_24
                )
                // set button text
                text = String.format("Play")
            }
        }

        // get progress of tour every second
        lifecycleScope.launch {
            // run as long as KML tour status is "Playing"
            while (kmlTourStatus == "Playing") {
                // get percentage of current position over total duration
                val tourProgressInt = ((kmlTourController.currentPosition.value * 100.0)
                        / (kmlTourController.totalDuration.value)).roundToInt()
                tourProgressBar.progress = tourProgressInt
                // set a second delay
                delay(1000)
            }
        }

    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(sceneView, message, Snackbar.LENGTH_SHORT).show()
    }
}
