/* Copyright 2026 Esri
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

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.PortalItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

internal class SampleDownloadEngine {

    suspend fun download(
        provisionUrls: List<String>,
        destinationFolder: File,
        onProgress: (Int?) -> Unit
    ) {
        require(provisionUrls.isNotEmpty()) { "At least one provision URL is required." }

        val stagingFolder = File(destinationFolder.parentFile, "${destinationFolder.name}.partial")
        if (stagingFolder.exists()) {
            FileUtils.deleteDirectory(stagingFolder)
        }
        stagingFolder.mkdirs()

        try {
            provisionUrls.forEachIndexed { index, provisionUrl ->
                coroutineContext.ensureActive()
                downloadPortalItem(
                    provisionUrl = provisionUrl,
                    destinationFolder = stagingFolder,
                    itemIndex = index,
                    itemCount = provisionUrls.size,
                    onProgress = onProgress
                )
            }
            replaceDestination(stagingFolder, destinationFolder)
            onProgress(100)
        } catch (exception: Exception) {
            FileUtils.deleteDirectory(stagingFolder)
            throw exception
        }
    }

    private suspend fun downloadPortalItem(
        provisionUrl: String,
        destinationFolder: File,
        itemIndex: Int,
        itemCount: Int,
        onProgress: (Int?) -> Unit
    ) {
        val portalItem = PortalItem(provisionUrl)
        portalItem.load().getOrThrow()

        val destinationFile = File(destinationFolder, portalItem.name)
        val downloadUrl =
            "${portalItem.portal.url}/sharing/rest/content/items/${portalItem.itemId}/data"

        ArcGISEnvironment.arcGISHttpClient.download(
            url = downloadUrl,
            destinationFile = destinationFile
        ) { totalBytes, bytesRead ->
            val progress = totalBytes?.let {
                (((itemIndex + bytesRead.toDouble() / it) / itemCount) * 100).roundToInt()
            }
            onProgress(progress)
        }.getOrThrow()

        if (portalItem.name.endsWith(".zip", ignoreCase = true)) {
            unzip(destinationFile, destinationFolder)
            FileUtils.delete(destinationFile)
        }
    }

    private fun unzip(zipFile: File, destinationFolder: File) {
        val destinationPath = destinationFolder.canonicalFile.toPath()
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outputFile = File(destinationFolder, entry.name).canonicalFile
                require(outputFile.toPath().startsWith(destinationPath)) {
                    "Invalid ZIP entry: ${entry.name}"
                }
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun replaceDestination(stagingFolder: File, destinationFolder: File) {
        if (destinationFolder.exists()) {
            FileUtils.deleteDirectory(destinationFolder)
        }
        if (!stagingFolder.renameTo(destinationFolder)) {
            FileUtils.copyDirectory(stagingFolder, destinationFolder)
            FileUtils.deleteDirectory(stagingFolder)
        }
    }

    suspend fun delete(destinationFolder: File) {
        withContext(Dispatchers.IO) {
            FileUtils.deleteDirectory(destinationFolder)
            FileUtils.deleteDirectory(
                File(destinationFolder.parentFile, "${destinationFolder.name}.partial")
            )
        }
    }
}