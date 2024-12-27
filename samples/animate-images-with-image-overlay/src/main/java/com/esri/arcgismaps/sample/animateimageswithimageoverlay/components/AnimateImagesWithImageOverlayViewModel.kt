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

package com.esri.arcgismaps.sample.animateimageswithimageoverlay.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.ImageFrame
import com.arcgismaps.mapping.view.ImageOverlay
import com.esri.arcgismaps.sample.animateimageswithimageoverlay.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class AnimateImagesWithImageOverlayViewModel(application: Application) : AndroidViewModel(application) {

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.animate_images_with_image_overlay_app_name
        )
    }

    // Get the folder path containing all the image overlays
    private val filePath = "$provisionPath/PacificSouthWest"

    var opacity by mutableFloatStateOf(1.0f)
        private set

    fun updateOpacity(opacityFromSlider: Float) {
        opacity = opacityFromSlider
        imageOverlay.opacity = opacity
    }

    val fpsOptions = listOf(60, 30, 15)
    private val _fps = MutableStateFlow(fpsOptions[1])
    val fps = _fps.asStateFlow()

    fun updateFpsOption(fpsIndex: Int) {
        _fps.value = fpsOptions[fpsIndex]
    }

    var isStarted by mutableStateOf(true)
        private set

    fun updateIsStarted(isStartedFromButton: Boolean) {
        isStarted = isStartedFromButton
        toggleAnimationTimer()
    }


    // Create an envelope of the pacific southwest sector for displaying the image frame
    private val pointForImageFrame = Point(
        x = -120.0724,
        y = 35.1310,
        spatialReference = SpatialReference.wgs84()
    )
    private val pacificSouthwestEnvelope = Envelope(
        center = pointForImageFrame,
        width = 15.0958,
        height = -14.3770
    )

    // Create a scene with the dark gray basemap and elevation source
    val arcGISScene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISDarkGray).apply {
        // Create a camera, looking at the pacific southwest sector
        val observationPoint = Point(-116.621, 24.7773, 856977.0)
        val camera = Camera(observationPoint, 353.994, 48.5495, 0.0)
        initialViewpoint = Viewpoint(pacificSouthwestEnvelope, camera)
    })

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Keep track of the list of image frames added in cache
    private var imageFrames = mutableListOf<ImageFrame>()

    // Keep track of the image frame currently in view
    private var imageFrameIndex = 0

    // Timer task to customize frame rates
    private var timer: Timer? = null

    var imageOverlay = ImageOverlay()
        private set

    init {

        // Get the image files from local storage
        (File(filePath).listFiles())?.sorted()?.forEach { imageFile ->
            // Create an image with the given path and use it to create an image frame
            val imageFrame = ImageFrame(imageFile.path, pacificSouthwestEnvelope)
            imageFrames.add(imageFrame)
        }
        // Set the initial image frame to image overlay
        imageOverlay.imageFrame = imageFrames[imageFrameIndex]

        viewModelScope.launch {
            arcGISScene.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    "Failed to load map",
                    error.message.toString()
                )
            }

            // On changes to the fps, create a new timer
            _fps.collect {
                if (isStarted) {
                    createNewTimer()
                }
            }
        }

        // Start the animation timer
        createNewTimer()
    }

    /**
     * Create a new image frame from the image at the current index and add it to the image overlay.
     */
    private fun addNextImageFrameToImageOverlay() {
        // Set image frame to image overlay
        imageOverlay.imageFrame = imageFrames[imageFrameIndex]
        // Increment the index to keep track of which image to load next
        imageFrameIndex++
        // Reset index once all files have been loaded
        if (imageFrameIndex == imageFrames.size)
            imageFrameIndex = 0
    }

    /**
     * Create a new timer for the given period which repeatedly calls [addNextImageFrameToImageOverlay]..
     */
    private fun createNewTimer() {
        // Get the current period from the fps state flow
        val period = when (_fps.value) {
            60 -> 17 // 1000ms/17 = 60 fps
            30 -> 33 // 1000ms/33 = 30 fps
            15 -> 67 // 1000ms/67 = 15 fps
            else -> 0
        }
        // Cancel any timers that might be running
        timer?.cancel()
        timer = null
        // Create a new timer with the given period
        timer = fixedRateTimer("Image overlay timer", period = period.toLong()) {
            addNextImageFrameToImageOverlay()
        }
    }

    /**
     * Toggles starting and stopping the timer on button tap.
     */
    private fun toggleAnimationTimer() {
        timer?.let {
            // Cancel any running timer
            timer?.cancel()
            timer = null
            // Change the start/stop button to "start"
            isStarted = false
        } ?: run {
            createNewTimer()
            // Change the start/stop button to "stop"
            isStarted = true
        }
    }
}
