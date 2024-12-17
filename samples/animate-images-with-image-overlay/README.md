# Animate images with image overlay

Animate a series of images with an image overlay.

![Image of animate images with image overlay](animate-images-with-image-overlay.png)

## Use case

An image overlay is useful for displaying fast and dynamic images; for example, rendering real-time sensor data captured from a drone. Each frame from the drone becomes a static image which is updated on the fly as the data is made available.

## How to use the sample

The application loads a map of the Southwestern United States. Tap the "Start" or "Stop" button to toggle the radar animation. Use the drop down menu to select how quickly the animation plays. Move the slider to change the opacity of the image overlay.

## How it works

1. Create an `ImageOverlay` and add it to the `SceneView`.
2. Set up a timer with an initial interval time of 17ms, which will display approximately 60 `ImageFrame`s per second.
3. On every tick of the timer, add the next image frame to the image overlay.

## Relevant API

* ImageFrame
* ImageOverlay
* SceneView

## About the data

These radar images were captured by the US National Weather Service (NWS). They highlight the Pacific Southwest sector which is made up of part the western United States and Mexico. For more information visit the [National Weather Service](https://www.weather.gov/jetstream/gis) website.

## Additional information

The supported image formats are GeoTIFF, TIFF, JPEG, and PNG. `ImageOverlay` does not support the rich processing and rendering capabilities of a `RasterLayer`. Use `Raster` and `RasterLayer` for static image rendering, analysis, and persistence.

This sample uses the GeoViewCompose Toolkit module to implement a Composable MapView.

## Tags

3d, animation, drone, dynamic, image frame, image overlay, real time, rendering
