package com.esri.arcgismaps.kotlin.build_logic.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Extension to use Android Kotlin configurations and dependencies
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = libs.findVersion("targetSdk").get().toString().toInt()

        defaultConfig.apply {
            minSdk = libs.findVersion("minSdk").get().toString().toInt()
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureKotlin()
}

/**
 * Configure base Kotlin options
 */
private fun Project.configureKotlin() {
    configure<KotlinAndroidProjectExtension> {
        jvmToolchain(17)
        val warningsAsErrors = providers.gradleProperty("warningsAsErrors").orNull
        compilerOptions {
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())
            freeCompilerArgs.set(
                listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    // Enable experimental coroutines APIs, including Flow
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview"
                )
            )
        }
    }
}
