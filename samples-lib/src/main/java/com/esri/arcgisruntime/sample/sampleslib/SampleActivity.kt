package com.esri.arcgisruntime.sample.sampleslib

import android.app.ProgressDialog
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import arcgisruntime.LoadStatus
import arcgisruntime.portal.PortalItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

abstract class SampleActivity : AppCompatActivity() {

    /**
     * The downloadManager checks for provisioned data.
     * If no provisioned data exists it performs a download using the [provisionURL]
     * at the [destinationPath]. The downloadManager clears the directory if user chooses
     * to re-download the provisioned data.
     */
    suspend fun downloadManager(
        provisionURL: String,
        destinationPath: String
    ): Flow<LoadStatus> = callbackFlow {

        // set up the alert dialog builder
        val provisionQuestionDialog = AlertDialog.Builder(this@SampleActivity)
            .setTitle("Download data?")

        // the provision file at the destination
        val provisionFile = File(destinationPath)
        if (provisionFile.exists()) {
            // file exists, prompt user to download again
            provisionQuestionDialog.setMessage(getString(R.string.already_provisioned))
            // if user taps "Re-download" data
            provisionQuestionDialog.setNeutralButton("Re-download data") { dialog, _ ->
                // dismiss provision dialog
                dialog.dismiss()
                // start the download of the portal item
                lifecycleScope.launch {
                    downloadPortalItem(provisionURL, provisionFile).collect {
                        if (it == LoadStatus.Loaded) {
                            // send load status
                            trySend(it)
                        } else if (it is LoadStatus.FailedToLoad) {
                            // send load status
                            trySend(it)
                        }
                    }
                }
            }
            // if user taps "Continue" with existing file
            provisionQuestionDialog.setPositiveButton("Continue") { dialog, _ ->
                // dismiss the provision question dialog
                dialog.dismiss()
                // send Loaded status as file exists
                trySend(LoadStatus.Loaded)
            }
        }
        // if file does not exist, ask for download permission
        else {
            // set require provisioning message
            provisionQuestionDialog.setMessage(getString(R.string.requires_provisioning))
            // if user taps "Download" button
            provisionQuestionDialog.setPositiveButton("Download") { dialog, _ ->
                // dismiss provision dialog
                dialog.dismiss()
                // start the download of the portal item
                lifecycleScope.launch {
                    downloadPortalItem(provisionURL, provisionFile).collect {
                        if (it == LoadStatus.Loaded) {
                            // send load status
                            trySend(it)
                        } else if (it is LoadStatus.FailedToLoad) {
                            // send load status
                            trySend(it)
                        }
                    }
                }
            }
            provisionQuestionDialog.setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        }

        // show the provision question dialog
        provisionQuestionDialog.show()
        // close this callbackFlow channel when the coroutine scope closes
        awaitClose { channel.close() }
    }

    /**
     * Handles the download process using the [provisionURL] at the [destinationPath].
     *
     */
    private fun downloadPortalItem(
        provisionURL: String,
        provisionLocation: File
    ): Flow<LoadStatus> =
        flow {

            // build another dialog to show the progress of the download
            val dialogView: View = layoutInflater.inflate(R.layout.download_dialog, null)
            val loadingBuilder = AlertDialog.Builder(this@SampleActivity).apply {
                setView(dialogView)
                create()
            }

            // show the loading dialog
            val loadingDialog = loadingBuilder.show()
            // delete folder/file prior to downloading dta
            provisionLocation.deleteRecursively()
            // get the PortalItem using the provision URL
            val portalItem = PortalItem(provisionURL)
            // load the PortalItem
            val loadResult = portalItem.load()
            // emit status to the calling activity
            emit(LoadStatus.Loading)
            loadResult.apply {
                onSuccess {
                    // get the data of the PortalItem
                    val portalItemData = portalItem.fetchData()
                    portalItemData.apply {
                        // get the byteArray of the PortalItem
                        onSuccess { byteArray ->
                            runCatching {
                                // create file at location to write the PortalItem ByteArray
                                provisionLocation.createNewFile()
                                // create and write the file output stream
                                val fileOutputStream = FileOutputStream(provisionLocation)
                                fileOutputStream.write(byteArray)
                                // close dialog and emit status
                                loadingDialog.dismiss()
                                emit(LoadStatus.Loaded)
                            }
                        }
                        onFailure {
                            // close dialog and emit status
                            loadingDialog.dismiss()
                            emit(LoadStatus.FailedToLoad(it))
                        }
                    }
                }
                onFailure {
                    // close dialog and emit status
                    loadingDialog.dismiss()
                    emit(LoadStatus.FailedToLoad(it))
                }
            }
        }
}