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
            kotlinCompilerExtensionVersion = libs.findVersion("kotlinVersion").get().toString()
        }

        dependencies {
            val composeBom = libs.findLibrary("androidx-compose-bom").get()
            implementation(platform(composeBom))
            implementation(libs.findLibrary("androidx-activity-compose").get())
            implementation(libs.findLibrary("androidx-compose-material3").get())
            implementation(libs.findLibrary("androidx-compose-material3-adaptive").get())
            implementation(libs.findLibrary("androidx-compose-material3-adaptiveLayout").get())
            implementation(libs.findLibrary("androidx-compose-material3-adaptiveNavigation").get())
            implementation(libs.findLibrary("androidx-compose-material3-windowSizeClass").get())
            implementation(libs.findLibrary("android-material").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            implementation(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            implementation(libs.findLibrary("androidx-concurrent-futures").get())
            implementation(libs.findLibrary("androidx-concurrent-futures-ktx").get())
            debugImplementation(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}

internal fun Project.configureAndroidComposeTests(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        dependencies {
            val composeBom = libs.findLibrary("androidx-compose-bom").get()
            androidTestImplementation(platform(composeBom))
            androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())
            androidTestImplementation(libs.findLibrary("androidx-test-uiautomator").get())
            androidTestImplementation(libs.findLibrary("androidx-test-runner").get())
            androidTestImplementation(libs.findLibrary("androidx-test-rules").get())
            androidTestImplementation(libs.findLibrary("androidx-test-ext-junit-ktx").get())
            androidTestImplementation(libs.findLibrary("androidx-junit").get())
            androidTestImplementation(libs.findLibrary("androidx-espresso-core").get())
            androidTestImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
            debugImplementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            debugImplementation(libs.findLibrary("junit").get())
        }
    }
}
