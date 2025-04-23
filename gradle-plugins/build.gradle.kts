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
    // `kotlin-dsl` applied here uses embedded Kotlin version 1.9.23 for gradle wrapper 8.9
    // https://docs.gradle.org/current/userguide/compatibility.html#kotlin
    // This is needed as newer versions of kotlinx-serialization has been compiled using Kotlin 2.0+
    // https://github.com/Kotlin/kotlinx.serialization/releases
    // Sample modules do not use kotlin-dsl, instead uses latest kotlin serialization from libs versions.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }
}
