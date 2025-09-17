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

package com.esri.arcgismaps.sample.addintegratedmeshlayer.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Surface
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.IntegratedMeshLayer
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.DrawStatus
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the AddIntegratedMeshLayer sample.
 *
 * This ViewModel constructs an ArcGISScene, adds a base surface (elevation)
 * and an IntegratedMeshLayer. Loading of the scene is initiated from here and
 * any errors are surfaced via the MessageDialogViewModel.
 */
class AddIntegratedMeshLayerViewModel(application: Application) : AndroidViewModel(application) {

    // Create an IntegratedMeshLayer from a scene service that contains the Girona, Spain integrated mesh.
    val integratedMeshLayer = IntegratedMeshLayer(
        uri = "https://tiles.arcgis.com/tiles/z2tnIkrLQ2BRzr6P/arcgis/rest/services/Girona_Spain/SceneServer"
    )

    // The elevation source to use on the scene's base surface
    val elevationSource = ArcGISTiledElevationSource(
        uri = "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"
    )

    // The initial camera object for the scene
    val camera = Camera(
        latitude = 41.9906,
        longitude = 2.8259,
        altitude = 200.0,
        heading = 190.0,
        pitch = 65.0,
        roll = 0.0
    )

    // The scene displayed by the SceneView composable
    var arcGISScene = ArcGISScene(BasemapStyle.ArcGISImagery).apply {
        // Add an elevation source to the scene's base surface so the integrated mesh renders with realistic elevation
        baseSurface = Surface().apply { elevationSources.add(elevationSource) }
        // Add the integrated mesh layer as an operational layer
        operationalLayers.add(integratedMeshLayer)
        // Set an initial viewpoint near the integrated mesh
        initialViewpoint = Viewpoint(
            center = camera.location,
            camera = camera,
            scale = 500.0
        )
    }

    // A boolean state indicating whether the scene's initial draw pass completed.
    // The UI shows a loading dialog while this is false.
    var isDrawStatusCompleted by mutableStateOf(false)
        private set

    // Message dialog for surface or layer load errors
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISScene.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    /**
     * Called by the UI when the SceneView reports a [DrawStatus] change.
     * When the draw status becomes Completed, set the flag to dismiss the loading indicator.
     */
    fun onDrawStatusChanged(drawStatus: DrawStatus) {
        if (!isDrawStatusCompleted && drawStatus == DrawStatus.Completed) {
            isDrawStatusCompleted = true
        }
    }
}
