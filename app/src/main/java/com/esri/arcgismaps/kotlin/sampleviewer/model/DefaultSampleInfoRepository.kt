package com.esri.arcgismaps.kotlin.sampleviewer.model

import android.content.Context
import android.util.Log
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.AppDatabase
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DefaultSampleInfoRepository : SampleInfoRepository {

    private val sampleList = mutableListOf<Sample>()
    private val sampleListMutex = Mutex()

    private suspend fun updateSampleList(block: MutableList<Sample>.() -> Unit) {
        sampleListMutex.withLock { sampleList.block() }
    }

    /**
     * Load the sample metadata from the metadata folder in the assets directory and updates sampleList
     * of [Sample] objects.
     */
    suspend fun load(context: Context) {

        // Only run load if sample's list hasn't already been populated
        if (sampleList.isEmpty()) {
            // Iterate through the metadata folder for all metadata files
            val json = Json { ignoreUnknownKeys = true }
            context.assets.list("samples")?.forEach { samplePath ->
                // Get this metadata files as a string
                context.assets.open("samples/$samplePath/README.metadata.json").use { inputStream ->
                    val metadataJsonString = inputStream.bufferedReader().use { it.readText() }
                    // Get this metadata file as a JSON object
                    val jsonObject = json.parseToJsonElement(metadataJsonString).jsonObject
                    try {
                        // Create and add a new sample metadata data class object to the list
                        val sampleItem = Sample(
                            name = jsonObject["title"]!!.jsonPrimitive.content,
                            codeFiles = loadCodeFiles(
                                context = context,
                                sampleName = jsonObject["title"]!!.jsonPrimitive.content
                            ),
                            url = "https://developers.arcgis.com/kotlin/sample-code/" +
                                    jsonObject["title"]!!.jsonPrimitive.content
                                        .replace(" ", "-").lowercase(),
                            readMe = loadReadMe(
                                context = context,
                                sampleName = samplePath
                            ),
                            screenshotURL = loadScreenshot(
                                sampleName = jsonObject["title"]!!.jsonPrimitive.content,
                                imageArray = jsonObject["images"]!!.jsonArray
                            ),
                            mainActivity = loadActivityPath(
                                snippets = jsonObject["snippets"]!!.jsonArray.map { it.jsonPrimitive.content }
                            ),
                            metadata = json.decodeFromString<SampleMetadata>(metadataJsonString),
                        )

                        updateSampleList {
                            add(sampleItem)
                        }

                    } catch (e: Exception) {
                        Log.e(
                            DefaultSampleInfoRepository::class.simpleName,
                            "Exception at $samplePath: " + e.printStackTrace()
                        )
                    }
                }
            }

            withContext(Dispatchers.IO) {
                // Populates the Room SQL database
                populateDatabase(context)
            }
        }
    }

    /**
     * Returns a list of [CodeFile] objects for the given sample name.
     */
    private fun loadCodeFiles(context: Context, sampleName: String): List<CodeFile> {
        // List of code files to be populated
        val codeFiles = mutableListOf<CodeFile>()

        // Code file folders stored in assets directory as kebab case
        val sampleNameKebabCase = sampleName.replace(" ", "-").lowercase()

        val sampleAssetFiles = context.assets.list("samples/$sampleNameKebabCase/")
        // Get the code files from sub-directories (components/, screens/)
        sampleAssetFiles?.forEach { sampleAssetFile ->
            if (sampleAssetFile.contains(".kt")) {
                val codeString = context.assets.open(
                    /* fileName = */ "samples/$sampleNameKebabCase/$sampleAssetFile"
                ).bufferedReader().use { it.readText() }
                codeFiles.add(
                    CodeFile(
                        name = sampleAssetFile,
                        code = codeString
                    )
                )
            }
        }

        return codeFiles
    }

    /**
     * Returns the readme for a given sample name.
     */
    private fun loadReadMe(context: Context, sampleName: String): String {
        // Get this metadata files as a string
        context.assets.open("samples/$sampleName/README.md").use { inputStream ->
            val readMeString = inputStream.bufferedReader().use { it.readText() }
            return readMeString
        }
    }

    /**
     * Returns the screenshot URL for a given sample name.
     */
    private fun loadScreenshot(
        sampleName: String, imageArray: JsonArray,
    ): String {
        // Assuming imageArray will always have one image.
        // Otherwise, function should be modified to return list of URLs for each image
        val modifiedJsonSampleName = sampleName.replace(" ", "-").lowercase()
        val imageFileName = imageArray.first().toString().replace("\"", "")
        return "https://raw.githubusercontent.com/Esri/arcgis-maps-sdk-kotlin-samples/v.next/$modifiedJsonSampleName/$imageFileName"
    }

    /**
     * Return's a path to DownloadActivity if one exists, otherwise returns the path to
     * MainActivity.
     */
    private fun loadActivityPath(snippets: List<String>): String {
        // Return a path to DownloadActivity if one exists
        snippets.find { it.contains("DownloadActivity.kt") }?.let { samplePath ->
            val activityPath = samplePath
                .substring(14, samplePath.indexOf("."))
                .replace("/".toRegex(), ".")
                .replace("\\", "")
            return activityPath
        }

        // Otherwise return the path the MainActivity
        snippets.find { it.contains("MainActivity.kt") }.apply {
            val samplePath = this.toString()
            val activityPath = samplePath
                .substring(14, samplePath.indexOf("."))
                .replace("/".toRegex(), ".")
                .replace("\\", "")
            return activityPath
        }
    }

    /**
     * Get a sample by its name. Either the formal name or title.
     */
    override fun getSampleByName(sampleName: String): Sample {
        return sampleList.first { it.metadata.formalName == sampleName || it.metadata.title == sampleName }
    }

    /**
     * Get a list of samples for the given [SampleCategory].
     */
    override fun getSamplesIn(sampleCategory: SampleCategory): List<Sample> {
        return sampleList.filter { it.metadata.sampleCategory == sampleCategory }
    }

    /**
     * Get a list of samples for the given category string.
     */
    override fun getSamplesIn(sampleCategoryString: String): List<Sample> {
        return sampleList.filter { it.metadata.sampleCategory.text == sampleCategoryString }
    }

    /**
     * Get a list of all samples in the app.
     */
    override fun getAllSamples(): List<Sample> {
        return sampleList
    }

    /**
     * Populates the Room SQL database with all samples and sample info
     */
    private suspend fun populateDatabase(context: Context) {
        val sampleEntities = sampleList.map { Converters().convertToEntity(sample = it) }
        // TODO #4682 - Needed to clear all tables before launch otherwise we build up duplicate entries
        AppDatabase.getDatabase(context).clearAllTables()
        AppDatabase.getDatabase(context).sampleDao().insertAll(samples = sampleEntities)
    }
}
