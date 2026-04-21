plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
    alias(libs.plugins.gradle.secrets)
}

secrets {
    // this file doesn't contain secrets, it just provides defaults which can be committed into git.
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.esri.arcgismaps.sample.displayadaptivescene"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Adaptive pane scaffolds for responsive three-pane layouts.
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptiveLayout)
    implementation(libs.androidx.compose.material3.adaptiveNavigation)
}
