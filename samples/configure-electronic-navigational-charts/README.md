# Configure electronic navigational charts

Display and configure electronic navigational charts per ENC specification.

![Screenshot of Configure electronic navigational charts sample](configure-electronic-navigational-charts.png)

## Use case

The S-52 standard defines how Electronic Navigational Chart (ENC) content should be displayed to ensure that data is presented consistently across every charting system. S-52 defines several display options, including variations on symbology to ensure that charts are readable both at night and in direct sunlight.

## How to use the sample

When opened, the sample displays an electronic navigational chart. Tap on the map to select ENC features and view the feature's acronyms and descriptions shown in a callout. Tap "Display Settings" and use the options to adjust some of the ENC mariner display settings, such as the colors and symbology.

## How it works

1. To display ENC content:
    1. On `EncEnvironmentSettings`, set `resourcePath` to the local hydrography data directory and `sencDataPath` to a temporary directory.
    2. Create an `EncExchangeSet` using URLs to the local ENC exchange set files and load it.
    3. Make an `EncCell` for each of the `EncExchangeSet.datasets` and then make an `EncLayer` from each cell.
    4. Add the layers to the map using `ArcGISMap.operationalLayers.add(encLayer)` to display the map.
2. To select ENC features:
    1. Use `onSingleTapConfirmed` on the map view to get the screen point from the tapped location.
    2. Create a `MapViewProxy` and use it to identify nearby features to the tapped location with `identifyLayers`.
    3. From the resulting `IdentifyLayerResult`, get the `EncLayer` from `layerContent` and the `EncFeature`(s) from `geoElements`.
    4. Use `EncLayer.selectFeature` to select the ENC feature(s).
3. To set ENC display settings:
    1. Get the `EncDisplaySettings` instance from `EncEnvironmentSettings.displaySettings`.
    2. Use `marinerSettings`, `textGroupVisibilitySettings`, and `viewingGroupSettings` to access the settings instances and set their properties.
    3. Reset the display settings using `resetToDefaults()` on the settings instances.

## Relevant API

* EncCell
* EncDataset
* EncDisplaySettings
* EncEnvironmentSettings
* EncExchangeSet
* EncLayer
* EncMarinerSettings
* EncTextGroupVisibilitySettings
* IdentifyLayerResult

## Offline data

This sample downloads the [ENC Exchange Set without updates](https://www.arcgis.com/home/item.html?id=9d2987a825c646468b3ce7512fb76e2d) item from *ArcGIS Online* automatically.

The latest Hydrography Data can be downloaded from the [*Esri Developer* downloads](https://developers.arcgis.com/downloads/). The `S57DataDictionary.xml` file is contained there.

## Additional information

Read more about [displaying](https://developers.arcgis.com/swift/layers/display-electronic-navigational-charts/) and [deploying](https://developers.arcgis.com/swift/license-and-deployment/deployment/#enc-electronic-navigational-charts-style-directory) electronic navigational charts on *Esri Developer*.

## Tags

ENC, hydrography, identify, IHO, layers, maritime, nautical chart, S-52, S-57, select, settings, symbology
