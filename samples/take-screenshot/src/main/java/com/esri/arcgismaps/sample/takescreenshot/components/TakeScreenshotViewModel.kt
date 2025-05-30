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

package com.esri.arcgismaps.sample.takescreenshot.components

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class TakeScreenshotViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // screenshot image for display
    val screenshotImage: MutableState<BitmapDrawable?> = mutableStateOf(null)

    // class to interact with MapView
    val mapViewProxy = MapViewProxy()

    // function to take screenshot of MapView
    fun takeScreenshot(){
        viewModelScope.launch {
            screenshotImage.value = mapViewProxy.exportImage().getOrNull()
        }
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        // Create a file in the cache directory
        val file = File(context.cacheDir, "screenshot.png")
        val outputStream = FileOutputStream(file)

        // Compress the bitmap and save it to the file
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        // Return the URI for the file
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

}
