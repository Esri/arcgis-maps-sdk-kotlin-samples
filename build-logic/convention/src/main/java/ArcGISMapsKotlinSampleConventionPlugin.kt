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
                dependencies {
                    // if a "build" property is set from the command line like: "-D build=300.X.X-XXXX"
                    val buildVersion = System.getProperty("build")
                    // Override version in libs.versions.toml file
                    if (buildVersion != null) {
                        implementation("com.esri:arcgis-maps-kotlin:$buildVersion")
                        implementation(platform("com.esri:arcgis-maps-kotlin-toolkit-bom:$buildVersion"))
                        implementation("com.esri:arcgis-maps-kotlin-toolkit-geoview-compose")
                    } else {
                        // Use version catalog when no build flag is provided
                        implementation(libs.findLibrary("arcgis-maps-kotlin").get())
                        implementation(platform(libs.findLibrary("arcgis-maps-kotlin-toolkit-bom").get()))
                        implementation(libs.findLibrary("arcgis-maps-kotlin-toolkit-geoview-compose").get())
                    }
                    // Local project common samples library
                    implementation(project(":samples-lib"))
                }
            }
        }
    }
}
