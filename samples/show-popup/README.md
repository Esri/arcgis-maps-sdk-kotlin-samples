# Show popup

Show a predefined popup from a web map.

![Show popup screenshot](show-popup.png)

## Use case

Many web maps contain predefined popups which are used to display the attributes associated with each feature layer in the map, such as hiking trails, land values, or unemployment rates. You can display text, attachments, images, charts, and web links. Rather than creating new popups to display information, you can easily access and display the predefined popups.

## How to use the sample

Tap on the features to prompt a popup that displays information about the feature.

## How it works

1. Create and load an `ArcGISMap` using a URL.
2. Create a `MapViewProxy` and set it to the composable `MapView`.
3. Use the `mapViewProxy.identify(...)` function to identify the top-most feature.
4. From the identified feature get the `Popup`.
5. Pass the `Popup` to the composable `Popup` to display it.

## Relevant API

* ArcGISMap
* IdentifyLayerResult
* Popup

## About the data

This sample uses a [feature layer](https://www.arcgis.com/apps/mapviewer/index.html?webmap=9f3a674e998f461580006e626611f9ad) that displays reported information about mountains in the Sierra Nevada.

## Additional information

This sample uses the [Popup Toolkit module](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/tree/main/toolkit/popup) to offer an out of the box solution for displaying popup information in the UI. This sample also uses the GeoView-Compose Toolkit module to be able to implement a composable MapView.

## Tags

feature, feature layer, popup, toolkit, web map
