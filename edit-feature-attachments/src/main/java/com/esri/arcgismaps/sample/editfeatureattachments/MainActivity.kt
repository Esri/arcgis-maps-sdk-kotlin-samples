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

package com.esri.arcgismaps.sample.editfeatureattachments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.data.FeatureRequestMode
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.AttachmentEditSheetBinding
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.AttachmentLoadingDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val attachmentsSheetBinding by lazy {
        AttachmentEditSheetBinding.inflate(layoutInflater)
    }

    private val loadingDialogBinding by lazy {
        AttachmentLoadingDialogBinding.inflate(layoutInflater)
    }

    // load the Damage to Residential Buildings feature server
    private val serviceFeatureTable by lazy {
        ServiceFeatureTable(getString(R.string.sample_service_url)).apply {
            // set the feature request mode to request from the server as they are needed
            featureRequestMode = FeatureRequestMode.OnInteractionCache
        }
    }

    // registers the activity for an image data result from the default image picker
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { imageUri ->
                // add the image data as a feature attachment
                addFeatureAttachment(imageUri)
            }
        }

    // tracks the selected ArcGISFeature and it's attachments
    private var selectedArcGISFeature: ArcGISFeature? = null

    // tracks the instance of the bottom sheet
    private var bottomSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create the feature layer using the service feature table
        val featureLayer = FeatureLayer.createWithFeatureTable(serviceFeatureTable)

        // create and add a map with a streets basemap style
        val streetsMap = ArcGISMap(BasemapStyle.ArcGISStreets).apply {
            operationalLayers.add(featureLayer)
        }
        // set the map and the viewpoint to the MapView
        mapView.apply {
            map = streetsMap
            setViewpoint(Viewpoint(40.0, -95.0, 1e8))
        }

        // identify feature selected on map tap
        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { tapConfirmedEvent ->
                // clear any previous selection
                featureLayer.clearSelection()
                // identify tapped feature
                val layerResult = mapView.identifyLayer(
                    layer = featureLayer,
                    screenCoordinate = tapConfirmedEvent.screenCoordinate,
                    tolerance = 5.0,
                    returnPopupsOnly = false,
                    maximumResults = 1
                ).getOrElse { exception ->
                    showError("Failed to select feature: ${exception.message}")
                } as IdentifyLayerResult

                // get a list of identified elements
                val resultGeoElements: List<GeoElement> = layerResult.geoElements
                // check if a feature was identified
                if (resultGeoElements.isNotEmpty() && resultGeoElements.first() is ArcGISFeature) {
                    // retrieve and set the currently selected feature
                    val selectedFeature = resultGeoElements.first() as ArcGISFeature
                    // highlight the currently selected feature
                    featureLayer.selectFeature(selectedFeature)

                    // show the bottom sheet layout
                    createBottomSheet(selectedFeature)

                    // keep track of the selected feature
                    selectedArcGISFeature = selectedFeature
                }
            }
        }
    }

    /**
     * Creates and displays a bottom sheet to display and modify
     * the attachments of [selectedFeature]. Calls [AttachmentsBottomSheet] to
     * inflate bottom sheet and listen for interactions.
     */
    private suspend fun createBottomSheet(selectedFeature: ArcGISFeature) {
        // get the number of attachments
        val attachmentList = selectedFeature.fetchAttachments().getOrElse {
            return showError(it.message.toString())
        }
        // get the attribute "typdamage" of the selected feature
        val damageTypeAttribute = selectedFeature.attributes["typdamage"].toString()

        // creates a new BottomSheetDialog
        bottomSheet = AttachmentsBottomSheet(
            context = this@MainActivity,
            bottomSheetBinding = attachmentsSheetBinding,
            attachments = attachmentList,
            damageType = damageTypeAttribute
        )
        // set the content view to the root of the binding layout
        bottomSheet?.setContentView(attachmentsSheetBinding.root)
        // display the bottom sheet view
        bottomSheet?.show()
    }

    /**
     * Retrieves the [attachment] data in the form of a byte array, converts it
     * to a [BitmapDrawable], caches the bitmap as a png image, and open's the
     * attachment image in the default image viewer.
     */
    suspend fun fetchAttachment(attachment: Attachment) {
        // display loading dialog
        val dialog = createLoadingDialog("Fetching attachment data").also {
            it.show()
        }

        // create folder /ArcGIS/Attachments in external storage
        val fileDir = File(externalCacheDir?.path + "/Attachments")
        fileDir.mkdirs()
        // create the file with the attachment name
        val file = File(fileDir, attachment.name)

        // file provider URI
        val contentUri = FileProvider.getUriForFile(
            applicationContext, applicationContext.packageName + ".provider", file
        )
        // open the file in gallery
        val imageIntent = Intent().apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            action = Intent.ACTION_VIEW
            setDataAndType(contentUri, "image/png")
        }

        // fetch the attachment data
        attachment.fetchData().onSuccess {
            // create a drawable from InputStream, then create the Bitmap
            val bitmapDrawable = BitmapDrawable(
                resources,
                BitmapFactory.decodeByteArray(it, 0, it.size)
            )
            // create a file output stream using the attachment file
            FileOutputStream(file).use { imageOutputStream ->
                // compress the bitmap to PNG format
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.PNG, 90, imageOutputStream)
                // start activity using created intent
                startActivity(imageIntent)
                // dismiss dialog
                dialog.dismiss()
            }
        }.onFailure {
            // dismiss dialog
            dialog.dismiss()
            showError(it.message.toString())
        }
    }

    /**
     * Adds a new attachment to the [selectedArcGISFeature] using the [selectedImageUri]
     * and updates the changes with the feature service table
     */
    private fun addFeatureAttachment(selectedImageUri: Uri) {
        // display a loading dialog
        val dialog = createLoadingDialog("Adding feature attachment").also {
            it.show()
        }

        // create an input stream at the selected URI
        contentResolver.openInputStream(selectedImageUri)?.use { imageInputStream ->
            // get the byte array of the image input stream
            val imageBytes: ByteArray = imageInputStream.readBytes()
            // create the attachment name with the current time
            val attachmentName = "attachment_${System.currentTimeMillis()}.png"

            lifecycleScope.launch {
                selectedArcGISFeature?.let { arcGISFeature ->
                    // add the attachment to the selected feature
                    arcGISFeature.addAttachment(
                        name = attachmentName,
                        contentType = "image/png",
                        data = imageBytes
                    ).onFailure {
                        return@launch showError(it.message.toString())
                    }
                    // update the feature changes in the loaded service feature table
                    serviceFeatureTable.updateFeature(arcGISFeature).getOrElse {
                        return@launch showError(it.message.toString())
                    }
                }
                applyServerEdits(dialog)
            }
        }
    }

    /**
     * Delete the [attachment] from the [selectedArcGISFeature] and update the changes
     * with the feature service table
     */
    fun deleteAttachment(attachment: Attachment) {
        lifecycleScope.launch {
            val dialog = createLoadingDialog("Deleting feature attachment").also {
                it.show()
            }
            selectedArcGISFeature?.let { arcGISFeature ->
                // delete the attachment from the selected feature
                arcGISFeature.deleteAttachment(attachment).getOrElse {
                    return@launch showError(it.message.toString())
                }
                // update the feature changes in the loaded service feature table
                serviceFeatureTable.updateFeature(arcGISFeature).getOrElse {
                    return@launch showError(it.message.toString())
                }
            }
            // apply changes back to the server
            applyServerEdits(dialog)
        }
    }

    /**
     * Applies changes from a Service Feature Table to the server.
     * The [dialog] will be dismissed when changes are applied.
     */
    private suspend fun applyServerEdits(dialog: AlertDialog) {
        // close the bottom sheet, as it will be created
        // after service changes are made
        bottomSheet?.dismiss()

        // apply edits to the server
        val updatedServerResult = serviceFeatureTable.applyEdits()
        updatedServerResult.onSuccess { edits ->
            dialog.dismiss()
            // check that the feature table was successfully updated
            if (edits.isEmpty()) {
                return showError(getString(R.string.failure_edit_results))
            }
            // if the edits were made successfully, create the bottom sheet to display new changes.
            selectedArcGISFeature?.let { createBottomSheet(it) }
        }.onFailure {
            showError(it.message.toString())
            dialog.dismiss()
        }
    }

    /**
     * Opens the default Android image selector
     */
    fun selectAttachment() {
        val mediaIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(mediaIntent)
    }

    /**
     * Creates a loading dialog with the [message]
     */
    private fun createLoadingDialog(message: String): AlertDialog {
        // build and return a new alert dialog
        return AlertDialog.Builder(this).apply {
            // set message
            setMessage(message)
            // allow it to be cancellable
            setCancelable(false)
            // removes parent of the progressDialog layout, if previously assigned
            loadingDialogBinding.root.parent?.let { parent ->
                (parent as ViewGroup).removeAllViews()
            }
            // set the loading dialog layout to this alert dialog
            setView(loadingDialogBinding.root)
        }.create()
    }

    private fun showError(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
