
## Metadata updater script

This script updates the `README.metadata.json` files for any samples that it is given.

### How to use this script

Navigate to the top-level directory of this repository (`arcgis-maps-sdk-kotlin-samples`).

The script has two types of arguments:
* `-m` or `--multiple` to recreate metadata files for all samples in a given directory.
```
# recreates all metadata files for samples
python3 tools/MetdataUpdater/metadata_updater.py -m ../arcgis-maps-sdk-kotlin-samples/samples
```
* `-s` or `--single` to recreate a metadata file for a single given sample. The argument should provide the language directory name and the sample directory name.
```
# recreates the metadata file for the kotlin sample "Add features feature service"
python3 tools/MetadataUpdater/metadata_updater.py -s ../arcgis-maps-sdk-kotlin-samples/samples/add-features-feature-service
```

**Note:** The script cannot create a metadata file from scratch. You should first create a file in the sample's directory called `README.metadata.json`. The contents of the file can be
```
{
}
```

When recreating single metadata files, if any of the following entries are not present or empty, they will be created and given the value "TODO". This is because they cannot be filled in by the script. Please remove the "TODO" and update with the correct info or remove the entry altogether before merging.
* category
* provision_from
* provision_to
* redirect_from

### How it works

To update all sample metadata files in a directory:

1. Loop through the subfolders of the provided directory
2. A `MetadataUpdater` is created, passing in the subfolder's path, with class fields for each key of the output json.
3. Populate fields from the existing `README.metadata.json`:
  * Check for a `category` key and write it to the updater's `self.category` field.
  * For each of `provision_from`, `provision_to`, and `redirect_from`, check if the key exists, and if it does, write it to the corresponding field of the updater.
4. Populate fields from the sample's `README.md`:
  * Split the readme by two hash symbols `##` to find the headings of each section.
  * Get the title and description by parsing the head (first section) of the readme.
  * Create the `formal_name` property by converting the title to Pascal case.
  * Parse the APIs and tags by cleaning up white space and separators in the readme.
5. Populate fields from the sample's file paths:
  * To get the screenshot, traverse the immediate files inside the sample directory, looking for a file with the `.png` extension.
  * To get the language and snippets, search recursively through the directory for files with the extension `.java` or `.kt`, ignoring the `/build/` directory.
6. Create a dictionary. For each of the required metadata keys, create a key with a string title and a corresponding class field as its value.
  * For the `category`, `provision_from`, `provision_to`, and `redirect_from` keys, check if that they are not empty in the updater's fields before adding them to the dictionary. If they are empty, set them to "TODO"
7. Dump the dictionary to a json file.
