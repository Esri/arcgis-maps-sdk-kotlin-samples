package com.esri.arcgisruntime.sample.sampleslib

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import arcgisruntime.LoadStatus
import arcgisruntime.portal.PortalItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SampleActivity : AppCompatActivity() {

    /**
     * The downloadManager checks for provisioned data.
     * If no provisioned data exists it performs a download using the [provisionURL]
     * at the [destinationPath]. The downloadManager clears the directory if user chooses
     * to re-download the provisioned data.
     */
    suspend fun sampleDownloadManager(
        provisionURL: String,
        destinationPath: String
    ): Flow<Unit> = flow {

        // the provision file at the destination
        val provisionFile = File(destinationPath)

        // suspends the coroutine until the dialog is resolved.
        val shouldDoDownload = suspendCancellableCoroutine<Boolean> { shouldDownloadContinuation ->
            // set up the alert dialog builder
            val provisionQuestionDialog = AlertDialog.Builder(this@SampleActivity)
                .setTitle("Download data?")

            if (provisionFile.exists()) {
                // file exists, prompt user to download again
                provisionQuestionDialog.setMessage(getString(R.string.already_provisioned))
                // if user taps "Re-download" data
                provisionQuestionDialog.setNeutralButton("Re-download data") { dialog, _ ->
                    // dismiss provision dialog
                    dialog.dismiss()
                    shouldDownloadContinuation.resume(true, null)
                }
                // if user taps "Continue" with existing file
                provisionQuestionDialog.setPositiveButton("Continue") { dialog, _ ->
                    // dismiss the provision question dialog
                    dialog.dismiss()
                    // send Loaded status as file exists
                    shouldDownloadContinuation.resume(fa, null)
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
                                trySend(Unit)
                            } else if (it is LoadStatus.FailedToLoad) {
                                // display error
                                showError(it.error.message.toString())
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
        }

        // Back in coroutine world, we know if the download should happen or not.
        if (shouldDoDownload) {
            // Start the download (we can just do it here because we are in a suspending
            // context), and wait until it completes.
            // Alternatively if we want to propagate the loading status flow to users, we could
            // just return it (I think, haven't thought too hard about it).
            //return downloadPortalItem(..)
            downloadPortalItem(provisionURL, provisionFile).collect()
        }
    }

    /**
     * Handles the download process using the [provisionURL] at the [provisionLocation].
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
            // set cancel button for the loading dialog
            val cancelButton = dialogView.findViewById<View>(R.id.cancelButton)
            cancelButton.setOnClickListener {
                loadingDialog.dismiss()
                //re-start sample to show the dialog again
                this@SampleActivity.recreate()
            }
            // delete folder/file prior to downloading data
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
                    val byteArrayInputStream = portalItemData.getOrElse {
                        it.printStackTrace()
                    }
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

    private fun showError(message: String) {
        Log.e(this.packageName, message)
        Toast.makeText(this@SampleActivity, message, Toast.LENGTH_SHORT).show()
    }
}
