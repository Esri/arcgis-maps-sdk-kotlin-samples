package com.esri.arcgismaps.kotlin.build_logic.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Extension to use compose configurations and dependencies
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = libs.findVersion("kotlinCompilerExt").get().toString()
        }

        dependencies {
            val composeBom = libs.findLibrary("androidx-compose-bom").get()
            implementation(platform(composeBom))
            androidTestImplementation(platform(composeBom))
            implementation(libs.findLibrary("androidx-activity-compose").get())
            implementation(libs.findLibrary("androidx-compose-material3").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            implementation(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            debugImplementation(libs.findLibrary("androidx-compose-ui-tooling").get())
            debugImplementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            androidTestImplementation(libs.findLibrary("androidx-compose-ui-test").get())
            androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())

        }
    }
}
