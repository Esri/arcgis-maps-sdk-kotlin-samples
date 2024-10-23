package com.arcgismaps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


/**
 * Copy screenshot files to a directory (with the name of the associated sample)
 * into the app's build assets directory.
 */
class CopyScreenshotsTask : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<Copy>("copyScreenshots") {
            description = """
                        Copies screenshot screenshots from all the given sample directories. Note:
                        Screenshots must be in .png format.
                        """.trimIndent()

            // Define input and output directories
            val inputDir = project.file("${project.rootDir.path}/samples/")
            val outputDir = project.file("${project.rootDir.path}/app/build/sampleAssets/samples/")

            from(inputDir) {
                // Include all screenshots.
                exclude("**/build/")
                exclude("**/mipmap*/**")
                exclude("**/res/")
                include("**/*.png")
                eachFile {
                    // Prepend the kebab style sample directory to each file path.
                    path = path.substringBefore("/") + "/" + name.replace("-", "_")
                    includeEmptyDirs = false
                    // Don't overwrite existing files with the same name.
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }

            into(outputDir)

            doLast {
                outputDir.listFiles()?.forEach { sampleFolder ->
                    sampleFolder.listFiles()?.filter { it.name.endsWith(".png") }?.forEach {
                        val inputImage = ImageIO.read(it)
                        val outputImage = BufferedImage(350, 200, inputImage.type)
                        val g2d = outputImage.createGraphics()
                        g2d.drawImage(inputImage, 0, 0, 350, 200, null)
                        g2d.dispose()
                        ImageIO.write(outputImage, "png", it)
                    }
                }
            }
        }
    }
}