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
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@OptIn(ExperimentalCoroutinesApi::class)
abstract class DownloaderActivity : AppCompatActivity() {

    // set up data binding for the activity
    private val activitySamplesBinding: ActivitySamplesBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_samples)
    }

    /**
     * Gets the [provisionURL] of the portal item to download at
     * the [samplePath], once download completes it starts the [mainActivity]
     */
    fun downloadAndStartSample(
        mainActivity: Intent,
        samplePath: String,
        provisionURLs: List<String>
    ) {
        // start the download manager to automatically add provision files to the app
        // alternatively, you can use ADB/Device File Explorer
        lifecycleScope.launch {
            sampleDownloadManager(provisionURL, samplePath).collect { loadStatus ->
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

        // the provision folder at the destination
        val provisionFolder = File(destinationPath)

        // suspends the coroutine until the dialog is resolved.
        val downloadRequired: Boolean =
            suspendCancellableCoroutine { downloadRequiredContinuation ->
                // set up the alert dialog builder
                val provisionQuestionDialog = AlertDialog.Builder(this@DownloaderActivity)
                    .setTitle("Download data?")

                if (provisionFolder.exists()) {
                    // folder exists, prompt user to download again
                    provisionQuestionDialog.setMessage(getString(R.string.already_provisioned))
                    // if user taps "Re-download" data
                    provisionQuestionDialog.setNeutralButton("Re-download data") { dialog, _ ->
                        // dismiss provision dialog question dialog
                        dialog.dismiss()
                        // set to should download
                        downloadRequiredContinuation.resume(true, null)
                    }
                    // if user taps "Continue" with existing folder
                    provisionQuestionDialog.setPositiveButton("Continue") { dialog, _ ->
                        // dismiss the provision question dialog
                        dialog.dismiss()
                        // set to should not download
                        downloadRequiredContinuation.resume(false, null)
                    }
                }
                // if folder does not exist, ask for download permission
                else {
                    // set require provisioning message
                    provisionQuestionDialog.setMessage(getString(R.string.requires_provisioning))
                    // if user taps "Download" button
                    provisionQuestionDialog.setPositiveButton("Download") { dialog, _ ->
                        // dismiss provision dialog
                        dialog.dismiss()
                        // set to should download
                        downloadRequiredContinuation.resume(true, null)
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
        if (downloadRequired) {
            // return the Loaded/FailedToLoad status
            this.emitAll(downloadPortalItem(provisionURL, provisionFolder))
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
            // emit loading status
            emit(LoadStatus.Loading)
            // delete folder/file prior to downloading data
            FileUtils.cleanDirectory(provisionLocation)
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
                // set up the file at the download path
                val destinationFilePath = provisionLocation.path + File.separator + portalItem.name
                val provisionFile = File(destinationFilePath)
                // create file at location to write the PortalItem ByteArray
                provisionFile.createNewFile()
                // create and write the file output stream
                val writeOutputStream = FileOutputStream(provisionFile)
                writeOutputStream.write(byteArray)

                // unzip the file if it is a .zip
                if (portalItem.name.contains(".zip")) {
                    val fileInputStream = FileInputStream(destinationFilePath)
                    val zipInputStream = ZipInputStream(BufferedInputStream(fileInputStream))
                    var zipEntry: ZipEntry? = zipInputStream.nextEntry
                    val buffer = ByteArray(1024)
                    while (zipEntry != null) {
                        val file = File(provisionLocation.path, zipEntry.name)
                        val fout = FileOutputStream(file)
                        var count = zipInputStream.read(buffer)
                        while (count != -1) {
                            fout.write(buffer, 0, count)
                            count = zipInputStream.read(buffer)
                        }
                        fout.close()
                        zipInputStream.closeEntry()
                        zipEntry = zipInputStream.nextEntry
                    }
                    zipInputStream.close();
                    // delete the .zip file, since unzipping is complete
                    FileUtils.delete(provisionFile)
                }

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
