package com.esri.arcgismaps.kotlin.sampleviewer.model.room

import androidx.room.TypeConverter
import com.esri.arcgismaps.kotlin.sampleviewer.model.CodeFile
import com.esri.arcgismaps.kotlin.sampleviewer.model.Sample
import kotlinx.serialization.encodeToString
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

    // Converts Sample to SampleEntity in order to select certain relevant fields
    fun convertToEntity(sample: Sample): SampleEntity {
        return SampleEntity(
            sampleName = sample.name,
            sampleCodeFile = sample.codeFiles,
            sampleReadMe = sample.readMe,
            sampleRelevantApi = sample.metadata.relevantApis
        )
    }

}
