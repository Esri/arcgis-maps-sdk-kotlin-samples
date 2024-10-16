import com.android.tools.build.jetifier.core.utils.Log
import java.util.Properties

plugins {
    alias(libs.plugins.arcgismaps.android.application)
    alias(libs.plugins.arcgismaps.android.application.compose)
    alias(libs.plugins.arcgismaps.kotlin.sample)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sample.files.copy)
    alias(libs.plugins.screenshots.copy)
    alias(libs.plugins.ksp)
}

tasks.named("preBuild").configure { dependsOn("copyCodeFiles") }
tasks.named("preBuild").configure { dependsOn("copyScreenshots") }

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
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            if (signingPropsFile.exists())
                signingConfig = signingConfigs.getByName("esriSignature")
            else {
                Log.e(
                    tag = "GradleSigningException",
                    message = "signing.properties file not found: ${signingPropsFile.absolutePath}, this will build an unsigned APK.\n" +
                            "Please provide the required \"signingPropsFilePath\" for a signed APK, using:\n" +
                            "./gradlew assembleRelease -PsigningPropsFilePath=absolute-file-path/signing.properties"
                )
            }

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
        if (sampleFile.isDirectory) {
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
