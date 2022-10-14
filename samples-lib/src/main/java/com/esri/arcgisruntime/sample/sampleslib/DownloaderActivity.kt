/* Copyright 2022 Esri
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

package com.esri.arcgisruntime.sample.sampleslib

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import arcgisruntime.LoadStatus
import arcgisruntime.portal.PortalItem
import com.esri.arcgisruntime.sample.sampleslib.databinding.ActivitySamplesBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
abstract class DownloaderActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activitySamplesBinding: ActivitySamplesBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_samples)
    }

    /**
     * Gets the [provisionURL] of the portal item to download at
     * the [filePath], once download completes it starts the [mainActivity]
     */
    fun doDownloadThenStartSample(
        mainActivity: Intent,
        filePath: String,
        provisionURL: String
    ) {
        // start the download manager to automatically add the .mmpk file to the app
        // alternatively, you can use ADB/Device File Explorer
        lifecycleScope.launch {
            sampleDownloadManager(provisionURL, filePath).collect { loadStatus ->
                if (loadStatus == LoadStatus.Loaded) {
                    // download complete, resuming sample
                    startActivity(Intent(mainActivity))
                    finish()
                } else if (loadStatus is LoadStatus.FailedToLoad) {
                    // show error message
                    val errorMessage = loadStatus.error.message.toString()
                    Snackbar.make(
                        activitySamplesBinding.layout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    Log.e(this@DownloaderActivity.packageName, errorMessage)
                }
            }
        }
    }

    /**
     * The downloadManager checks for provisioned data.
     * If no provisioned data exists it performs a download using the [provisionURL]
     * at the [destinationPath]. The downloadManager clears the directory if user chooses
     * to re-download the provisioned data.
     */
    private suspend fun sampleDownloadManager(
        provisionURL: String,
        destinationPath: String
    ): Flow<LoadStatus> = flow {

        // the provision file at the destination
        val provisionFile = File(destinationPath)

        // suspends the coroutine until the dialog is resolved.
        val shouldDoDownload: Boolean = suspendCancellableCoroutine { shouldDownloadContinuation ->
            // set up the alert dialog builder
            val provisionQuestionDialog = AlertDialog.Builder(this@DownloaderActivity)
                .setTitle("Download data?")

            if (provisionFile.exists()) {
                // file exists, prompt user to download again
                provisionQuestionDialog.setMessage(getString(R.string.already_provisioned))
                // if user taps "Re-download" data
                provisionQuestionDialog.setNeutralButton("Re-download data") { dialog, _ ->
                    // dismiss provision dialog question dialog
                    dialog.dismiss()
                    // set to should download
                    shouldDownloadContinuation.resume(true, null)
                }
                // if user taps "Continue" with existing file
                provisionQuestionDialog.setPositiveButton("Continue") { dialog, _ ->
                    // dismiss the provision question dialog
                    dialog.dismiss()
                    // set to should not download
                    shouldDownloadContinuation.resume(false, null)
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
                    // set to should download
                    shouldDownloadContinuation.resume(true, null)
                }
                provisionQuestionDialog.setNegativeButton("Exit") { dialog, _ ->
                    dialog.dismiss()
                    // close the sample
                    finish()
                }
            }

            // show the provision question dialog
            provisionQuestionDialog.show()
        }

        // Back in coroutine world, we know if the download should happen or not.
        if (shouldDoDownload) {
            // return the Loaded/FailedToLoad status
            this.emitAll(downloadPortalItem(provisionURL, provisionFile))
        } else {
            // using local data, to returns a loaded signal
            this.emit(LoadStatus.Loaded)
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
            val progressBar =
                dialogView.findViewById<LinearProgressIndicator>(R.id.download_spinner)
            progressBar.setProgress(0, false)
            val loadingBuilder = AlertDialog.Builder(this@DownloaderActivity).apply {
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
                this@DownloaderActivity.recreate()
            }
            // emit loading status
            emit(LoadStatus.Loading)
            // delete folder/file prior to downloading data
            provisionLocation.deleteRecursively()
            // get the PortalItem using the provision URL
            val portalItem = PortalItem(provisionURL)
            // load the PortalItem
            val loadResult = portalItem.load()
            loadResult.getOrElse {
                // close dialog and emit status
                loadingDialog.dismiss()
                emit(LoadStatus.FailedToLoad(it))
                return@flow
            }
            // get the data of the PortalItem
            val portalItemData = portalItem.fetchData()
            val byteArray = portalItemData.getOrElse {
                // close dialog and emit status
                loadingDialog.dismiss()
                emit(LoadStatus.FailedToLoad(it))
                return@flow
            }
            // get the byteArray of the PortalItem
            runCatching {
                val byteArrayInputStream = ByteArrayInputStream(byteArray)
                val data = ByteArray(1024)
                var downloadTotal = 0
                var downloadCount: Int
                downloadCount = byteArrayInputStream.read(data)
                while (downloadCount != -1) {
                    downloadTotal += downloadCount
                    val progressPercentage = (downloadTotal * 100 / portalItem.size)
                    downloadCount = byteArrayInputStream.read(data)
                    progressBar.setProgress(progressPercentage.toInt(), true)
                }
                // create file at location to write the PortalItem ByteArray
                provisionLocation.createNewFile()
                // create and write the file output stream
                val fileOutputStream = FileOutputStream(provisionLocation)
                fileOutputStream.write(byteArray)
                // close dialog and emit status
                loadingDialog.dismiss()
                emit(LoadStatus.Loaded)
            }.onFailure {
                // close dialog and emit status
                loadingDialog.dismiss()
                emit(LoadStatus.FailedToLoad(it))
            }
        }
}
