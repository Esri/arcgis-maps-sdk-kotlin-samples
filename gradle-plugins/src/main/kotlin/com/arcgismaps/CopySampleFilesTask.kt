package com.arcgismaps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

/**
 * Copy md, kt, and json files to a directory (with the name of the associated sample)
 * into the app's build assets directory.
 */
class CopySampleFilesTask : Plugin<Project> {
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
        }
    }
}