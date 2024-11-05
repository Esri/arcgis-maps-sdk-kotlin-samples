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
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authentication.signOut
import kotlinx.serialization.Serializable

/**
 * Holds information about a sample.
 */
@Serializable
data class Sample(
    val name: String,
    val mainActivity: String,
    val codeFiles: List<CodeFile>,
    val readMe: String,
    val screenshotURL: String,
    val url: String,
    val metadata: SampleMetadata,
    val isFavorite: Boolean = false,
    var score: Double = 0.0
) {
    companion object {
        val PREVIEW_INSTANCE = Sample(
            name = "Analyze hotspots",
            codeFiles = listOf(CodeFile("", "")),
            url = "",
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
                title = "Analyze hotspots"
            ),
            isFavorite = false,
            mainActivity = ""
        )

        /**
         * Returns a list of [CodeFile] objects for the given sample name.
         */
        fun loadCodeFiles(context: Context, sampleName: String): List<CodeFile> {
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
        fun loadReadMe(context: Context, sampleName: String): String {
            // Get this metadata files as a string
            context.assets.open("samples/$sampleName/README.md").use { inputStream ->
                val readMeString = inputStream.bufferedReader().use { it.readText() }
                // Remove screenshot markdown text from the README
                return readMeString.lines().filterNot { it.contains("![") }.joinToString("\n")
            }
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
            val imageFileName = imageArray.first().toString().replace("\"", "")
            return "https://raw.githubusercontent.com/Esri/arcgis-maps-sdk-kotlin-samples/v.next/$modifiedJsonSampleName/$imageFileName"
        }

        /**
         * Return's a path to DownloadActivity if one exists, otherwise returns the path to
         * MainActivity.
         */
        fun loadActivityPath(codePaths: List<String>): String {
            // Return a path to DownloadActivity if one exists
            codePaths.find { it.contains("DownloadActivity.kt") }?.let { samplePath ->
                val activityPath = samplePath
                    .substring(14, samplePath.indexOf("."))
                    .replace("/".toRegex(), ".")
                    .replace("\\", "")
                return activityPath
            }

            // Otherwise return the path the MainActivity
            codePaths.find { it.contains("MainActivity.kt") }.apply {
                val samplePath = this.toString()
                val activityPath = samplePath
                    .substring(14, samplePath.indexOf("."))
                    .replace("/".toRegex(), ".")
                    .replace("\\", "")
                return activityPath
            }
        }
    }
}

/**
 * Starts the sample activity.
 */
suspend fun Sample.start(context: Context) {
    // Revoke previously configured ArcGISEnvironment settings like ApiKeys/OAuth tokens/Credentials
    ArcGISEnvironment.authenticationManager.signOut()
    ArcGISEnvironment.apiKey = null
    // Obtain and launch the sample activity
    val className = Class.forName(mainActivity) as Class<*>
    val sampleLauncherActivity = context.getActivityOrNull() ?: return
    sampleLauncherActivity.startActivity(Intent(sampleLauncherActivity, className))
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
