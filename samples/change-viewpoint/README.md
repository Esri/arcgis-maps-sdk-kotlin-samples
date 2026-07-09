# Change viewpoint

Set the map view to a new viewpoint.

![Change viewpoint sample](change-viewpoint.png)

## Use case

Navigate programmatically to a specific location on the map, allowing you to zoom in on a particular point.

## How to use the sample

The map view has several methods for setting its current viewpoint. Select a viewpoint from the UI to see the viewpoint changed using that method.

## How it works

1. Create a new `ArcGISMap` object and pass it to the `MapView` composable's `arcGISMap` parameter.
2. Change the map's `Viewpoint` by calling one of the available methods via `MapViewProxy`:
    * Use `MapViewProxy.setViewpointAnimted()` to pan to a viewpoint over a specified `Duration`.
    * Use `MapViewProxy.setViewpointCenter()` to center the viewpoint on a `Point`.
    * Use `MapViewProxy.setViewpointGeometry()` to set a viewpoint on a given `Geometry`

## Relevant API

* ArcGISMap
* Geometry
* MapViewProxy
* Point
* Viewpoint

## Additional information

See the various "setViewpoint" methods on `MapViewProxy` and `SceneViewProxy` [here](https://developers.arcgis.com/kotlin/toolkit-api-reference/arcgis-maps-kotlin-toolkit/com.arcgismaps.toolkit.geoviewcompose/-map-view-proxy/index.html).

## Tags

animate, center, extent, pan, rotate, scale, view, zoom
