pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://esri.jfrog.io/artifactory/arcgis") }
        maven { url = uri("https://olympus.esri.com/artifactory/arcgisruntime-repo/") }
    }
}

rootDir.listFiles()?.forEach {
    if (it.isDirectory && File(it, "build.gradle.kts").exists()) {
        include(":${it.name}")
    }
}
