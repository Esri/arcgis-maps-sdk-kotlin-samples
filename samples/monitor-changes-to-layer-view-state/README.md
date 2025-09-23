# Monitor changes to layer view state

Determine whether a layer is currently visible.

![Image of Monitor changes to layer view state](monitor-changes-to-layer-view-state.png)

## Use case

The view status includes information on the loading state of layers and whether layers are visible at a given scale. You might change how a layer is displayed in a layer list to communicate whether it is being viewed on the map. For example, you could show a loading spinner next to its name when the view status is `Loading`, gray out the name when `notVisible` or `OutOfScale`, show the name normally when `Active`, or with a warning or error icon when the status is `Warning` or `Error`.

## How to use the sample

When the feature layer is loaded, pan and zoom around the map. Note how the `LayerViewState` flags change. For example, the layer status `OutOfScale` is retrieved when the map is scaled outside the layer's min and max scale range. Tap the toggle to hide the layer and observe the view state change to `NotVisible`. Disconnect from the network and pan around the map to see the `Warning` status when the layer cannot fetch online data. Reconnect to the network to see the warning disappear.

## How it works

1. Create a `ArcGISMap` with an operational layer.
2. Use the `onLayerViewStateChanged()` callback on the map view to get updates to the layers' view states.
3. Get the `Layer` and the current view `status` of the `LayerViewState` defining the new state.

## Relevant API

* GeoViewLayerViewStateChanged
* Layer
* LayerViewState
* Map
* MapView

## About the data

The [Satellite (MODIS) Thermal Hotspots and Fire Activity](https://www.arcgis.com/home/item.html?id=b8f4033069f141729ffb298b7418b653) layer presents detectable thermal activity from MODIS satellites for the last 48 hours. [MODIS Global Fires](https://www.earthdata.nasa.gov/learn/find-data/near-real-time/firms/active-fire-data) is a product of NASA’s [Earth Observing System Data and Information System (EOSDIS)](https://www.earthdata.nasa.gov/eosdis), part of NASA's Earth Science Data. EOSDIS integrates remote sensing and GIS technologies to deliver global MODIS hotspot/fire locations to natural resource managers and other stakeholders around the World.

## Additional information

The following are the `LayerViewState.status` options:

* `Active`: The layer in the view is active.
* `NotVisible`: The layer in the view is not visible.
* `OutOfScale`: The layer in the view is out of scale. A status of `OutOfScale` indicates that the view is zoomed outside of the scale range of the layer. If the view is zoomed too far in (e.g., to a street level), it is beyond the max scale defined for the layer. If the view has zoomed too far out (e.g., to global scale), it is beyond the min scale defined for the layer.
* `Loading`: The layer in the view is loading. Once loading has completed, the layer will be available for display in the view. If there was a problem loading the layer, the status will be set to `Error`.
* `Error`: The layer in the view has an unrecoverable error. When the status is `Error`, the layer cannot be rendered in the view. For example, it may have failed to load, be an unsupported layer type, or contain invalid data.
* `Warning`: The layer in the view has a non-breaking problem with its display, such as incomplete information (e.g., by requesting more features than the max feature count of a service) or a network request failure.

## Tags

layer, load, map, status, view, visibility
