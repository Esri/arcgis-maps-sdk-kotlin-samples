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

package com.esri.arcgismaps.sample.showmobilemappackageexpirationdate.components

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import com.esri.arcgismaps.sample.showmobilemappackageexpirationdate.R
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch

class ShowMobileMapPackageExpirationDateViewModel(application: Application) : AndroidViewModel(application) {
    // The map displayed on the MapView. Updated after loading the mobile map package
    var arcGISMap: ArcGISMap by mutableStateOf(ArcGISMap())
        private set

    // Message dialog view model for handling error messages
    val messageDialogVM = MessageDialogViewModel()

    // Expiration UI states exposed to the screen
    var isExpired by mutableStateOf(false)
        private set

    var expirationMessage by mutableStateOf<String?>(null)
        private set

    var expirationDateText by mutableStateOf<String?>(null)
        private set

    // Build the provision path where offline resources are stored for the sample app
    private val provisionPath: String by lazy {
        val basePath = application.getExternalFilesDir(null)?.path ?: ""
        val appFolderName = getApplication<Application>().getString(
            R.string.show_mobile_map_package_expiration_date_app_name
        )
        basePath + File.separator + appFolderName
    }

    init {
        // Load the mobile map package and update map/expiration info
        viewModelScope.launch {
            loadMobileMapPackageAndUpdateState()
        }
    }

    // Loads the local mobile map package and updates the map and expiration states
    private suspend fun loadMobileMapPackageAndUpdateState() {
        // Locate the LothianRiversAnno.mmpk file in the provisioned path
        val mmpkFile = File(provisionPath, "LothianRiversAnno.mmpk")
        if (!mmpkFile.exists()) {
            messageDialogVM.showMessageDialog("Mobile map package file does not exist.")
            return
        }

        // Create and load the mobile map package
        val mobileMapPackage = MobileMapPackage(mmpkFile.path)
        mobileMapPackage.load().onSuccess {
            // If the loaded mobile map package does not contain any maps
            if (mobileMapPackage.maps.isEmpty()) {
                messageDialogVM.showMessageDialog("Mobile map package does not contain a map")
                return@onSuccess
            }

            // Set the map to the first map in the mobile map package
            arcGISMap = mobileMapPackage.maps.first()

            // Read expiration information from the mobile map package
            mobileMapPackage.expiration?.let { expiration ->
                isExpired = expiration.isExpired
                expirationMessage = expiration.message

                val formatter = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault())

                expirationDateText = expiration.dateTime?.let { formatter.format(it) } ?: "N/A"
            }
        }.onFailure { error ->
            messageDialogVM.showMessageDialog(
                title = "Failed to load the mobile map package",
                description = error.message.toString()
            )
        }
    }
}
