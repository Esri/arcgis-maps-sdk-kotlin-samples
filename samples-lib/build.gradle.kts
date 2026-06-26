import com.esri.arcgismaps.kotlin.build_logic.convention.implementation

plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
}

android {
    namespace = "com.esri.arcgismaps.sample.sampleslib"
    buildFeatures {
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
    val buildVersion = System.getProperty("build")
    // Override version in libs.versions.toml file
    if (buildVersion != null) {
        implementation("com.esri:arcgis-maps-kotlin:${buildVersion}")
        implementation(platform("com.esri:arcgis-maps-kotlin-toolkit-bom:$buildVersion"))
    } else {
        implementation(libs.arcgis.maps.kotlin)
        implementation(platform(libs.arcgis.maps.kotlin.toolkit.bom))
    }
}


