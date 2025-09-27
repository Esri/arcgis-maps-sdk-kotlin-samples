# Apply RGB renderer

This sample demonstrates how to apply an RGB renderer to a multispectral raster to adjust how the raster's bands are mapped to red, green and blue and how stretch parameters affect the display.

What this sample does

- Attempts to load a local multispectral raster named `Shasta.tif` from the app's external files directory under the folder `ApplyRgbRenderer`.
- If the raster is found it is added as the basemap using a RasterLayer.
- The sample provides controls to choose a stretch parameter type and configure its parameters (Min-Max, Percent Clip, Histogram Equalization, Standard Deviation) and then applies an RGB renderer to the raster layer.

How to use

1. (Optional) For the full sample experience place a multispectral raster named `Shasta.tif` into the folder:

   <external-files>/ApplyRgbRenderer/Shasta.tif

   The external files folder path varies by device and can be found programmatically by the app when running. If the raster is not present the sample will still run with a default basemap but you will not be able to apply an RGB renderer.

2. Launch the sample.
3. Tap the settings FAB to open the bottom sheet.
4. Choose a stretch type. If the selected stretch type requires additional parameters configure them.
5. Press "Update Renderer" to apply the RGB renderer to the raster layer (if available).

Notes

- The sample is written with Jetpack Compose and follows the pattern of exposing ArcGIS SDK types from a ViewModel and consuming them from a Compose UI.
- The sample performs safety checks: if the raster is not found a helpful message will be shown. This allows the sample to run even when the local resource is not present.
- The sample uses simple preset color choices for the Min-Max stretch. In a production app you could provide a color picker or retrieve raster statistics for better endpoints.

Relevant API

- Raster, RasterLayer
- RGBRenderer and StretchParameters (MinMaxStretchParameters, PercentClipStretchParameters, StandardDeviationStretchParameters, HistogramEqualizationStretchParameters)
- MapView (Toolkit Compose MapView)

Troubleshooting

- If you place a raster file into the external files folder and the sample still cannot find it, verify the file name and that the app has access to external storage. On recent Android versions the app-specific external files directory does not require special permissions.

License

This sample uses the ArcGIS Maps SDK for Kotlin. Follow the SDK licensing and your organization's policy for distributing imagery files.