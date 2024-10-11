import com.android.build.gradle.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.configureKotlinAndroid
import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import com.esri.arcgismaps.kotlin.build_logic.convention.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                compileSdk = 35
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    lint.targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }

            dependencies {
                testImplementation(kotlin("test"))
                // External libraries
                implementation(libs.findLibrary("androidx-constraintlayout").get())
                implementation(libs.findLibrary("androidx-appcompat").get())
                implementation(libs.findLibrary("android-material").get())
            }
        }
    }
}
