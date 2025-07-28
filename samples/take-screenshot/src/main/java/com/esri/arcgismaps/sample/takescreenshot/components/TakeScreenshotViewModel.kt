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
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getString
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.takescreenshot.R
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TakeScreenshotViewModel(app: Application) : AndroidViewModel(app) {

    val arcGISMap = ArcGISMap(BasemapStyle.ArcGISNavigationNight).apply {
        initialViewpoint = Viewpoint(39.8, -98.6, 10e7)
    }

    // Create a message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // screenshot image for display
    var screenshotImage: BitmapDrawable? by mutableStateOf(null)
        private set

    // class to interact with MapView
    val mapViewProxy = MapViewProxy()

    init {
        viewModelScope.launch {
            arcGISMap.load().onFailure { messageDialogVM.showMessageDialog(it) }
        }
    }

    // clears the current screenshot image by setting it to null
    fun clearScreenshotImage(){
        screenshotImage = null
    }

    // function to take screenshot of MapView
    fun takeScreenshot(){
        viewModelScope.launch {
            screenshotImage = mapViewProxy.exportImage().getOrNull()
        }
    }

    // Function to save a bitmap image to a file and return its URI
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
        // Create a file in the cache directory
        val file = File(context.cacheDir, "take-screenshot-sample-screenshot.png")
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val outputStream = FileOutputStream(file)

        // Compress the bitmap and save it to the file
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        // Return the URI for the file
        return FileProvider.getUriForFile(context, getString(context, R.string.take_screenshot_provider_authority), file)
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val filename = "screenshot-${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(imagesDir, filename)
            FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            // Notify the media scanner about the new file.
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.toString()),
                arrayOf("image/jpeg"),
                null
            )
            return Uri.fromFile(imageFile)
        }

        // Use MediaStore API for Android 10 (API 29) and above
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }
        return uri
    }

}

// Custom FileProvider for handling file sharing of screenshots
class TakeScreenshotFileProvider : FileProvider()
