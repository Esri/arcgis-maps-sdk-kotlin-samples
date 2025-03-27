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
// CI builds pass -PartifactoryURL=${ARTIFACTORY_URL} -PartifactoryUser=${ARTIFACTORY_USER} -PartifactoryPassword=${ARTIFACTORY_PASSWORD}
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
        if (artifactoryUrl != "") {
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
File("samples/").listFiles()?.filter { !it.name.contains(".DS_Store") }?.forEach { sampleFolder ->
    if (sampleFolder.listFiles()?.find { it.name.equals("build.gradle.kts") } != null){
        // include all of the samples, which have been changed into libs of the sample viewer app
        include(":samples:" + sampleFolder.name)
        project(":samples:" + sampleFolder.name).projectDir = file("samples/" + sampleFolder.name)
    }
}
