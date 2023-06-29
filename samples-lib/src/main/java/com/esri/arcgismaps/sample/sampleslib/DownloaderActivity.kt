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

package com.esri.arcgismaps.sample.sampleslib

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.PortalItem
import com.esri.arcgismaps.sample.sampleslib.databinding.ActivitySamplesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
abstract class DownloaderActivity : AppCompatActivity() {

    /**
     * Returns the location of the download folder based on the [appName]
     */
    private fun getDownloadFolder(appName: String): String {
        return getExternalFilesDir(null)?.path.toString() + File.separator + appName
    }

    // set up data binding for the activity
    private val activitySamplesBinding: ActivitySamplesBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_samples)
    }

    /**
     * Gets the [provisionURLs] of the portal item to download at
     * the [sampleName], once download completes it starts the [mainActivity]
     */
    fun downloadAndStartSample(
        mainActivity: Intent,
        sampleName: String,
        provisionURLs: List<String>
    ) {
        // get the path to download files
        val samplePath: String = getDownloadFolder(sampleName)
        // start the download manager to automatically add the .mmpk file to the app
        // alternatively, you can use ADB/Device File Explorer
        lifecycleScope.launch {
            sampleDownloadManager(provisionURLs, samplePath).collect { loadStatus ->
                if (loadStatus is LoadStatus.Loaded) {
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
     * If no provisioned data exists it performs a download using the [provisionURLs]
     * at the [destinationPath]. The downloadManager clears the directory if user chooses
     * to re-download the provisioned data.
     */
    private suspend fun sampleDownloadManager(
        provisionURLs: List<String>,
        destinationPath: String,
    ): Flow<LoadStatus> = flow {

        // the provision folder at the destination
        val provisionFolder = File(destinationPath)
        if (!provisionFolder.exists()) {
            provisionFolder.mkdirs()
        }

        // suspends the coroutine until the dialog is resolved.
        val downloadRequired: Boolean =
            suspendCancellableCoroutine { downloadRequiredContinuation ->
                // set up the alert dialog builder
                val provisionQuestionDialog = MaterialAlertDialogBuilder(this@DownloaderActivity)
                    .setTitle("Download data?")

                if (provisionFolder.list()?.isNotEmpty() == true) {
                    // folder is not empty, prompt user to download again
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

        // check if the download should happen or not.
        if (downloadRequired) {
            // create a list to collect the status of each portal item download
            val loadStatusResult = mutableListOf<LoadStatus>()
            // download the portal item for each provisionURL
            provisionURLs.asFlow().flatMapMerge { downloadPortalItem(it, provisionFolder) }
                .collect { loadStatus ->
                    // add the result of each download to the mutable list
                    loadStatusResult.add(loadStatus)
                    // check if we have all the results added
                    if (loadStatusResult.size == provisionURLs.size) {
                        // loop through to see if download has succeeded
                        loadStatusResult.forEach {
                            // emit failed if a download failed
                            if (it is LoadStatus.FailedToLoad) {
                                this.emit(it)
                                return@collect
                            }
                        }
                        // emit Loaded since download succeeded
                        this.emit(LoadStatus.Loaded)
                        return@collect
                    }
                }

        } else {
            // using local data, to returns a loaded signal
            this.emit(LoadStatus.Loaded)
        }
    }

    /**
     * Handles the download process using the [provisionURL] at the [provisionLocation].
     */
    private fun downloadPortalItem(
        provisionURL: String,
        provisionLocation: File
    ): Flow<LoadStatus> =
        flow {

            // build another dialog to show the progress of the download
            val dialogView: View = layoutInflater.inflate(R.layout.download_dialog, null)
            val loadingBuilder = MaterialAlertDialogBuilder(this@DownloaderActivity).apply {
                setView(dialogView)
                create()
            }
            // download progress indicator layout
            val downloadProgressLayout = dialogView.findViewById<View>(R.id.downloadLayout)
            // show progress indicator for determinate downloads
            val progressIndicator: LinearProgressIndicator =
                dialogView.findViewById(R.id.downloadProgressIndicator)
            // show circular spinner for indeterminate downloads
            val circularSpinner: CircularProgressIndicator =
                dialogView.findViewById(R.id.downloadCircularIndicator)
            // display a percentage text of the download progress
            val progressTV: TextView = dialogView.findViewById(R.id.downloadProgressTV)

            // show the loading dialog
            val loadingDialog = loadingBuilder.show()
            // set cancel button for the loading dialog
            val cancelButton = dialogView.findViewById<View>(R.id.cancelButton)
            cancelButton.setOnClickListener {
                loadingDialog.dismiss()
                //re-start sample to show the dialog again
                this@DownloaderActivity.recreate()
            }
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
            // set up the file at the download path
            val destinationFile = File(provisionLocation.path, portalItem.name)
            // set up the download URL
            val downloadURL =
                "${portalItem.portal.url}/sharing/rest/content/items/${portalItem.itemId}/data"
            // get the data of the PortalItem
            ArcGISEnvironment.arcGISHttpClient.download(
                url = downloadURL,
                destinationFile = destinationFile
            ) { totalBytes, bytesRead ->
                if (totalBytes != null) {
                    val percentage = ((100.0 * bytesRead) / totalBytes).roundToInt()
                    lifecycleScope.launch(Dispatchers.Main) {
                        // show the download progress layout
                        downloadProgressLayout.visibility = View.VISIBLE
                        circularSpinner.visibility = View.GONE
                        progressIndicator.progress = percentage
                        progressTV.text = "$percentage%"
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        // show the indeterminate loading spinner
                        downloadProgressLayout.visibility = View.GONE
                        circularSpinner.visibility = View.VISIBLE
                    }
                }
            }.onSuccess {
                // unzip the file if it is a .zip
                if (portalItem.name.contains(".zip")) {
                    // set up the input streams
                    FileInputStream(destinationFile).use { fileInputStream ->
                        ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
                            var zipEntry: ZipEntry? = zipInputStream.nextEntry
                            val buffer = ByteArray(1024)
                            while (zipEntry != null) {
                                if (zipEntry.isDirectory) {
                                    File(provisionLocation.path, zipEntry.name).mkdirs()
                                } else {
                                    val file = File(provisionLocation.path, zipEntry.name)
                                    FileOutputStream(file).use { fileOutputStream ->
                                        var count = zipInputStream.read(buffer)
                                        while (count != -1) {
                                            fileOutputStream.write(buffer, 0, count)
                                            count = zipInputStream.read(buffer)
                                        }
                                    }
                                }
                                // close this entry, and move to the next zipped file
                                zipInputStream.closeEntry()
                                zipEntry = zipInputStream.nextEntry
                            }
                            // delete the .zip file, since unzipping is complete
                            FileUtils.delete(destinationFile)
                        }
                    }
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
