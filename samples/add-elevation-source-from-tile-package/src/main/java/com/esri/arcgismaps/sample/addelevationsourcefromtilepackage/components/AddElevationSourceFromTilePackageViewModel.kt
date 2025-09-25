/* Copyright 2025 Esri
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

package com.esri.arcgismaps.sample.addelevationsourcefromtilepackage.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.addelevationsourcefromtilepackage.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

class AddElevationSourceFromTilePackageViewModel(app: Application) : AndroidViewModel(app) {
    // Message dialog view model to display errors
    val messageDialogVM = MessageDialogViewModel()

    // Base provision path for this sample's offline resources
    private val provisionPath: String by lazy {
        val basePath = app.getExternalFilesDir(null)?.path ?: ""
        basePath + File.separator + app.getString(R.string.add_elevation_source_from_tile_package_app_name)
    }

    // Camera location point (Monterey, CA)
    private val cameraLocation: Point by lazy {
        Point(
            x = -121.8,
            y = 36.525,
            z = 300.0,
            spatialReference = SpatialReference.wgs84()
        )
    }

    // Camera to view the scene
    private val camera: Camera by lazy {
        Camera(
            locationPoint = cameraLocation,
            heading = 180.0,
            pitch = 80.0,
            roll = 0.0
        )
    }

    // Create the ArcGISScene with imagery basemap
    val arcGISScene: ArcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Create a surface and add the local elevation source if found
        baseSurface = Surface().apply {
            val tilePackageFile = File(provisionPath, "MontereyElevation.tpkx")
            if (tilePackageFile.exists()) {
                elevationSources.add(ArcGISTiledElevationSource(tilePackageFile.path))
            } else {
                messageDialogVM.showMessageDialog(
                    title = "Elevation tile package not found",
                    description = "Expected file at:\n${tilePackageFile.path}"
                )
            }
        }
        // Set initial viewpoint using boundingGeometry with camera
        initialViewpoint = Viewpoint(
            boundingGeometry = cameraLocation,
            camera = camera
        )
    }

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { error ->
                messageDialogVM.showMessageDialog(
                    title = "Failed to load scene",
                    description = error.message.toString()
                )
            }
        }
    }
}
