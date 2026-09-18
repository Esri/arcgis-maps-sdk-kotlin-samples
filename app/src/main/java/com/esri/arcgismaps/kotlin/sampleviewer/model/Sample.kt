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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.esri.arcgismaps.kotlin.sampleviewer.BuildConfig
import com.esri.arcgismaps.sample.sampleslib.DownloaderActivity
import kotlinx.serialization.Serializable

/**
 * Holds information about a sample.
 */
@Serializable
data class Sample(
    val id: String,
    val name: String,
    val mainActivity: String,
    val codeFiles: List<CodeFile>,
    val readMe: String,
    val screenshotURL: String,
    val url: String,
    val offlineDataUrls: List<String>,
    val metadata: SampleMetadata,
    val isFavorite: Boolean = false,
    var score: Double = 0.0
) {
    companion object {
        val PREVIEW_INSTANCE = Sample(
            id = "",
            name = "Analyze hotspots",
            codeFiles = listOf(CodeFile("", "")),
            url = "",
            offlineDataUrls = emptyList(),
            readMe = "",
            screenshotURL = "",
            metadata = SampleMetadata(
                description = "",
                formalName = "Analyze hotspots",
                ignore = false,
                imagePaths = listOf(""),
                keywords = listOf(""),
                relevantApis = listOf(""),
                codePaths = listOf(""),
                sampleCategory = SampleCategory.ANALYSIS,
                offlineDataUrls = listOf(""),
                title = "Analyze hotspots"
            ),
            isFavorite = false,
            mainActivity = ""
        )

        /**
         * Returns a list of [CodeFile] objects for the given sample [fileMap].
         */
        fun loadCodeFiles(fileMap: Map<String, String>): List<CodeFile> {
            val codeFiles = mutableListOf<CodeFile>()
            fileMap.forEach { (fileName, fileContent) ->
                if (fileName.endsWith(".kt", ignoreCase = true)) {
                    codeFiles.add(CodeFile(name = fileName, code = fileContent))
                }
            }
            return codeFiles
        }

        /**
         * Returns the readme for a given sample [fileMap].
         */
        fun loadReadMe(fileMap: Map<String, String>): String {
            val readMeString = fileMap["README.md"]
                ?: throw Exception("README.md not found in sample.")
            // Remove screenshot markdown text from the README
            return readMeString.lines().filterNot { it.contains("![") }.joinToString("\n")
        }

        /**
         * Returns the screenshot URL for a given sample name.
         */
        fun loadScreenshot(
            sampleName: String, imageArray: List<String>,
        ): String {
            // Assuming imageArray will always have one image.
            // Otherwise, function should be modified to return list of URLs for each image
            val modifiedJsonSampleName = sampleName.replace(" ", "-").lowercase()
            val imageFileName = imageArray.first().replace("\"", "")
            return "https://raw.githubusercontent.com/Esri/arcgis-maps-sdk-kotlin-samples/v.next/samples/$modifiedJsonSampleName/$imageFileName"
        }

        /**
         * Returns the path to the sample's MainActivity.
         */
        fun loadActivityPath(codePaths: List<String>): String {
            val mainActivityPath = codePaths.find { it.endsWith("MainActivity.kt") }
                ?: error("MainActivity.kt not found in sample metadata.")
            return mainActivityPath.toActivityClassName()
        }

        private fun String.toActivityClassName(): String {
            return replace('\\', '/')
                .removePrefix("src/main/java/")
                .removeSuffix(".kt")
                .replace('/', '.')
        }
    }
}

/**
 * Starts the sample activity.
 */
suspend fun Sample.start(context: Context) {
    // Revoke previously configured OAuth tokens and credentials.
    AuthenticatorState().signOut()

    // Configure the ArcGIS API key.
    ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.ACCESS_TOKEN)

    val sampleActivity = context.getActivityOrNull() ?: return

    val mainActivityClass = Class.forName(mainActivity)

    val launchIntent = if (offlineDataUrls.isEmpty()) {
        // This sample does not require downloaded data.
        Intent(sampleActivity, mainActivityClass)
    } else {
        // This sample requires offline data.
        DownloaderActivity.createIntent(
            context = sampleActivity,
            sampleId = id,
            sampleName = name,
            provisionUrls = offlineDataUrls,
            mainActivityClassName = mainActivity
        )
    }

    sampleActivity.startActivity(launchIntent)
}

/**
 * Returns the activity from the context.
 */
fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
