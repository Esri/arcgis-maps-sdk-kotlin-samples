package com.arcgismaps

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Packages md, kt, and json files (with the name of the associated sample)
 * into the app's build assets directory.
 */
class CopySampleFilesTask : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<GenerateSampleAssetsTask>("copyCodeFiles") {
            description = "Generates the sample source, README and metadata JSON asset."
            samplesDirectory.set(project.rootProject.layout.projectDirectory.dir("samples"))
            outputFile.set(project.layout.buildDirectory.file("sampleAssets/samples/samples.json"))
        }
    }

    @CacheableTask
    abstract class GenerateSampleAssetsTask : DefaultTask() {
        private val json = Json { prettyPrint = true }

        @get:Internal
        abstract val samplesDirectory: DirectoryProperty

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val sampleFiles: FileTree
            get() = samplesDirectory.asFileTree.matching {
                exclude("**/build/")
                include("**/*.md", "**/*.kt", "**/*.metadata.json")
            }

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        @TaskAction
        fun generate() {
            val outputDir = outputFile.get().asFile.parentFile
            java.nio.file.Files.createDirectories(outputDir.toPath())
            createJsonAssetFile(outputDir)
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
            val samplesMap: MutableMap<String, MutableMap<String, String>> = sortedMapOf()
            val samplesRoot = samplesDirectory.get().asFile
            sampleFiles.files
                .sortedBy { it.relativeTo(samplesRoot).invariantSeparatorsPath }
                .forEach { file ->
                    val sampleName = file.relativeTo(samplesRoot).invariantSeparatorsPath.substringBefore("/")
                    val filesMap = samplesMap.getOrPut(sampleName) { sortedMapOf() }
                    // Preserve flat filenames with deterministic first-match handling of duplicates.
                    if (file.name !in filesMap) {
                        filesMap[file.name] = file.readText()
                    }
                }

            // Serialize using Kotlin Serialization with pretty printing
            val jsonString = json.encodeToString(samplesMap)

            // Write the JSON to a file named "samples.json" in outputDir
            val jsonFile = File(outputDir, outputFile.get().asFile.name)
            jsonFile.writeText(jsonString)
        }
    }
}
