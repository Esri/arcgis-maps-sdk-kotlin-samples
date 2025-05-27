# Display map from portal item

Display a web map from an ArcGIS Online portal item.

![Screenshot of display map from portal item](display-scene.png)

## Use case

Display web maps stored on ArcGIS Online by referencing their portal item IDs.

## How to use the sample

Select a map from the dropdown menu to display it in the map view. The map will update to show the selected web map.

## How it works

1. Create a `Portal` instance for ArcGIS Online.
2. Create a `PortalItem` using the portal and the web map's item ID.
3. Create an `ArcGISMap` from the `PortalItem`.
4. Set the map to the `MapView`.

## Relevant API

- ArcGISMap
- MapView
- Portal
- PortalItem

## About the data

The web maps accessed by this sample show:
- [Geology for United States](https://arcgis.com/home/item.html?id=92ad152b9da94dee89b9e387dfe21acd)
- [Terrestrial Ecosystems of the World](https://arcgis.com/home/item.html?id=5be0bc3ee36c4e058f7b3cebc21c74e6)
- [Recent Hurricanes, Cyclones and Typhoons](https://arcgis.com/home/item.html?id=064f2e898b094a17b84e4a4cd5e5f549)

## Tags

portal item, web map