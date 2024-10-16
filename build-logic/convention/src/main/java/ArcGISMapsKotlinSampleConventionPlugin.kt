import com.esri.arcgismaps.kotlin.build_logic.convention.implementation
import com.esri.arcgismaps.kotlin.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class ArcGISMapsKotlinSampleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                // ArcGIS Maps SDK for Kotlin
                implementation(libs.findLibrary("arcgis-maps-kotlin").get())
                // Get the Toolkit BOM
                implementation(platform(libs.findLibrary("arcgis-maps-kotlin-toolkit-bom").get()))
                // ArcGIS Maps SDK Toolkit GeoView Compose
                implementation(libs.findLibrary("arcgis-maps-kotlin-toolkit-geoview-compose").get())
                // Local project common samples library
                implementation(project(":samples-lib"))
            }
        }
    }
}
