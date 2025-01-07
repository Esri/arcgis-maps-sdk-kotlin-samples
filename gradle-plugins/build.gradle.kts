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
