plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
}

android {
    namespace = "com.esri.arcgismaps.sample.filterbuildingscenelayer"
}

dependencies {
    // Only module specific dependencies needed here
    implementation(libs.arcgis.maps.kotlin.toolkit.popup)
}
