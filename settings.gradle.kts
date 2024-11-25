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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://esri.jfrog.io/artifactory/arcgis") }
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "ArcGISMapsKotlinSamples"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":samples-lib")

// dynamically include all sample libraries
File("samples/").listFiles()?.filter { !it.name.contains(".DS_Store") }?.forEach { sampleFolder ->
    // include all of the samples, which have been changed into libs of the sample viewer app
    include(":samples:" + sampleFolder.name)
    project(":samples:" + sampleFolder.name).projectDir = file("samples/" + sampleFolder.name)
}
