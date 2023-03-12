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
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.ActivityMainBinding
import com.esri.arcgismaps.sample.editfeatureattachments.databinding.SheetEditAttachmentBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // set up data binding for the activity
    private val activityMainBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    private val mapView by lazy {
        activityMainBinding.mapView
    }

    private val bottomSheetBinding by lazy {
        SheetEditAttachmentBinding.inflate(layoutInflater)
    }


    private val mServiceFeatureTable by lazy {
        ServiceFeatureTable(getString(R.string.sample_service_url))
    }

    private val RESULT_LOAD_IMAGE = 1
    private var mSelectedArcGISFeature: ArcGISFeature? = null
    private var mAttributeID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)
        lifecycle.addObserver(mapView)

        // create and add a map with a navigation night basemap style
        val map = ArcGISMap(BasemapStyle.ArcGISStreets)
        mapView.map = map
        mapView.setViewpoint(Viewpoint(40.0, -95.0, 1e8))

        // create feature layer with its service feature table and create the service feature table
        mServiceFeatureTable.featureRequestMode = FeatureRequestMode.OnInteractionCache
        // create the feature layer using the service feature table
        val mFeatureLayer = FeatureLayer(mServiceFeatureTable)
        // add the layer to the map
        map.operationalLayers.add(mFeatureLayer)

        // TODO get callout, set content and show
        //mCallout = mMapView.getCallout()

        lifecycleScope.launch {
            mapView.onSingleTapConfirmed.collect { tapConfirmedEvent ->
                val screenCoordinate = tapConfirmedEvent.screenCoordinate
                // clear any previous selection
                mFeatureLayer.clearSelection()
                mapView.identifyLayer(
                    layer = mFeatureLayer,
                    screenCoordinate = screenCoordinate,
                    tolerance = 5.0,
                    returnPopupsOnly = false,
                    maximumResults = 1
                ).onSuccess { layerResult ->
                    val resultGeoElements: List<GeoElement> = layerResult.geoElements
                    if (resultGeoElements.isNotEmpty() && resultGeoElements[0] is ArcGISFeature) {
                        // retrieve and set the currently selected feature
                        val selectedFeature = resultGeoElements[0] as ArcGISFeature
                        // highlight the currently selected feature
                        mFeatureLayer.selectFeature(selectedFeature)
                        mAttributeID = selectedFeature.attributes["objectid"].toString()
                        // get the number of attachments
                        val attachments = selectedFeature.fetchAttachments().getOrThrow()
                        // show callout with the value for the attribute "typdamage" of the selected feature
                        val damageType = selectedFeature.attributes["typdamage"].toString()
                        createBottomSheet(damageType, attachments)
                        //showDialog(damageType, attachments.size)
                        //showCallout(mSelectedArcGISFeatureAttributeValue, attachments!!.size)
                        mSelectedArcGISFeature = selectedFeature
                    } else {
                        // none of the features on the map were selected
                        // TODO
                        // mCallout.dismiss();
                    }
                }.onFailure {
                    showError("Failed to select feature: ${it.message}")
                }
            }
        }
    }

    private fun showDialog(damageType: String, attachmentSize: Int) {
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setTitle("Damage type: $damageType")
            setMessage(getString(R.string.attachment_info_message) + attachmentSize)
            setNegativeButton("Dismiss") { _, _ ->

            }
            setPositiveButton("Edit attachments") { _, _ ->
                // start EditAttachmentActivity to view/edit the attachments
                val myIntent = Intent(this@MainActivity, SheetEditAttachmentBinding::class.java)
                myIntent.putExtra(getString(R.string.attribute), mAttributeID)
                myIntent.putExtra(getString(R.string.noOfAttachments), attachmentSize)
            }
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun createBottomSheet(damageType: String, attachments: List<Attachment>) {
        // creates a new BottomSheetDialog
        val bottomSheet = BottomSheetDialog(this).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // clear and set bottom sheet content view to layout,
        // to be able to set the content view on each bottom sheet draw
        if (bottomSheetBinding.root.parent != null) {
            (bottomSheetBinding.root.parent as ViewGroup).removeAllViews()
        }

        val attachmentList = mutableListOf<String>()
        attachments.forEach {
            attachmentList.add(it.name)
        }
        val adapter = CustomList(this, attachmentList)

        bottomSheetBinding.apply {

            bottomSheetBinding.listView.adapter = adapter
            bottomSheetBinding.damageStatus.text = String.format("Damage type: %s", damageType)
            bottomSheetBinding.numberOfAttachments.text =
                String.format("Number of attachments: %d", attachments.size)

            bottomSheetBinding.addAttachmentButton.setOnClickListener {
                selectAttachment()
            }

            // listener on attachment items to download the attachment
            bottomSheetBinding.listView.onItemClickListener =
                AdapterView.OnItemClickListener { _: AdapterView<*>?, view: View?, position: Int, id: Long ->
                    fetchAttachmentAsync(
                        attachments[position]
                    )
                }

            // set on long click listener to delete the attachment
            bottomSheetBinding.listView.onItemLongClickListener =
                AdapterView.OnItemLongClickListener { _, _, position: Int, _ ->
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setMessage(application.getString(R.string.delete_query))
                    builder.setCancelable(true)
                    builder.setPositiveButton(
                        resources.getString(R.string.yes)
                    ) { dialog: DialogInterface, which: Int ->
                        deleteAttachment(attachments[position])
                        adapter.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                    builder.setNegativeButton(
                        resources.getString(R.string.no)
                    ) { dialog: DialogInterface, which: Int -> dialog.cancel() }
                    val alert = builder.create()
                    alert.show()
                    true
                }

            // set apply button to validate and apply contingency feature on map
            applyTv.setOnClickListener {
                bottomSheet.dismiss()
            }
        }

        // set the content view to the root of the binding layout
        bottomSheet.setContentView(bottomSheetBinding.root)
        // display the bottom sheet view
        bottomSheet.show()
    }

    private fun fetchAttachmentAsync(attachment: Attachment) {
        /*
                progressDialog?.setTitle(application.getString(R.string.downloading_attachments))
        progressDialog?.setMessage(application.getString(R.string.wait))
        progressDialog?.show()
         */

        // create a listenableFuture to fetch the attachment asynchronously
        lifecycleScope.launch {
            attachment.fetchData().onSuccess {
                val fileName = attachment.name
                // create a drawable from InputStream
                val d = BitmapDrawable(resources, BitmapFactory.decodeByteArray(it, 0, it.size))
                // create a bitmap from drawable
                val bitmap = (d as BitmapDrawable?)!!.bitmap
                val fileDir = File(getExternalFilesDir(null).toString() + "/ArcGIS/Attachments")
                // create folder /ArcGIS/Attachments in external storage
                var isDirectoryCreated = fileDir.exists()
                if (!isDirectoryCreated) {
                    isDirectoryCreated = fileDir.mkdirs()
                }
                var file: File? = null
                if (isDirectoryCreated) {
                    file = File(fileDir, fileName)
                    val fos = FileOutputStream(file)
                    // compress the bitmap to PNG format
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                    fos.flush()
                    fos.close()
                }
                // open the file in gallery
                val i = Intent()
                i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                i.action = Intent.ACTION_VIEW
                val contentUri = FileProvider.getUriForFile(
                    applicationContext, applicationContext.packageName + ".provider", file!!
                )
                i.setDataAndType(contentUri, "image/png")
                startActivity(i)
            }.onFailure {
                showError(it.message.toString())
            }
        }
    }

    /**
     * Delete the attachment from the feature
     *
     * @param pos position of the attachment in the list view to be deleted
     */
    private fun deleteAttachment(attachment: Attachment) {
        lifecycleScope.launch {
            mSelectedArcGISFeature?.deleteAttachment(attachment)?.getOrElse {
                showError(it.message.toString())
            }
            mServiceFeatureTable.updateFeature(mSelectedArcGISFeature!!).getOrElse {
                showError(it.message.toString())
            }
            // apply changes back to the server
            applyServerEdits()
        }
    }

    /**
     * Applies changes from a Service Feature Table to the server.
     */
    private suspend fun applyServerEdits() {
        // apply edits to the server
        val updatedServerResult = mServiceFeatureTable.applyEdits()
        updatedServerResult.onSuccess { edits ->
            // check that the feature table was successfully updated
            if (edits.isNotEmpty()) {
                mAttributeID = mSelectedArcGISFeature?.attributes?.get("objectid").toString()
                //fetchAttachmentsFromServer(mAttributeID!!)
                // update the attachment list view on the control panel
                showError(getString(R.string.success_message))
            } else {
                showError(getString(R.string.failure_edit_results))
            }
        }.onFailure {
            showError("Error getting feature edit result: ${it.message}")
        }
    }

    private fun selectAttachment() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, RESULT_LOAD_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // check if image is selected from MediaStore
        if (requestCode == RESULT_LOAD_IMAGE && data != null) {
            val selectedImage = data.data ?: return showError("Error with selected image")
            val imageInputStream = contentResolver.openInputStream(selectedImage)
                ?: return showError("Error opening input stream")
            val imageBytes: ByteArray = bytesFromInputStream(imageInputStream)
            val attachmentName =
                getString(R.string.attachment) + '_' + System.currentTimeMillis() + ".png"
            /*
            progressDialog.setTitle(application.getString(R.string.apply_edit_message))
            progressDialog.setMessage(application.getString(R.string.wait))
            progressDialog.show()
             */

            lifecycleScope.launch {
                val addResult = mSelectedArcGISFeature?.addAttachment(
                    name = attachmentName,
                    contentType = "image/png",
                    data = imageBytes
                )?.getOrElse {
                    return@launch showError("Error converting image to byte array: ${it.message}")
                } as Attachment

                mServiceFeatureTable.updateFeature(mSelectedArcGISFeature!!).getOrElse {
                    return@launch showError("Error updating feature to service feature table")
                }
                applyServerEdits()
                showError("DONE!")
            }
        }
    }


    /**
     * Converts the given input stream into a byte array.
     *
     * @param inputStream from an image
     * @return an array of bytes from the input stream
     * @throws IOException if input stream can't be read
     */
    private fun bytesFromInputStream(inputStream: InputStream): ByteArray {
        ByteArrayOutputStream().use { byteBuffer ->
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            return byteBuffer.toByteArray()
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        Snackbar.make(mapView, message, Snackbar.LENGTH_SHORT).show()
    }
}
