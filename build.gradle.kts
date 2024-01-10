import com.android.build.gradle.BaseExtension
import java.net.URL

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${libs.versions.gradleVersion.get()}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlinVersion.get()}")
    }
}

subprojects {
    afterEvaluate {
        if (hasProperty("dependencies")) {
            dependencies {
                val implementation by configurations
                implementation(libs.androidx.appcompat)
                implementation(libs.stdlib.jdk8)
                implementation(libs.arcgis.maps.kotlin)
                implementation(libs.androidx.multidex)
            }
        }

        extensions.findByType(BaseExtension::class)?.let { android ->
            android.compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            android.defaultConfig {
                multiDexEnabled = true
            }
            android.packagingOptions {
                resources {
                    excludes.add("META-INF/DEPENDENCIES")
                }
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }

    }
}

tasks.register("apiKey") {
    doLast {
        val apiKeyFile = File("${System.getProperty("user.home")}/.gradle/gradle.properties")
        if (!apiKeyFile.exists()) {
            print("Go to ${URL("https://developers.arcgis.com/dashboard/")} to get an API key.")
            print("Add your API Key to ${System.getProperty("user.home")}\\.gradle\\gradle.properties.")
            val apiKeyFileContents = "API_KEY = "
            apiKeyFile.writeText(apiKeyFileContents)
        }
    }
}
