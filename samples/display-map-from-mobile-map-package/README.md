# Display map from mobile map package

Display a map from a mobile map package.

![Image of open mobile map package](display-map-from-mobile-map-package.png)

## Use case

An .mmpk file is an archive containing the data (specifically, basemaps and features) used to display an offline map.
A mobile map package may need to be unpacked to a directory to allow read support for certain data types.

## How to use the sample

When the sample opens, it will automatically display the map in the mobile map package. Pan and zoom to observe the data from the mobile map package.

## How it works

1. Create a `MobileMapPackage` specifying the path to the .mmpk file.
2. Load the mobile map package with `mapPackage.load()`.
3. After it successfully loads, get the map from the ".mmpk" and add it to the map view: `mapView.map = mapPackage.maps.first()`.

## Relevant API

* MapView
* MobileMapPackage

## About the data

This mobile map package shows points of interest within Yellowstone National Park. It is available for download [here on ArcGIS Online](https://arcgisruntime.maps.arcgis.com/home/item.html?id=e1f3a7254cb845b09450f54937c16061).

## Tags

mmpk, mobile map package, offline
