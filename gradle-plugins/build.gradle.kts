repositories {
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
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
    // implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }
}
