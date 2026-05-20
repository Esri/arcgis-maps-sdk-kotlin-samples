repositories {
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        create("copyCodeFiles") {
            id = "com.arcgismaps.sampleFiles.copy"
            version = "1.0"
            implementationClass = "com.arcgismaps.CopySampleFilesTask"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
