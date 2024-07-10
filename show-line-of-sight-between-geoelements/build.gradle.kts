plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.esri.arcgismaps.sample.showlineofsightbetweengeoelements"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        buildConfigField("String", "API_KEY", project.properties["API_KEY"].toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExt.get()
    }

    namespace = "com.esri.arcgismaps.sample.showlineofsightbetweengeoelements"
}

dependencies {
    // lib dependencies from rootProject build.gradle.kts
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose Bill of Materials
    implementation(platform(libs.androidx.compose.bom))
    // Jetpack Compose dependencies
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(project(":samples-lib"))
    // Toolkit dependencies
    implementation(platform(libs.arcgis.maps.kotlin.toolkit.bom))
    implementation(libs.arcgis.maps.kotlin.toolkit.geoview.compose)
    implementation(libs.firebase.perf.ktx)
}
