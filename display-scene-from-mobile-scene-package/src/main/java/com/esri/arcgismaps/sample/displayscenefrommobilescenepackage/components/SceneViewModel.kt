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

package com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.MobileScenePackage
import com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class SceneViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // create a base scene to be used to load the mobile scene package
    var scene by mutableStateOf(ArcGISScene(BasemapStyle.ArcGISStreets))

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    private val provisionPath: String by lazy {
        application.getExternalFilesDir(null)?.path.toString() + File.separator + application.getString(
            R.string.app_name
        )
    }

    init {
        createMobileScenePackage()
    }

    private fun createMobileScenePackage() {
        // get the file path of the (.mspk) file
        val filePath = provisionPath + application.getString(R.string.philadelphia_mspk)

        // create the mobile scene package
        val mobileScenePackage = MobileScenePackage(filePath)

        sampleCoroutineScope.launch {
            // load the mobile scene package
            mobileScenePackage.load().onSuccess {
                // update the mutable state holder with the first scene from the MobileScenePackage
                scene = mobileScenePackage.scenes.first()
            }.onFailure { error ->
                // show the message dialog and pass the error message to be displayed in the dialog
                messageDialogVM.showMessageDialog(error.message.toString(), error.cause.toString())
            }
        }
    }
}
