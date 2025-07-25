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

package com.esri.arcgismaps.kotlin.sampleviewer.model.room

import androidx.room.TypeConverter
import com.esri.arcgismaps.kotlin.sampleviewer.model.CodeFile
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import kotlinx.serialization.json.Json

/**
 * Room cannot convert complex data types like [CodeFile] or [List],
 * thus, this class helps to convert them into JSON [String] format
 */
class Converters {

    private val json = Json { encodeDefaults = true }

    @TypeConverter
    fun fromCodeFileList(codeFiles: List<CodeFile>): String {
        return codeFiles.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toCodeFileList(jsonString: String): List<CodeFile> {
        return jsonString.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromRelevantApiList(relevantApi: List<String>): String {
        return relevantApi.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toRelevantApiList(relevantApiJsonString: String): List<String> {
        return relevantApiJsonString.let { json.decodeFromString(it) }
    }

    // Converts Sample to SampleEntity in order to select relevant fields
    fun convertToEntity(sample: Sample): SampleEntity {
        return SampleEntity(
            sampleName = sample.name,
            sampleCodeFile = sample.codeFiles,
            sampleReadMe = sample.readMe,
            sampleRelevantApi = sample.metadata.relevantApis
        )
    }

}
