package com.arcgismaps

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Copy md, kt, and json files to a directory (with the name of the associated sample)
 * into the app's build assets directory.
 */
class CopySampleFilesTask : Plugin<Project> {

    private val json = Json { prettyPrint = true }

    override fun apply(project: Project) {
        project.tasks.register<Copy>("copyCodeFiles") {
            description = """
                        Copies sample files for the given sample directory if a code file doesn't
                        already exist or a newer version is present.
                        """.trimIndent()

            // Define the input files
            val inputFiles: FileCollection = project.fileTree("${project.rootDir.path}/samples/") {
                exclude("**/build/")
                include("**/*.md", "**/*.kt", "**/*.metadata.json")
            }

            // Set input files
            inputs.files(inputFiles)

            // Define the output directory
            val outputDir = project.file("${project.rootDir.path}/app/build/sampleAssets/samples/")
            outputs.dir(outputDir)

            from(inputFiles) {
                eachFile {
                    path = path.substringBefore("/") + "/" + name
                    includeEmptyDirs = false
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }

            // Place in the assets codeFiles folder of the app.
            into(outputDir)

            // After the copy finishes, run JSON generation + cleanup
            doLast {
                // Create a JSON file that maps each sample folder’s files to their contents
                createJsonAssetFile(outputDir)

                // Delete everything but the JSON file itself
                outputDir.listFiles()?.forEach { file ->
                    if (file.isDirectory || file.name != "samples.json") {
                        file.deleteRecursively()
                    }
                }
            }
        }
    }

    /**
     * Creates the main list of all samples as "samples.json" file in [outputDir] with structure:
     *
     * {
     *   "sampleA": {
     *     "MainActivity.kt": "...content...",
     *     "README.md": "...content...",
     *     ...
     *   },
     *   "sampleB": {
     *      ...
     *   }
     * }
     */
    private fun createJsonAssetFile(outputDir: File) {
        // This map will hold the structure: sampleDirName -> { filename -> fileContent }
        val samplesMap = mutableMapOf<String, MutableMap<String, String>>()

        // List subdirectories in outputDir (i.e., each sample folder)
        outputDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?.forEach { sampleDir ->
                val filesMap = mutableMapOf<String, String>()
                sampleDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        filesMap[file.name] = file.readText()
                    }
                }
                samplesMap[sampleDir.name] = filesMap
            }

        // Serialize using Kotlin Serialization with pretty printing
        val jsonString = json.encodeToString(samplesMap)

        // Write the JSON to a file named "samples.json" in outputDir
        val jsonFile = File(outputDir, "samples.json")
        jsonFile.writeText(jsonString)
    }
}
