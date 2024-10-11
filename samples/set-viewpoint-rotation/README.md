# Set viewpoint rotation

Rotate a map.

![Image of set viewpoint rotation](set-viewpoint-rotation.png)

## Use case

A user may wish to view the map in an orientation other than north-facing.

## How to use the sample

Use the slider to rotate the map. If the map is not pointed north, the slider will show the heading relative to north in degrees. Slide the slider to zero to set the map's heading back to north.

## How it works

1. Instantiate an `ArcGISMap` object.
2. Set the map to a `MapView` object.
3. Use `awaitSetViewpointRotation(...)` to set the rotation angle.

## Relevant API

* ArcGISMap
* MapView

## Tags

rotate, rotation, viewpoint
