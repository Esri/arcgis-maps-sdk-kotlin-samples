import com.android.build.gradle.LibraryExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.configureKotlinAndroid
import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                compileSdk = libs.findVersion("targetSdk").get().toString().toInt()
                defaultConfig {
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    lint.targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                }

                buildTypes {
                    release {
                        // Keep libraries, app module performs the final R8 shrink.
                        isMinifyEnabled = false
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                        ndk {
                            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
                        }
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }

            dependencies {
                // External libraries
                implementation(libs.findLibrary("androidx-constraintlayout").get())
                implementation(libs.findLibrary("androidx-appcompat").get())
                implementation(libs.findLibrary("android-material").get())
                implementation(libs.findLibrary("androidx-compose-material-icons-extended").get())
            }
        }
    }
}
