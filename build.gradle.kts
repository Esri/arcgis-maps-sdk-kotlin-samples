// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradle.secrets) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

buildscript {
    // if a "build" property is set from the command line like: "-D build=100.X.X-XXXX"
    if (System.getProperty("build") != null) {
        // override versions in libs.versions.toml file
        rootProject.extra.apply {
            set("arcgisMapsKotlinVersion", System.getProperty("build"))
            set("arcgisToolkitVersion", System.getProperty("build"))
            set("versionName", System.getProperty("build"))
        }
    } else {
        // use versions in libs.versions.toml file
        rootProject.extra.apply {
            set("arcgisMapsKotlinVersion", libs.versions.arcgisMapsKotlinVersion.get())
        }
    }
}