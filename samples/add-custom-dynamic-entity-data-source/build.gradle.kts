plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
}

android {
    namespace = "com.esri.arcgismaps.sample.addcustomdynamicentitydatasource"
}

dependencies {
    // Only module specific dependencies needed here
    implementation(libs.kotlinx.serialization.json)
}
