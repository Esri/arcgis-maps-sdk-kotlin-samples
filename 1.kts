apply(from = "version.gradle")

buildscript {
    apply(from = "version.gradle")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$gradleVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

subprojects {
    afterEvaluate { project ->
        if (project.hasProperty("dependencies")) {
            dependencies {
                implementation("androidx.appcompat:appcompat:$appcompatVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
                implementation("com.esri:arcgis-maps-kotlin:$arcgisVersion")
                implementation("androidx.multidex:multidex:$multidexVersion")
            }
        }
        project.android {
            compileOptions {
                sourceCompatibility = rootProject.ext.javaVersion
                targetCompatibility = rootProject.ext.javaVersion
            }
            defaultConfig {
                multiDexEnabled = true
            }
            packagingOptions {
                exclude("META-INF/DEPENDENCIES")
            }
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}

task("apiKey") {
    val apiKeyFile = new File("${System.properties.getProperty("user.home")}/.gradle/gradle.properties")
    if (!apiKeyFile.exists()) {
        print("Go to " + new URL("https://developers.arcgis.com/dashboard/") + " to get an API key.")
        print("Add your API Key to ${System.properties.getProperty("user.home")}\\.gradle\\gradle.properties.")
        String apiKeyFileContents = "API_KEY = "
        apiKeyFile.write(apiKeyFileContents)
    }
}
