plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.esri.arcgismaps.sample.showgrid"
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
        dataBinding = true
        buildConfig = true
    }

    namespace = "com.esri.arcgismaps.sample.showgrid"
}

dependencies {
    // lib dependencies from rootProject build.gradle.kts
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.material)
    implementation(project(":samples-lib"))
}
