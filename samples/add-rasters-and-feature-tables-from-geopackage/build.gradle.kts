plugins {
    alias(libs.plugins.arcgismaps.android.library)
    alias(libs.plugins.arcgismaps.android.library.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
}

android {
    namespace = "com.esri.arcgismaps.sample.addrastersandfeaturetablesfromgeopackage"
}

dependencies {
    // Only module specific dependencies needed here
}
