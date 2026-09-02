/* Copyright 2026 Esri
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

package com.esri.arcgismaps.sample.displayscene.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.esri.arcgismaps.sample.displayscene.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

class DisplaySceneViewModel(private val app: Application) : AndroidViewModel(app) {
    //Initial Camera Location
    val cameraLocation = Point(
        x = -118.794,
        y = 33.909,
        z = 5330.0,
        spatialReference = SpatialReference.wgs84()
    )

    //Initial camera object
    val camera = Camera(
        locationPoint = cameraLocation,
        heading = 355.0,
        pitch = 72.0,
        roll = 0.0
    )
    // Create a scene to be present on scene view
    val imageryScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Add base surface for elevation data
        val elevationSource =
            ArcGISTiledElevationSource(uri = application.getString(R.string.elevation_image_service))
        val surface = Surface().apply {
            elevationSources.add(elevationSource)
            // Add an exaggeration factor to increase the 3D effect of the elevation.
            elevationExaggeration = 2.5f
        }
        baseSurface = surface
        initialViewpoint = Viewpoint(boundingGeometry = cameraLocation, camera = camera)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Load the map and handle any errors by showing a message dialog
        viewModelScope.launch {
            imageryScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }
}
