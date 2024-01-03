//apply(plugin = "com.android.application")
//apply(plugin = "org.jetbrains.kotlin.android")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdkVersion(libs.versions.compileSdkVersion.get().toInt())
    defaultConfig {
        applicationId = "com.esri.arcgismaps.sample.adddynamicentitylayer"
        minSdkVersion(libs.versions.minSdkVersion.get())
        targetSdkVersion(libs.versions.targetSdkVersion.get())
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        buildConfigField("String", "API_KEY", project.properties["API_KEY"].toString())
    }

    buildTypes {
        getByName("release") {
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

    namespace = "com.esri.arcgismaps.sample.adddynamicentitylayer"
}

dependencies {
    // lib dependencies from rootProject build.gradle.kts
    implementation("androidx.core:core-ktx:${libs.versions.ktxAndroidCore.get()}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${libs.versions.ktxLifecycle.get()}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.ktxLifecycle.get()}")
    implementation("androidx.activity:activity-compose:${libs.versions.composeActivityVersion.get()}")
    // Jetpack Compose Bill of Materials
    implementation(platform("androidx.compose:compose-bom:${libs.versions.composeBOM.get()}"))
    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(project(":samples-lib"))
}
