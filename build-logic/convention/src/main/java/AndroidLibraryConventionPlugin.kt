import com.android.build.api.dsl.LibraryExtension
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
                // External libraries
                implementation(libs.findLibrary("androidx-constraintlayout").get())
                implementation(libs.findLibrary("androidx-appcompat").get())
                implementation(libs.findLibrary("android-material").get())
                implementation(libs.findLibrary("androidx-compose-material-icons-extended").get())
            }
        }
    }
}
