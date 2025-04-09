# Filter features in scene

Filter 3D scene features out of a given geometry with a polygon filter.

![Filter features in scene screenshot](filter-features-in-scene.png)

## Use case

You can directly control what users see within a specific scene view to give a more focused or cleaner user experience by using a `SceneLayerPolygonFilter` to selectively show or hide scene features within a given area.

## How to use the sample

The sample initializes showing overlapping datasets of 3D buildings from the OpenStreetMap layer and an additional detailed scene layer of buildings in San Francisco. Notice how the two scene layers overlap and clip into each other. Click the "Filter OSM buildings" button, to set a `SceneLayerPolygonFilter` and filter out the OpenStreetMap buildings within the extent of the detailed buildings scene. Notice how the OSM buildings within and intersecting the extent of the detailed buildings layer are hidden.

## How it works

1. Construct an `ArcGISScene` and add a `Surface` elevation source set to the World Elevation 3D as an elevation source.
2. Add the two `ArcGISSceneLayer`s building scene layers to the `ArcGISScene`s operational layers.
3. Construct a `SceneLayerPolygonFilter` with the extent of the San Francisco Buildings Scene Layer and the `SceneLayerPolygonFilterSpatialRelationship.Disjoint` object to hide all features within the extent.
4. Set the `SceneLayerPolygonFilter` on the OSM Buildings layer to hide all OSM buildings within the extent of the San Francisco Buildings layer.

## Relevant API

* ArcGISSceneLayer
* SceneLayerPolygonFilter
* SceneLayerPolygonFilterSpatialRelationship

## About the data

This sample uses the [OpenStreetMap 3D Buildings](https://www.arcgis.com/home/item.html?id=ca0470dbbddb4db28bad74ed39949e25) which provides generic 3D outlines of buildings throughout the world. It is based on the OSM Daylight map distribution and is hosted by Esri. It uses the [San Francisco 3D Buildings](https://www.arcgis.com/home/item.html?id=d3344ba99c3f4efaa909ccfbcc052ed5) scene layer which provides detailed 3D models of buildings in San Francisco, California, USA.

## Additional information

This sample uses `SceneLayerPolygonFilterSpatialRelationship.Disjoint` to hide all features within the extent of the given geometry. You can alternatively use `SceneLayerPolygonFilterSpatialRelationship.Contains` to only show features within the extent of the geometry.

You can also show or hide features in a scene layer using `ArcGISSceneLayer.setFeatureVisible(...)` and pass in a feature or list of features and a boolean value to set their visibility.

## Tags

3D, buildings, disjoint, exclude, extent, filter, hide, OSM, polygon
