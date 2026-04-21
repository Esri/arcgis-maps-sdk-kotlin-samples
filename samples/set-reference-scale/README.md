# Set reference scale

Set the map's reference scale and which feature layers should honor the reference scale.

![Image of Set reference scale sample](set-reference-scale.png)

## Use case

Setting a reference scale on a map fixes the size of symbols and text to the desired height and
width at that scale. As you zoom in and out, symbols and text will increase or decrease in size
accordingly. When no reference scale is set, symbol and text sizes remain the same size relative to
the map view.

Map annotations are typically only relevant at certain scales. For instance, annotations to a map
showing a construction site are only relevant at that construction site's scale. When the map is
zoomed out, that information shouldn't scale with the map view but should instead remain scaled with
the map.

## How to use the sample

When the sample loads, tap or click the "Map Settings" button. Use the "Reference Scale" picker to
set the map's reference scale (1:500,000, 1:250,000, 1:100,000, or 1:50,000). Then tap or click
the "Set to Reference Scale" button to set the map scale to the reference scale.

Tap or click "Layers" to show a list of the map's feature layers. Tap or click a layer to toggle
whether that layer should honor the reference scale. Tap or click Done to dismiss the settings view.

## How it works

1. Get and set the `referenceScale` property on the `ArcGISMap` object.
2. Get and set the `scaleSymbols` property on individual `FeatureLayer` objects.

## Relevant API

* FeatureLayer
* Map

## Additional information

The map reference scale should normally be set by the map's author and not exposed to the end user
like it is in this sample.

## Tags

map, reference scale, scene
