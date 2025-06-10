# Apply mosaic rule to rasters

Apply mosaic rules to a mosaic dataset of rasters using the ArcGIS Maps SDK for Kotlin.

![Image of Apply mosaic rule to rasters sample](apply-mosaic-rule-to-rasters.png)

## Use case

An image service can use a mosaic rule to mosaic multiple rasters on-the-fly. A mosaic rule can specify which rasters are selected, how the selected rasters are z-ordered, and how overlapping pixels from different rasters at the same location are resolved.

For example, using the "By Attribute" mosaic method, the values in an attribute field are used to sort the images. Using the "Center" method, the image closest to the center of the display is positioned as the top image in the mosaic. The mosaic operator allows you to define how to resolve overlapping cells, such as blending or averaging.

Specifying mosaic rules is useful for viewing overlapping rasters. For example, using the "By Attribute" mosaic method to sort the rasters based on their acquisition date allows the newest image to be on top. Using the "mean" mosaic operation makes the overlapping areas contain the mean cell values from all the overlapping rasters.

## How to use the sample

When the rasters are loaded, choose from a list of preset mosaic rules to apply to the rasters using the dropdown menu at the bottom of the screen. The map will update to display the rasters according to the selected rule.

## How it works

1. Create an `ImageServiceRaster` using the service's URL.
2. Create a `MosaicRule` object and set it to the `mosaicRule` property of the image service raster.
3. Create a `RasterLayer` from the image service raster and add it to the map.
4. Set the `mosaicMethod`, `mosaicOperation`, and other properties of the mosaic rule object according to the selected rule.
5. When the user selects a different mosaic rule, update the `mosaicRule` property of the image service raster and reload the raster layer.

## Relevant API

- ImageServiceRaster
- MosaicRule
- MosaicMethod
- MosaicOperation
- RasterLayer

## About the data

This sample uses a [raster image service](https://sampleserver7.arcgisonline.com/server/rest/services/amberg_germany/ImageServer) that shows aerial images of Amberg, Germany.

## Additional information

For more information, see [Understanding the mosaicking rules for a mosaic dataset](https://desktop.arcgis.com/en/arcmap/latest/manage-data/raster-and-images/understanding-the-mosaicking-rules-for-a-mosaic-dataset.htm) from *ArcGIS Desktop* documentation. To learn more about how to define certain mosaic rules, see [Mosaic rule objects](https://developers.arcgis.com/rest/services-reference/enterprise/mosaic-rules/) from the *ArcGIS REST API documentation*.

## Tags

image service, mosaic method, mosaic rule, raster