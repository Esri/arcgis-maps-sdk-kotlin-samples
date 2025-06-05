# Display overview map

Include an overview or inset map as an additional map view
to show the wider context of the primary view.

![Image of display overview map](display-overview-map.png)

## Use case

An overview map provides a useful, smaller-scale overview of the current map view's location.
For example, when you need to inspect a layer with many features while remaining aware of the
wider context of the view, use an overview map to help show the extent of the main map view.

## How to use the sample

Pan or zoom across the map view to browse through the tourist attractions feature layer and
notice the viewpoint and scale of the linked overview map update automatically.

## How it works

1. Create States to hold the current viewpoint and current visible area of the map.
2. Instantiate a `FeatureLayer` to display the tourist attraction features.
3. Create an `ArcGISMap` object, set its `initialViewpoint` to the initial value of the viewpoint State, and add the `FeatureLayer` into its `operationalLayers`.
4. In the user-interface, declare a `MapView` to display the `ArcGISMap`. Use `onViewpointChangedForCenterAndScale` and `onVisibleAreaChanged` to keep the viewpoint and visible area States up to date.
5. In the user-interface, declare an `OverviewMap` object from the ArcGIS Maps SDK Toolkit. Set its `viewpoint` and `visibleArea` to the previously created States.

## Relevant API

* ArcGISMap
* MapView
* OverviewMap

## About the data

The data used in this sample is the [OpenStreetMap Tourist Attractions for North America](https://www.arcgis.com/home/item.html?id=addaa517dde346d1898c614fa91fd032) feature layer, which is scale-dependent and displays at scales larger than 1:160,000.

## Additional information

This sample uses the overview map toolkit component, which requires the [toolkit](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit) to be cloned and set up locally.

## Tags

context, inset, map, minimap, overview, preview, small scale, toolkit, view
