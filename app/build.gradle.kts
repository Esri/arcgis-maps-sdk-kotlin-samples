import java.util.Properties

plugins {
    alias(libs.plugins.arcgismaps.android.application)
    alias(libs.plugins.arcgismaps.android.application.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.secrets)
    alias(libs.plugins.sample.files.copy)
    alias(libs.plugins.screenshots.copy)
    alias(libs.plugins.ksp)
}

tasks.named("preBuild").configure { dependsOn("copyCodeFiles") }
tasks.named("preBuild").configure { dependsOn("copyScreenshots") }

secrets {
    // this file doesn't contain secrets, it just provides defaults which can be committed into git.
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "com.esri.arcgismaps.kotlin.sampleviewer"

    defaultConfig {
        applicationId = "com.esri.arcgismaps.kotlin.sampleviewer"
        buildConfigField("String", "ARCGIS_VERSION", "\"${rootProject.extra.get("arcgisMapsKotlinVersion")}\"")
    }

    // Optional input to apply the external signing configuration for the sample viewer
    // Example: ./gradlew assembleRelease -PsigningPropsFilePath=absolute-file-path/signing.properties -D build=200.6.0-4385
    val signingPropsFilePath = project.findProperty("signingPropsFilePath").toString()
    val signingPropsFile = rootProject.file(signingPropsFilePath)

    signingConfigs {
        create("esriSignature") {
            // Check for signing.properties from the external file and apply the properties if present
            if (signingPropsFile.exists()) {
                val signingProps = Properties().apply {
                    load(signingPropsFile.inputStream())
                }
                keyAlias = signingProps["keyAlias"] as String
                keyPassword = signingProps["keyPassword"] as String
                storeFile = file(signingProps["storeFilePath"] as String)
                storePassword = signingProps["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            // If signing.properties file not found, gradle will build an unsigned APK.
            // For release builds, provide the required "signingPropsFilePath" for a signed APK, using:
            // ./gradlew assembleRelease -PsigningPropsFilePath=absolute-file-path/signing.properties
            if (signingPropsFile.exists())
                signingConfig = signingConfigs.getByName("esriSignature")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        dataBinding = true
    }
}

dependencies {
    for (sampleFile in file("../samples").listFiles()!!) {
        if (sampleFile.isDirectory &&
            sampleFile.listFiles()?.find { it.name.equals("build.gradle.kts") } != null
        ) {
            implementation(project(":samples:" + sampleFile.name))
        }
    }
    implementation(project(":samples-lib"))
    implementation(libs.arcgis.maps.kotlin.toolkit.authentication)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.appcompat)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.http)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
}
