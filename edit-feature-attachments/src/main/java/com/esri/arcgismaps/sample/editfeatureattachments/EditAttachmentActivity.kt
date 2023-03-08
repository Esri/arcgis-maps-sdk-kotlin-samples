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
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.Attachment
import com.arcgismaps.data.FeatureQueryResult
import com.arcgismaps.data.QueryParameters
import com.arcgismaps.data.ServiceFeatureTable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class EditAttachmentActivity : AppCompatActivity() {

    private val RESULT_LOAD_IMAGE = 1
    private var adapter: ArrayAdapter<String>? = null
    private var attachments: List<Attachment>? = null
    private var mSelectedArcGISFeature: ArcGISFeature? = null
    private var mServiceFeatureTable: ServiceFeatureTable? = null
    private var mAttributeID: String? = null
    private var listView: ListView? = null
    private var attachmentList = ArrayList<String>()
    private var progressDialog: AlertDialog? = null
    private var builder: AlertDialog.Builder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_attachment)

        val bundle = intent.extras
        val s = bundle!!.getString(getString(R.string.attribute)).toString()
        val noOfAttachments = bundle.getInt(application.getString(R.string.noOfAttachments))

        // Build a alert dialog with specified style

        // Build a alert dialog with specified style
        builder = AlertDialog.Builder(this)

        // get a reference to the floating action button

        // get a reference to the floating action button
        val addAttachmentFab = findViewById<FloatingActionButton>(R.id.addAttachmentFAB)

        // select an image to upload as an attachment

        // select an image to upload as an attachment
        addAttachmentFab.setOnClickListener { v: View? -> selectAttachment() }

        mServiceFeatureTable = ServiceFeatureTable(resources.getString(R.string.sample_service_url))

        // display progress dialog if selected feature has attachments
        if (noOfAttachments != 0) {
            val dialog = AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.fetching_attachments))
                setMessage(getString(R.string.fetching_attachments))
            }.create()
            dialog.show()
        } else {
            Toast.makeText(this, getString(R.string.empty_attachment_message), Toast.LENGTH_LONG)
                .show()
        }

        // get a reference to the list view
        listView = findViewById(R.id.listView)
        // create custom adapter
        adapter = CustomList(this, attachmentList)
        // set custom adapter on the list
        listView!!.adapter = adapter
        lifecycleScope.launch {
            fetchAttachmentsFromServer(s)
        }


        // listener on attachment items to download the attachment

        // listener on attachment items to download the attachment
        listView!!.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                fetchAttachmentAsync(
                    position
                )
            }

        // set on long click listener to delete the attachment
        listView!!.onItemLongClickListener = OnItemLongClickListener { _, _, position: Int, _ ->
            builder!!.setMessage(application.getString(R.string.delete_query))
            builder!!.setCancelable(true)
            builder!!.setPositiveButton(
                resources.getString(R.string.yes)
            ) { dialog: DialogInterface, which: Int ->
                deleteAttachment(position)
                dialog.dismiss()
            }
            builder!!.setNegativeButton(
                resources.getString(R.string.no)
            ) { dialog: DialogInterface, which: Int -> dialog.cancel() }
            val alert = builder!!.create()
            alert.show()
            true
        }
    }

    private fun selectAttachment() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, RESULT_LOAD_IMAGE)
    }

    private fun fetchAttachmentAsync(position: Int) {
        progressDialog!!.setTitle(application.getString(R.string.downloading_attachments))
        progressDialog!!.setMessage(application.getString(R.string.wait))
        progressDialog!!.show()

        // create a listenableFuture to fetch the attachment asynchronously
        lifecycleScope.launch {
            attachments!![position].fetchData().onSuccess {
                val fileName = attachmentList[position]
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
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
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
                showMessage(it.message.toString())
            }
        }
    }

    /**
     * Delete the attachment from the feature
     *
     * @param pos position of the attachment in the list view to be deleted
     */
    private fun deleteAttachment(pos: Int) {
        progressDialog!!.setTitle(application.getString(R.string.deleting_attachments))
        progressDialog!!.setMessage(application.getString(R.string.wait))
        progressDialog!!.show()
        attachmentList.removeAt(pos)
        adapter!!.notifyDataSetChanged()
        lifecycleScope.launch {
            mSelectedArcGISFeature?.deleteAttachment(attachments!![pos])?.getOrElse {
                showMessage(it.message.toString())
            }
            mServiceFeatureTable?.updateFeature(mSelectedArcGISFeature!!)?.getOrElse {
                showMessage(it.message.toString())
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
        val updatedServerResult = mServiceFeatureTable?.applyEdits()
        updatedServerResult?.onSuccess { edits ->
            // check that the feature table was successfully updated
            if (edits.isNotEmpty()) {
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
                mAttributeID = mSelectedArcGISFeature?.attributes?.get("objectid").toString()
                fetchAttachmentsFromServer(mAttributeID!!)
                // update the attachment list view on the control panel
                showMessage(getString(R.string.success_message))
            } else {
                showMessage(getString(R.string.failure_edit_results))
            }
        }?.onFailure {
            showMessage("Error getting feature edit result: ${it.message}")
        }
    }

    /**
     * Asynchronously fetch the given feature's attachments and show them a list view.
     *
     * @param objectID of the feature from which to fetch attachments
     */
    private suspend fun fetchAttachmentsFromServer(objectID: String) {
        attachmentList = java.util.ArrayList()
        // create objects required to do a selection with a query
        val query = QueryParameters()
        // set the where clause of the query
        query.whereClause = "OBJECTID = $objectID"

        // query the feature table
        val featureQueryResult = mServiceFeatureTable?.queryFeatures(query)?.getOrElse {
            showMessage("Error retrieving service feature table")
        } as FeatureQueryResult
        mSelectedArcGISFeature = featureQueryResult.iterator().next() as ArcGISFeature

        // get the number of attachments
        val attachments=  mSelectedArcGISFeature?.fetchAttachments()?.getOrElse {
            showMessage("Error retrieving attachments")
        } as List<Attachment>

        if(attachments.isNotEmpty()){
            for (attachment in attachments) {
                attachmentList.add(attachment.name)
            }
            runOnUiThread {
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
                adapter = CustomList(this, attachmentList)
                listView!!.adapter = adapter
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun showMessage(message: String) {
        Log.e(localClassName, message)
        Snackbar.make(findViewById(R.id.listView), message, Snackbar.LENGTH_SHORT).show()
    }
}