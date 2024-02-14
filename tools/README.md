
# Android Samples Scripts

This directory consists of a script useful for creating new samples on Android.

## New Module Script

This script creates a new sample and configures it as a new module in Android Studio.

### How to use this script

Navigate to the top-level directory of this repository (`arcgis-maps-sdk-kotlin-samples/`).

**Note:** This `.jar` file requires Java 11 installed.

To run the script and create a new sample:

 - `java -jar tools/NewModuleScript.jar`
 - The `.jar` file will prompt you to type in the name of the new sample.

Example:

    $ java -jar tools/NewModuleScript.jar
    Enter Name of the sample with spaces (Eg. "Display New Map"):    
    Display New Map
    Using repository $USER/../arcgis-maps-sdk-kotlin-samples
    Sample Successfully Created!

**Note:** The script will generate all the `gradle`, `src` and `res` files needed for  a new sample. You will have to reload `gradle` manually for Android Studio to implement the changes. To do this run:

 - File -> Sync Project with Gradle Files

### How to build a new script

To build a new `NewModuleSript.jar` using IntelliJ IDEA:

- Open `NewModuleScript` as the project in the IDE.
- In IDE, go to `File` -> `Project Structure` -> `Artifacts`.
- Add a new artifact of type `JAR` from module with dependencies.
  - Select `NewModuleScript.main` as `Module`.
  - Select `ScriptMain` as `Main Class`.
  - Click `OK`.
  - Click `Apply` and `OK`.
- In IDE, go to `Build` -> `Build Artifacts`, select `NewModuleScript.main:jar` and click `Rebuild`.
- The `.jar` file will be generated in the `out` directory of the project.
- Move the `.jar` file to the `tools` directory of the repository. Rename it to `NewModuleScript.jar`.