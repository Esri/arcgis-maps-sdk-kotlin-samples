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

package com.esri.arcgismaps.newmodule

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

/**
 * This Kotlin file creates a new sample and configures it as a new library module in Android Studio.
 * The IntelliJ project creates an .jar artifact which is used to create new samples.
 */
fun main() {
    run()
}

private var sampleName: String = ""
private var sampleWithHyphen: String = ""
private var sampleWithoutSpaces: String = ""
private var samplesRepoPath: String = ""
private var sampleNameUnderscore: String = ""
private var sampleNameCamelCase: String = ""
private var sampleCategory: String = "Maps"

fun run() {
    val scanner = Scanner(System.`in`)

    // Get the name of the sample
    println("Enter Name of the sample with spaces (Eg. \"Display new map\"): ")
    sampleName = scanner.nextLine().trim()

    sampleWithHyphen = sampleName.replace(" ", "-").lowercase(Locale.getDefault())
    sampleWithoutSpaces = sampleName.replace(" ", "").lowercase(Locale.getDefault())
    sampleNameUnderscore = sampleName.replace(" ", "_").lowercase(Locale.getDefault())
    sampleNameCamelCase = sampleName.trim().toUpperCamelCase()

    // Get the sample category
    println("Choose the sample category: \n1:  Analysis \n2:  Augmented Reality \n3:  Cloud and Portal \n4:  Layers \n5:  Edit and Manage Data \n6:  Maps \n7:  Scenes \n8:  Routing and Logistics \n9:  Utility Networks \n10: Search and Query \n11: Visualization")
    print("Enter a number (1-11) to sample category: ")
    sampleCategory = getSampleCategory(scanner.nextLine().trim().toIntOrNull())

    // Handles either if JAR file or source code is executed.
    samplesRepoPath = Paths.get("").toAbsolutePath().toString().replace("/NewModuleScript", "")
    samplesRepoPath = samplesRepoPath.replace("/tools", "")
    println("Using repository... $samplesRepoPath")

    try {
        createFilesAndFolders()
        deleteUnwantedFiles()
        updateSampleContent()
    } catch (e: Exception) {
        exitProgram(e)
    }
    println("Sample Successfully Created! ")
}

private fun getSampleCategory(i: Int?): String {
    if (i == null || i > 11 || i < 1) {
        exitProgram(Exception("Invalid category input"))
    }
    when (i) {
        1 -> return "Analysis"
        2 -> return "Augmented Reality"
        3 -> return "Cloud and Portal"
        4 -> return "Layers"
        5 -> return "Edit and Manage Data"
        6 -> return "Maps"
        7 -> return "Scenes"
        8 -> return "Routing and Logistics"
        9 -> return "Utility Networks"
        10 -> return "Search and Query"
        11 -> return "Visualization"
    }
    return ""
}

/**
 * This function cleans up unwanted files copied
 * when createFilesAndFolders() is called
 */
private fun deleteUnwantedFiles() {
    val buildFolder = File("$samplesRepoPath/samples/$sampleWithHyphen/build")
    val displayComposableMapKotlinFolder = File(
        "$samplesRepoPath/samples/$sampleWithHyphen/src/main/java/com/esri/arcgismaps/sample/displaycomposablemapview"
    )
    FileUtils.deleteDirectory(buildFolder)
    FileUtils.deleteDirectory(displayComposableMapKotlinFolder)
}

/**
 * Creates the files and folders needed for a new sample
 */
private fun createFilesAndFolders() {
    // Create the sample resource folders
    val destinationResDirectory = File("$samplesRepoPath/samples/$sampleWithHyphen")
    destinationResDirectory.mkdirs()
    // Display Map's res directory to copy over to new sample
    val sourceResDirectory = File("$samplesRepoPath/samples/display-composable-mapview/")

    // Screenshot image copied from source
    val image = File("$samplesRepoPath/samples/$sampleWithHyphen/display-composable-mapview.png")

    // Perform copy of the Android res folders from display-composable-mapview sample.
    FileUtils.copyDirectory(sourceResDirectory, destinationResDirectory)

    // Create the sample package directory in the source folder
    val packageDirectory =
        File("$samplesRepoPath/samples/$sampleWithHyphen/src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces")
    if (!packageDirectory.exists()) {
        packageDirectory.mkdirs()
    } else {
        exitProgram(Exception("Sample folder already exists!: ${packageDirectory.path}"))
    }

    // Copy Kotlin template files to new sample
    val mainActivityTemplate = File("$samplesRepoPath/tools/NewModuleScript/MainActivityTemplate.kt")
    val mapViewModelTemplate = File("$samplesRepoPath/tools/NewModuleScript/MapViewModelTemplate.kt")
    val mainScreenTemplate = File("$samplesRepoPath/tools/NewModuleScript/MainScreenTemplate.kt")

    // Perform copy
    FileUtils.copyFileToDirectory(mainActivityTemplate, packageDirectory)
    var source = Paths.get("$packageDirectory/MainActivityTemplate.kt")
    Files.move(source, source.resolveSibling("MainActivity.kt"))

    var componentsDir = File("$packageDirectory/components")
    componentsDir.mkdirs()

    // copy and rename view-model
    FileUtils.copyFileToDirectory(mapViewModelTemplate, componentsDir)
    source = Paths.get("$componentsDir/MapViewModelTemplate.kt")
    Files.move(source, source.resolveSibling("${sampleNameCamelCase}ViewModel.kt"))

    // rename screenshot
    Files.move(image.toPath(), image.toPath().resolveSibling("${sampleWithHyphen}.png"))

    // copy and rename screens
    componentsDir = File("$packageDirectory/screens")
    componentsDir.mkdirs()
    FileUtils.copyFileToDirectory(mainScreenTemplate, componentsDir)
    source = Paths.get("$componentsDir/MainScreenTemplate.kt")
    Files.move(source, source.resolveSibling("${sampleNameCamelCase}Screen.kt"))
}

/**
 * Exits the program with error -1 if it encounters an error
 * @param e Error message to display
 */
private fun exitProgram(e: Exception) {
    println("Error creating the sample: ")
    e.printStackTrace()
    exitProcess(-1)
}

/**
 * Updates the content in the copied files to reflect the name of the sample
 * E.g. README.md, build.gradle.kts, MainActivity.kt, etc.
 */
private fun updateSampleContent() {
    //Update README.md

    var file = File("$samplesRepoPath/samples/$sampleWithHyphen/README.md")
    FileUtils.write(file, "# $sampleName\n", StandardCharsets.UTF_8)

    //Update README.metadata.json
    file = File("$samplesRepoPath/samples/$sampleWithHyphen/README.metadata.json")
    FileUtils.write(
        file,
        """
                {
                    "category": "$sampleCategory",
                    "description": "TODO",
                    "formal_name": "$sampleNameCamelCase",
                    "ignore": false,
                    "images": [
                        "$sampleWithHyphen.png"
                    ],
                    "keywords": [ ],
                    "language": "kotlin",
                    "redirect_from": "",
                    "relevant_apis": [ ],
                    "snippets": [
                        "src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/${sampleNameCamelCase}ViewModel.kt",
                        "src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/${sampleNameCamelCase}Screen.kt",
                        "src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/MainActivity.kt"
                    ],
                    "title": "$sampleName"
                }
                
                """.trimIndent(), StandardCharsets.UTF_8
    )

    //Update build.gradle.kts
    file = File("$samplesRepoPath/samples/$sampleWithHyphen/build.gradle.kts")
    var fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    fileContent = fileContent.replace("sample.displaycomposablemapview", "sample.$sampleWithoutSpaces")
    FileUtils.write(file, fileContent, StandardCharsets.UTF_8)

    //Update strings.xml
    file = File("$samplesRepoPath/samples/$sampleWithHyphen/src/main/res/values/strings.xml")
    fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    fileContent = fileContent.replace(
        "<string name=\"display_composable_map_view_app_name\">Display composable map view</string>",
        "<string name=\"${sampleNameUnderscore}_app_name\">$sampleName</string>"
    )
    FileUtils.write(file, fileContent, StandardCharsets.UTF_8)

    //Update MainActivity.kt
    file =
        File("$samplesRepoPath/samples/$sampleWithHyphen/src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/MainActivity.kt")
    fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance()[Calendar.YEAR])
    fileContent = fileContent.replace("sample.displaycomposablemapview", "sample.$sampleWithoutSpaces")
    fileContent = fileContent.replace("SampleApp()", "${sampleNameCamelCase}App()")
    fileContent = fileContent.replace("MainScreen(", "${sampleNameCamelCase}Screen(")
    fileContent = fileContent.replace("app_name", "${sampleNameUnderscore}_app_name")
    fileContent = fileContent.replace("screens.MainScreen", "screens.${sampleNameCamelCase}Screen")
    FileUtils.write(file, fileContent, StandardCharsets.UTF_8)

    //Update MapViewModel.kt
    file =
        File("$samplesRepoPath/samples/$sampleWithHyphen/src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/components/${sampleNameCamelCase}ViewModel.kt")
    fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance()[Calendar.YEAR])
    fileContent = fileContent.replace("sample.displaycomposablemapview", "sample.$sampleWithoutSpaces")
    fileContent = fileContent.replace("MapViewModel", "${sampleNameCamelCase}ViewModel")
    FileUtils.write(file, fileContent, StandardCharsets.UTF_8)

    //Update MainScreen.kt
    file =
        File("$samplesRepoPath/samples/$sampleWithHyphen/src/main/java/com/esri/arcgismaps/sample/$sampleWithoutSpaces/screens/${sampleNameCamelCase}Screen.kt")
    fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance()[Calendar.YEAR])
    fileContent = fileContent.replace("sample.displaycomposablemapview", "sample.$sampleWithoutSpaces")
    fileContent = fileContent.replace("MapViewModel", "${sampleNameCamelCase}ViewModel")
    fileContent = fileContent.replace("MainScreen(", "${sampleNameCamelCase}Screen(")
    fileContent =
        fileContent.replace("display_composable_map_view_app_name", "${sampleNameUnderscore}_app_name")
    FileUtils.write(file, fileContent, StandardCharsets.UTF_8)
}

/**
 * Needed only for debugging purposes
 */
private fun resetProgram() {
    val toDelete = File("$samplesRepoPath/samples/$sampleWithHyphen")
    try {
        FileUtils.deleteDirectory(toDelete)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun String.toUpperCamelCase(): String {
    return this.split(" ")
        .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
}

