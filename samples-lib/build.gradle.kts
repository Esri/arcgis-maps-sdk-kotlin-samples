plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
    alias(libs.plugins.gradle.secrets)
}

secrets {
    // this file doesn't contain secrets, it just provides defaults which can be committed into git.
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.esri.arcgismaps.sample.sampleslib"
    buildFeatures {
        buildConfig = true
        dataBinding = true
    }
}

dependencies {
    // Only module specific dependencies needed here

    // lib dependencies from rootProject build.gradle
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.material)
    implementation(libs.commons.io)
    // lib dependencies for samples using Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose Bill of Materials
    implementation(platform(libs.androidx.compose.bom))
    // Jetpack Compose dependencies
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.arcgis.maps.kotlin)
}


