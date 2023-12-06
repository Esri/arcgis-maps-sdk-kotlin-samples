import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Scanner;

/**
 * This java file creates a new sample and configures it as a new module in Android Studio.
 * The IntelliJ project creates an .jar artifact which is used to create new samples.
 */
public class ScriptMain {

    private String sampleName;
    private String sampleWithHyphen;
    private String sampleWithoutSpaces;
    private String samplesRepoPath;

    public static void main(String[] args) {
        ScriptMain mainClassObj = new ScriptMain();
        mainClassObj.run();
    }

    private void run(){

        Scanner scanner = new Scanner(System.in);

        // Get the name of the sample
        System.out.println("Enter Name of the sample with spaces (Eg. \"Display New Map\"): ");
        sampleName = scanner.nextLine();

        sampleWithHyphen = sampleName.replace(" ", "-").toLowerCase();
        sampleWithoutSpaces = sampleName.replace(" ", "").toLowerCase();

        // Handles either if JAR file or source code is executed.
        samplesRepoPath = Paths.get("").toAbsolutePath().toString().replace("/NewModuleScript","");
        samplesRepoPath = samplesRepoPath.replace("/tools","");
        System.out.println("Using repository... "+  samplesRepoPath);

        try{
            createFilesAndFolders();
            deleteUnwantedFiles();
            updateSampleContent();
        }catch (Exception e){
            exitProgram(e);
        }
        System.out.println("Sample Successfully Created! ");
    }


    /**
     * This function cleans up unwanted files copied
     * when createFilesAndFolders() is called
     */
    private void deleteUnwantedFiles() {
        File buildFolder = new File(samplesRepoPath + "/" + sampleWithHyphen + "/build");
        File displayComposableMapKotlinFolder = new File(
                samplesRepoPath + "/" + sampleWithHyphen + "/src/main/java/com/esri/arcgismaps/sample/displaycomposablemapview");
        File image = new File(samplesRepoPath + "/" + sampleWithHyphen + "/display-composable-mapview.png");
        try {
            FileUtils.deleteDirectory(buildFolder);
            FileUtils.deleteDirectory(displayComposableMapKotlinFolder);
            image.delete();
        } catch (IOException e) {
            exitProgram(e);
            e.printStackTrace();
        }
    }

    /**
     * Creates the files and folders needed for a new sample
     */
    private void createFilesAndFolders() {
        // Create the sample resource folders
        File destinationResDirectory = new File(samplesRepoPath + "/" + sampleWithHyphen);
        destinationResDirectory.mkdirs();
        // Display Map's res directory to copy over to new sample
        File sourceResDirectory = new File(samplesRepoPath + "/display-composable-mapview/");

        // Perform copy of the Android res folders from display-composable-mapview sample.
        try {
            FileUtils.copyDirectory(sourceResDirectory, destinationResDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        // Create the sample package directory in the source folder
        File packageDirectory = new File(samplesRepoPath + "/" + sampleWithHyphen + "/src/main/java/com/esri/arcgismaps/sample/" + sampleWithoutSpaces);
        if(!packageDirectory.exists()){
            packageDirectory.mkdirs();
        }else{
            exitProgram(new Exception("\"Sample already exists!\""));
        }

        // Copy Kotlin template files to new sample
        File mainActivityTemplate = new File(samplesRepoPath + "/tools/NewModuleScript/MainActivityTemplate.kt");
        File mapViewModelTemplate = new File(samplesRepoPath + "/tools/NewModuleScript/MapViewModelTemplate.kt");
        File mainScreenTemplate = new File(samplesRepoPath + "/tools/NewModuleScript/MainScreenTemplate.kt");

        // Perform copy
        try {
            FileUtils.copyFileToDirectory(mainActivityTemplate, packageDirectory);
            Path source = Paths.get(packageDirectory+"/MainActivityTemplate.kt");
            Files.move(source, source.resolveSibling("MainActivity.kt"));

            File composeComponentsDir = new File(packageDirectory + "/components");
            composeComponentsDir.mkdirs();

            FileUtils.copyFileToDirectory(mapViewModelTemplate, composeComponentsDir);
            source = Paths.get(composeComponentsDir+"/MapViewModelTemplate.kt");
            Files.move(source, source.resolveSibling("MapViewModel.kt"));

            composeComponentsDir = new File(packageDirectory + "/screens");
            composeComponentsDir.mkdirs();
            FileUtils.copyFileToDirectory(mainScreenTemplate, composeComponentsDir);
            source = Paths.get(composeComponentsDir+"/MainScreenTemplate.kt");
            Files.move(source, source.resolveSibling("MainScreen.kt"));
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }
    }

    /**
     * Exits the program with error -1 if it encounters an error
     * @param e Error message to display
     */
    private void exitProgram(Exception e){
        System.out.println("Error creating the sample: " + e.getMessage());
        System.out.println("StackTrace:");
        e.printStackTrace();
        System.exit(-1);
    }

    /**
     * Updates the content in the copied files to reflect the name of the sample
     * Eg. README.md, build.gradle, MainActivity.kt, etc.
     */
    private void updateSampleContent() {

        //Update README.md
        File file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/README.md");
        try {
            FileUtils.write(file,"# " + sampleName, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update README.metadata.json
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/README.metadata.json");
        try {
            FileUtils.write(file,"{\n}", StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update build.gradle
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/build.gradle");
        try {
            String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            fileContent = fileContent.replace("sample.displaycomposablemapview", "sample." + sampleWithoutSpaces);
            FileUtils.write(file,fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update strings.xml
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/src/main/res/values/strings.xml");
        try {
            String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            fileContent = fileContent.replace(
                    "<string name=\"app_name\">Display composable mapView</string>",
                    "<string name=\"app_name\">" + sampleName +"</string>");
            FileUtils.write(file,fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update MainActivity.kt
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/src/main/java/com/esri/arcgismaps/sample/"+sampleWithoutSpaces+"/MainActivity.kt");
        try {
            String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance().get(Calendar.YEAR));
            fileContent = fileContent.replace("sample.displaycomposablemapview", "sample." + sampleWithoutSpaces);
            FileUtils.write(file,fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update MapViewModel.kt
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/src/main/java/com/esri/arcgismaps/sample/"+sampleWithoutSpaces+"/components/MapViewModel.kt");
        try {
            String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance().get(Calendar.YEAR));
            fileContent = fileContent.replace("sample.displaycomposablemapview", "sample." + sampleWithoutSpaces);
            FileUtils.write(file,fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }

        //Update MainScreen.kt
        file = new File(samplesRepoPath + "/" + sampleWithHyphen + "/src/main/java/com/esri/arcgismaps/sample/"+sampleWithoutSpaces+"/screens/MainScreen.kt");
        try {
            String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            fileContent = fileContent.replace("Copyright 2023", "Copyright " + Calendar.getInstance().get(Calendar.YEAR));
            fileContent = fileContent.replace("sample.displaycomposablemapview", "sample." + sampleWithoutSpaces);
            FileUtils.write(file,fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            exitProgram(e);
        }
    }

    /**
     * Needed only for debugging purposes
     */
    private void resetProgram() {
        File toDelete = new File(samplesRepoPath + "/" + sampleWithHyphen);
        try {
            FileUtils.deleteDirectory(toDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
