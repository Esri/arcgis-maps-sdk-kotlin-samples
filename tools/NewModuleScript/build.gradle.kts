import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.gradleup.shadow") version "8.3.6"
    kotlin("jvm") version "2.0.0"
}

group = "com.esri.arcgismaps.newmodule"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.6")
}

application {
    mainClass = "com.esri.arcgismaps.newmodule.NewModuleScriptKt"
}

kotlin {
    jvmToolchain(17)
}

tasks.register<ShadowJar>("generateNewModuleScriptArtifact") {
    dependsOn("classes")
    from(sourceSets.main.get().output)
    archiveClassifier = ""
    archiveVersion = ""
    // Place artifact in tools/NewModuleScript/build/dist/NewModuleScript.jar
    destinationDirectory = layout.buildDirectory.dir("dist")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest.attributes["Main-Class"] = "com.esri.arcgismaps.newmodule.NewModuleScriptKt"
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.named("generateNewModuleScriptArtifact"))
}
