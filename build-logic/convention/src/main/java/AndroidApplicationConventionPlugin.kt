import com.android.build.api.dsl.ApplicationExtension
import com.esri.arcgismaps.kotlin.build_logic.convention.configureKotlinAndroid
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get

class AndroidApplicationConventionPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                compileSdk = libs.findVersion("targetSdk").get().toString().toInt()
                defaultConfig {
                    buildConfigField(
                        type = "String",
                        name = "ARCGIS_VERSION",
                        value = "\"${System.getProperty("build") ?: libs.findVersion("arcgisMapsKotlinVersion").get()}\""
                    )
                    buildConfigField(
                        type = "String",
                        name = "ARCGIS_STANDARD_LICENSE_KEY",
                        value = project.properties["ARCGIS_STANDARD_LICENSE_KEY"].toString()
                    )
                    buildConfigField(
                        type = "String",
                        name = "ARCGIS_SPATIAL_ANALYSIS_EXTENSION_KEY",
                        value = project.properties["ARCGIS_SPATIAL_ANALYSIS_EXTENSION_KEY"].toString()
                    )
                    buildConfigField(
                        type = "String",
                        name = "ARCGIS_ADVANCED_EDITING_EXTENSION_KEY",
                        value = project.properties["ARCGIS_ADVANCED_EDITING_EXTENSION_KEY"].toString()
                    )

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    versionCode = libs.findVersion("versionCode").get().toString().toInt()
                    versionName = libs.findVersion("versionName").get().toString()
                }

                buildTypes {
                    release {
                        // Enable R8 for release builds.
                        isMinifyEnabled = true
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

                // Add the custom assets directory to the app module's assets build.
                sourceSets["main"].assets.directories.add(
                    layout.buildDirectory.dir("sampleAssets").get().asFile.path
                )
            }
        }
    }
}
