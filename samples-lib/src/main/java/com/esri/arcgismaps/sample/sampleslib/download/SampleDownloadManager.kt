package com.esri.arcgismaps.sample.sampleslib.download

import android.content.Context
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.FileDownloadTask
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.io.FileUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

internal data class DownloadProgress(
    val progress: Int?,
    val itemIndex: Int,
    val itemCount: Int
)

class SampleDownloadManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Semaphore(1)
    private val lock = Any()
    private val jobs = mutableMapOf<String, Deferred<Unit>>()
    private var activeSampleId: String? = null
    private var downLoadTask: FileDownloadTask? = null

    private val _statuses = MutableStateFlow(loadCompletedStatuses())
    val statuses: StateFlow<Map<String, DownloadUiState>> = _statuses.asStateFlow()

    fun requestDownload(sampleId: String, sampleName: String, itemIds: List<String>): Deferred<Unit> {
        require(itemIds.isNotEmpty()) { "A sample download requires at least one item ID." }

        synchronized(lock) {
            jobs[sampleId]?.takeIf { it.isActive }?.let { return it }

            _statuses.update { it + (sampleId to DownloadUiState.Queued) }
            val job = scope.async {
                try {
                    queue.withPermit {
                        if (isComplete(sampleId, itemIds, destinationFor(sampleName))) {
                            _statuses.update { it + (sampleId to DownloadUiState.Downloaded) }
                            return@withPermit
                        }

                        clearCompletion(sampleId)
                        synchronized(lock) { activeSampleId = sampleId }
                        try {
                            download(itemIds, destinationFor(sampleName)) { progress ->
                                _statuses.update {
                                    it + (sampleId to DownloadUiState.Downloading(
                                        progress = progress.progress,
                                        itemIndex = progress.itemIndex,
                                        itemCount = progress.itemCount
                                    ))
                                }
                            }
                            saveCompletion(sampleId, itemIds, destinationFor(sampleName))
                            _statuses.update { it + (sampleId to DownloadUiState.Downloaded) }
                        } finally {
                            synchronized(lock) { activeSampleId = null }
                        }
                    }
                } catch (cancellation: CancellationException) {
                    _statuses.update { it + (sampleId to DownloadUiState.NotDownloaded) }
                    throw cancellation
                } catch (exception: Exception) {
                    _statuses.update {
                        it + (sampleId to DownloadUiState.Failed(
                            exception.message ?: "Unable to download sample data."
                        ))
                    }
                    throw exception
                }
            }
            jobs[sampleId] = job
            job.invokeOnCompletion {
                synchronized(lock) {
                    if (jobs[sampleId] === job) jobs.remove(sampleId)
                }
            }
            return job
        }
    }

    suspend fun refreshStatus(sampleId: String, sampleName: String, itemIds: List<String>) {
        val state = _statuses.value[sampleId]
        if (state is DownloadUiState.Downloading || state == DownloadUiState.Queued) return

        val complete = withContext(Dispatchers.IO) {
            isComplete(sampleId, itemIds, destinationFor(sampleName))
        }
        synchronized(lock) {
            if (jobs[sampleId]?.isActive == true) return
            val latestState = _statuses.value[sampleId]
            if (latestState == DownloadUiState.Queued || latestState is DownloadUiState.Downloading) return
            _statuses.update {
                it + (sampleId to if (complete) DownloadUiState.Downloaded else DownloadUiState.NotDownloaded)
            }
        }
    }

    suspend fun cancelDownload(sampleId: String) {
        val job = synchronized(lock) {
            jobs[sampleId]?.takeIf { it.isActive }?.also {
                if (activeSampleId == sampleId) downLoadTask?.cancel()
            }
        }
        job?.cancelAndJoin()
    }

    private fun destinationFor(sampleName: String) =
        File(appContext.getExternalFilesDir(null), sampleName)

    private suspend fun download(
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
        coroutineContext.ensureActive()
        val destinationFile = File(destinationFolder, portalItem.name)
        val downloadUrl =
            "${portalItem.portal.url}/sharing/rest/content/items/${portalItem.itemId}/data"

        val task = ArcGISEnvironment.arcGISHttpClient.downloadWithTask(
            url = downloadUrl,
            destinationFile = destinationFile
        )
        synchronized(lock) { downLoadTask = task }
        try {
            coroutineContext.ensureActive()
            val progressJob = CoroutineScope(coroutineContext).launch(Dispatchers.Main) {
                task.progress.collect { info ->
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

            try {
                task.start()
                task.result().getOrThrow()
            } finally {
                progressJob.cancelAndJoin()
            }
        } finally {
            synchronized(lock) {
                if (downLoadTask === task) downLoadTask = null
            }
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

    private fun saveCompletion(sampleId: String, itemIds: List<String>, destination: File) {
        val root = destination.canonicalFile
        val files = JSONArray()
        destination.walkTopDown().filter(File::isFile).forEach { file ->
            val canonicalFile = file.canonicalFile
            require(canonicalFile.toPath().startsWith(root.toPath())) {
                "Downloaded file escaped the sample data directory."
            }
            files.put(
                JSONObject()
                    .put("path", file.relativeTo(destination).invariantSeparatorsPath)
                    .put("length", file.length())
            )
        }
        require(files.length() > 0) { "The download completed without producing any files." }
        val record = JSONObject()
            .put("itemIds", JSONArray(itemIds))
            .put("files", files)
        val manifest = manifestFile(sampleId)
        manifest.parentFile?.mkdirs()
        val temporaryManifest = File(manifest.parentFile, "${manifest.name}.tmp")
        FileOutputStream(temporaryManifest).use { output ->
            output.write(record.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (manifest.exists()) check(manifest.delete()) { "Unable to replace sample download manifest." }
        check(temporaryManifest.renameTo(manifest)) { "Unable to publish sample download manifest." }
        check(preferences.edit().putString(completionKey(sampleId), JSONArray(itemIds).toString()).commit()) {
            "Unable to save sample download status."
        }
    }

    private fun isComplete(sampleId: String, itemIds: List<String>, destination: File): Boolean {
        val marker = preferences.getString(completionKey(sampleId), null) ?: return false
        return try {
            val markerArray = JSONArray(marker)
            val recordedIds = List(markerArray.length()) { index -> markerArray.getString(index) }
            if (recordedIds != itemIds) return false

            val manifest = manifestFile(sampleId)
            if (!manifest.isFile) return false
            val record = JSONObject(manifest.readText())
            val manifestIds = record.getJSONArray("itemIds").let { array ->
                List(array.length()) { index -> array.getString(index) }
            }
            if (manifestIds != itemIds) return false

            val root = destination.canonicalFile
            val files = record.getJSONArray("files")
            files.length() > 0 && (0 until files.length()).all { index ->
                val fileRecord = files.getJSONObject(index)
                val file = File(root, fileRecord.getString("path")).canonicalFile
                file.toPath().startsWith(root.toPath()) &&
                    file.isFile && file.length() == fileRecord.getLong("length")
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun loadCompletedStatuses(): Map<String, DownloadUiState> {
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(COMPLETION_PREFIX) || value !is String) return@mapNotNull null
            val sampleId = key.removePrefix(COMPLETION_PREFIX)
            try {
                JSONArray(value)
                sampleId to DownloadUiState.Downloaded
            } catch (_: Exception) {
                null
            }
        }.toMap()
    }

    private fun clearCompletion(sampleId: String) {
        preferences.edit().remove(completionKey(sampleId)).commit()
        manifestFile(sampleId).delete()
    }

    private fun manifestFile(sampleId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sampleId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(File(appContext.filesDir, MANIFEST_DIRECTORY), "$digest.json")
    }

    private fun completionKey(sampleId: String) = "$COMPLETION_PREFIX$sampleId"

    companion object {
        private const val PREFERENCES_NAME = "sample_download_status"
        private const val COMPLETION_PREFIX = "completed_"
        private const val MANIFEST_DIRECTORY = "sample_download_manifests"

        @Volatile
        private var instance: SampleDownloadManager? = null

        fun getInstance(context: Context): SampleDownloadManager =
            instance ?: synchronized(this) {
                instance ?: SampleDownloadManager(context.applicationContext).also { instance = it }
            }
    }
}