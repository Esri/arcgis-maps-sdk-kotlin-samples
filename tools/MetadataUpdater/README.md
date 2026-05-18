
## Metadata updater script

This script updates the `README.metadata.json` files for any samples that it is given.

### How to use this script

Navigate to the top-level directory of this repository (`arcgis-maps-sdk-kotlin-samples`).

The script has two types of arguments:
* `-m` or `--multiple` to recreate metadata files for all samples in a given directory.
```
# recreates all metadata files for samples
python3 tools/MetadataUpdater/metadata_updater.py -m ../arcgis-maps-sdk-kotlin-samples/samples
```
* `-s` or `--single` to recreate a metadata file for a single given sample. The argument should provide the path to that sample directory.
```
# recreates the metadata file for the kotlin sample "Add features feature service"
python3 tools/MetadataUpdater/metadata_updater.py -s ../arcgis-maps-sdk-kotlin-samples/samples/add-features-feature-service
```

**Note:** The script cannot create a metadata file from scratch. You should first create a file in the sample's directory called `README.metadata.json`. The contents of the file can be
```
{
}
```

When recreating single metadata files, if `category` is not present or empty, it will be created and given the value `"TODO"`. Please replace `"TODO"` with the correct category before merging. If `redirect_from` is missing, the script writes an empty string.

### How it works

To update all sample metadata files in a directory:

1. Loop through the subfolders of the provided directory
2. A `MetadataUpdater` is created, passing in the subfolder's path, with class fields for each key of the output json.
3. Populate fields from the existing `README.metadata.json`:
  * Check for a `category` key and write it to the updater's `self.category` field.
4. Populate fields from the sample's `README.md`:
  * Split the readme by two hash symbols `##` to find the headings of each section.
  * Get the title and description by parsing the head (first section) of the readme.
  * Create the `formal_name` property by converting the title to Pascal case.
  * Parse the APIs and tags by cleaning up white space and separators in the readme.
5. Populate fields from the sample's file paths:
  * To get the screenshot, traverse the immediate files inside the sample directory, looking for a file with the `.png` extension.
  * To get the language and snippets, search recursively through the directory for files with the extension `.java` or `.kt`, ignoring the `/build/` directory.
6. Create a dictionary. For each of the required metadata keys, create a key with a string title and a corresponding class field as its value.
  * If `category` tag is missing/empty for a sample, the script shall set them to `"TODO"`.
  * If `redirect_from` tag is missing sample for a sample, the script shall set them to use an empty string.
7. Dump the dictionary to a json file.
