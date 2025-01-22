# Get elevation at point on surface

Get the elevation for a given point on a surface in a scene.

![Image of get elevation at point](get-elevation-at-point-on-surface.png)

## Use case

Knowing the elevation at a given point in a landscape can aid in navigation, planning and survey in the field.

## How to use the sample

Tap anywhere on the surface to get the elevation at that point. Elevation is reported in meters since the scene view is in WGS84.

## How it works

1. Create a composable `SceneView` and `ArcGISScene` with an imagery base map.
2. Set an `ArcGISTiledElevationSource` as the elevation source of the scene's `baseSurface`.
3. Use the `screenToLocation(screenCoordinate)` function on the scene view to convert the tapped screen coordinate into a point on surface.
4. Use the `getElevation(scenePoint)` method on the base surface to asynchronously get the elevation.

## Relevant API

* ArcGISTiledElevationSource
* BaseSurface
* ElevationSource
* SceneView

## Additional information

`getElevation(scenePoint)` retrieves the most accurate available elevation value at a given point. To do this, the method must go to the server or local raster file and load the highest level of detail of data for the target location and return the elevation value.

If multiple elevation sources are present in the surface, the top most visible elevation source with a valid elevation in the given location is used to determine the result.

## Tags

elevation, surface
