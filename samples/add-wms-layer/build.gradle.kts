plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.kotlin.sample)
    alias(libs.plugins.gradle.secrets)
}

secrets {
    // this file doesn't contain secrets, it just provides defaults which can be committed into git.
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.esri.arcgismaps.sample.addwmslayer"
    // For view based samples
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    // Only module specific dependencies needed here
}
