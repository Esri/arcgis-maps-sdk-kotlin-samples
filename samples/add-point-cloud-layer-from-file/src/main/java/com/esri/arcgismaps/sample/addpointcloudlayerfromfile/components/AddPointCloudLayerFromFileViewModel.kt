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

package com.esri.arcgismaps.sample.addpointcloudlayerfromfile.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.PointCloudLayer
import com.arcgismaps.mapping.view.Camera
import com.esri.arcgismaps.sample.addpointcloudlayerfromfile.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the Add point cloud layer from file sample.
 */
class AddPointCloudLayerFromFileViewModel(private val app: Application) : AndroidViewModel(app) {

    // The scene displayed in the SceneView composable
    val arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Set an initial camera viewpoint focused on the point cloud area.
        val camera = Camera(
            latitude = 32.720195,
            longitude = -117.155593,
            altitude = 1050.0,
            heading = 23.0,
            pitch = 70.0,
            roll = 0.0
        )
        initialViewpoint = Viewpoint(camera = camera, boundingGeometry = camera.location)
    }

    // Folder path for the sample provisioned directory
    private val provisionPath: String by lazy {
        app.getExternalFilesDir(null)?.path + File.separator + app.getString(R.string.add_point_cloud_layer_from_file_app_name)
    }

    // Name of the local slpk file provisioned on device
    private val slpkFileName = "sandiego-north-balboa-pointcloud.slpk"

    // File path to the local slpk
    private val slpkFilePath: String
        get() = File(provisionPath, slpkFileName).path

    // World 3D elevation service
    private val elevationSource = ArcGISTiledElevationSource(
        uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    )

    // Message dialog helper to show load errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        // Create the point cloud layer from the local SLPK file. The file must exist at the provision path.
        val slpkFile = File(slpkFilePath)
        if (!slpkFile.exists()) {
            messageDialogVM.showMessageDialog(
                title = "Point cloud file not found",
                description = "Expected .slpk at: $slpkFilePath."
            )
        } else {
            viewModelScope.launch {
                // Add an elevation source so point cloud will be draped properly on the surface
                arcGISScene.baseSurface.elevationSources.add(elevationSource)

                // Add the point cloud layer to the scene's operational layers
                arcGISScene.operationalLayers.add(PointCloudLayer(uri = slpkFile.path))

                // Load the scene and the point cloud layer
                arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
            }
        }
    }
}
