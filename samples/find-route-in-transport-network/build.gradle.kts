plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.kotlin.sample)
}

android {
    namespace = "com.esri.arcgismaps.sample.findrouteintransportnetwork"
    // For view based samples
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    // Only module specific dependencies needed here
}
