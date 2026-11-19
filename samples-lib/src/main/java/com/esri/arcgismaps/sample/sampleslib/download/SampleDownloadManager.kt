package com.esri.arcgismaps.sample.sampleslib.download

import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.FileDownloadTask
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

internal data class DownloadProgress(
    val progress: Int?,
    val itemIndex: Int,
    val itemCount: Int
)

internal class SampleDownloadManager {

    private var downLoadTask : FileDownloadTask? = null

    suspend fun download(
        itemIds: List<String>,
        destinationFolder: File,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val stagingFolder = File(destinationFolder.parentFile, "${destinationFolder.name}.partial")
        if (stagingFolder.exists()) {
            FileUtils.deleteDirectory(stagingFolder)
        }
        stagingFolder.mkdirs()

        try {
            itemIds.forEachIndexed { index, provisionUrl ->
                coroutineContext.ensureActive()
                downloadPortalItem(
                    itemId = provisionUrl,
                    destinationFolder = stagingFolder,
                    itemIndex = index,
                    itemCount = itemIds.size,
                    onProgress = onProgress
                )
            }
            replaceDestination(stagingFolder, destinationFolder)
            onProgress(DownloadProgress(progress = 100, itemIndex = itemIds.lastIndex, itemCount = itemIds.size))
        } catch (exception: Exception) {
            FileUtils.deleteDirectory(stagingFolder)
            throw exception
        }
    }

    private suspend fun downloadPortalItem(
        itemId: String,
        destinationFolder: File,
        itemIndex: Int,
        itemCount: Int,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val portalItem = PortalItem(
            portal = Portal.arcGISOnline(Portal.Connection.Anonymous),
            itemId = itemId
        )
        portalItem.load().getOrThrow()
        val destinationFile = File(destinationFolder, portalItem.name)
        val downloadUrl =
            "${portalItem.portal.url}/sharing/rest/content/items/${portalItem.itemId}/data"

        downLoadTask = ArcGISEnvironment.arcGISHttpClient.downloadWithTask(
            url = downloadUrl,
            destinationFile = destinationFile
        )
        try {
            downLoadTask?.start()

            val progressJob = CoroutineScope(Dispatchers.Main).launch {
                downLoadTask?.progress?.collect { info ->
                    val overallProgress = if (info.totalBytes == null || info.totalBytes == 0L) {
                        null
                    } else {
                        val currentItemProgress = info.bytesRead.toDouble() / info.totalBytes!!.toDouble()
                        (((itemIndex + currentItemProgress) / itemCount) * 100.0).roundToInt()
                    }

                    onProgress(
                        DownloadProgress(
                            progress = overallProgress,
                            itemIndex = itemIndex,
                            itemCount = itemCount
                        )
                    )
                }
            }

            downLoadTask?.result()?.getOrThrow()
            progressJob.cancel()

            downLoadTask?.result()?.getOrThrow()
        } finally {
            downLoadTask = null
        }

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
                // Skip the duplicate named file to avoid exception
                if (outputFile.canonicalFile == zipFile.canonicalFile) {
                    return@forEach
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

    fun cancelDownload(){
        try {
            downLoadTask?.cancel()
        } finally {
            downLoadTask = null
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