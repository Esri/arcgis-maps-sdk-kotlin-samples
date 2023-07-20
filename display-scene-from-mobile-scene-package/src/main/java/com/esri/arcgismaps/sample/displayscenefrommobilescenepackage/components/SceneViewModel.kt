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
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.MobileScenePackage
import com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SceneViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    // create a mutable state holder for the first scene
    private val _firstScene = MutableStateFlow<ArcGISScene?>(null)
    // expose a read-only state flow for observing changes to the first scene
    val firstScene: StateFlow<ArcGISScene?> = _firstScene.asStateFlow()

    // create a mutable state holder for the snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    // expose a read-only state flow for observing changes to the snackbar message
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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
                _firstScene.value = mobileScenePackage.scenes.first()
                // set the value to null on successful load
                _snackbarMessage.value = null
            }.onFailure { error->
                // set the value to be displayed in the snackbar
                _snackbarMessage.value = error.message.toString()
            }
        }
    }

    fun onSingleTap() {
        // do nothing
    }
}
