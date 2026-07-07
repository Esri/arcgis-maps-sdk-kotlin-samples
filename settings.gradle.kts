pluginManagement {
    includeBuild("build-logic")
    includeBuild("gradle-plugins")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
val localProperties = java.util.Properties().apply {
    val localPropertiesFile = file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// The Artifactory credentials for the ArcGIS Maps SDK for Kotlin repository.
// First look for the credentials provided via command line (for CI builds), if not found,
// take the one defined in local.properties.
// CI builds pass -PartifactoryUrl=${ARTIFACTORY_URL} -PartifactoryUser=${ARTIFACTORY_USER} -PartifactoryPassword=${ARTIFACTORY_PASSWORD}
val artifactoryUrl: String =
    providers.gradleProperty("artifactoryUrl").orNull
        ?: localProperties.getProperty("artifactoryUrl")
        ?: ""

val artifactoryUsername: String =
    providers.gradleProperty("artifactoryUsername").orNull
        ?: localProperties.getProperty("artifactoryUsername")
        ?: ""

val artifactoryPassword: String =
    providers.gradleProperty("artifactoryPassword").orNull
        ?: localProperties.getProperty("artifactoryPassword")
        ?: ""

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://esri.jfrog.io/artifactory/arcgis") }
        if (!artifactoryUrl.isBlank()) {
            maven {
                url = java.net.URI(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "ArcGISMapsKotlinSamples"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":samples-lib")

// dynamically include all sample libraries
val samplesDir = file("samples")
samplesDir.listFiles()
    ?.asSequence()
    ?.filter { it.isDirectory && it.name != ".DS_Store" }
    ?.filter { sampleFolder ->
        sampleFolder.listFiles()?.any { it.name == "build.gradle.kts" } == true
    }
    ?.forEach { sampleFolder ->
        // The settings file owns project structure for the build.
        include(":samples:${sampleFolder.name}")
        project(":samples:${sampleFolder.name}").projectDir = samplesDir.resolve(sampleFolder.name)
    }
