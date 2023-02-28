# Apply dictionary renderer to feature layer

Convert features into graphics to show them with mil2525d symbols.

![Image of apply dictionary renderer to feature layer](apply-dictionary-renderer-to-feature-layer.png)

## Use case

A dictionary renderer uses a style file along with a rule engine to display advanced symbology. This is useful for displaying features using precise military symbology.

## How to use the sample

Pan and zoom around the map. Observe the displayed military symbology on the map.

## How it works

1. Create a `Geodatabase` using `new Geodatabase(geodatabasePath)`.
2. Load the geodatabase using `Geodatabase.load()`.
3. Instantiate a `DictionarySymbolStyle`  using `DictionarySymbolStyle.createFromFile(dictionarySymbolStylePath)`.
4. Load the symbol dictionary using `dictionarySymbolStyle.load()`.
5. For each `GeoDatabaseFeatureTable` in the `GeoDatabase`, create a feature layer with it, then add it to the map using `map.operationalLayers.add(featureLayer)`.
6. Create `DictionaryRenderer(dictionarySymbolStyle)` and assign it to the feature layer renderer.
7. Set the viewpoint of the map view to the extent of the feature layer using `mapView.setViewpoint(Viewpoint(featureLayer.fullExtent))`.

## Relevant API

* DictionaryRenderer
* DictionarySymbolStyle
* FeatureLayer
* Geodatabase

## About the data

The sample uses a [mil2525d stylx file](https://www.arcgis.com/home/item.html?id=c78b149a1d52414682c86a5feeb13d30) and a [mobile geodatabase](https://www.arcgis.com/home/item.html?id=e0d41b4b409a49a5a7ba11939d8535dc) from ArcGIS Online.

## Tags

military, symbol
