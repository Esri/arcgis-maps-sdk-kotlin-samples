/* Copyright 2024 Esri
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

package com.esri.arcgismaps.kotlin.sampleviewer.model

import android.content.Context
import android.util.Log
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample.Companion.loadActivityPath
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample.Companion.loadReadMe
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample.Companion.loadScreenshot
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.AppDatabase
import com.esri.arcgismaps.kotlin.sampleviewer.model.room.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single source of truth for app wide data. It reads the sample metadata to create as list of
 * [Sample] objects and populates the database used for search.
 * It also provides functions to get samples by category, name, or all samples.
 */
object DefaultSampleInfoRepository : SampleInfoRepository {

    private val isInitialized = AtomicBoolean(false)

    private val json = Json { ignoreUnknownKeys = true }

    private val _sampleData = MutableStateFlow<List<Sample>>(emptyList())

    val sampleData = _sampleData.asStateFlow() // Read-only flow for observing data

    /**
     * Load the sample metadata from the metadata folder in the assets directory and updates sampleList
     * of [Sample] objects.
     */
    suspend fun load(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            // List that will be populated with samples
            val sampleList = mutableListOf<Sample>()
            // Iterate through the metadata folder for all metadata files
            context.assets.list("samples")?.forEach { samplePath ->
                // Get this metadata files as a string
                context.assets.open("samples/$samplePath/README.metadata.json").use { inputStream ->
                    val metadataJsonString = inputStream.bufferedReader().use { it.readText() }
                    try {
                        val metadata = json.decodeFromString<SampleMetadata>(metadataJsonString)

                        // Create and add a new sample metadata data class object to the list
                        val sample = Sample(
                            name = metadata.title,
                            codeFiles = Sample.loadCodeFiles(
                                context = context,
                                sampleName = metadata.title
                            ),
                            url = "https://developers.arcgis.com/kotlin/sample-code/" +
                                    metadata.title.replace(" ", "-").lowercase(),
                            readMe = loadReadMe(
                                context = context,
                                sampleName = samplePath
                            ),
                            screenshotURL = loadScreenshot(
                                sampleName = metadata.title,
                                imageArray = metadata.imagePaths
                            ),
                            mainActivity = loadActivityPath(
                                codePaths = metadata.codePaths
                            ),
                            metadata = metadata,
                        )
                        // Add the new sample to the list
                        sampleList.add(sample)
                    } catch (e: Exception) {
                        Log.e(
                            DefaultSampleInfoRepository::class.simpleName,
                            "Exception at $samplePath: " + e.printStackTrace()
                        )
                    }
                }
            }
            _sampleData.value = sampleList
            withContext(Dispatchers.IO) {
                // Populates the Room SQL database
                populateDatabase(context)
            }
        }
    }

    /**
     * Populates the Room SQL database with all samples and sample info
     */
    private suspend fun populateDatabase(context: Context) {
        val sampleEntities = _sampleData.value.map { Converters().convertToEntity(sample = it) }
        AppDatabase.getDatabase(context).clearAllTables()
        AppDatabase.getDatabase(context).sampleDao().insertAll(samples = sampleEntities)
    }

    /**
     * Get a sample by its name. Either the formal name or title.
     */
    override fun getSampleByName(sampleName: String): Flow<Sample> {
        return _sampleData.map { it.first { sample -> sample.metadata.formalName == sampleName || sample.metadata.title == sampleName } }
    }

    /**
     * Get a list of samples for the given [SampleCategory].
     */
    override fun getSamplesInCategory(sampleCategory: SampleCategory): Flow<List<Sample>> {
        return _sampleData.map { it.filter { sample -> sample.metadata.sampleCategory == sampleCategory } }
    }
}
