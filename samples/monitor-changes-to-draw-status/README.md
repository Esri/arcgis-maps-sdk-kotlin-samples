# Monitor changes to draw status

Get the draw status of your MapView to know when all layers in the map have finished drawing.

Image: monitor-changes-to-draw-status.png

## Use case

Display a progress indicator while layers are drawing.

## How to use the sample

1. Pan and zoom around the map.
2. Observe the draw status text in the toolbar and the progress indicator while the map is drawing.

## How it works

1. Create an ArcGISMap.
2. Pass the ArcGISMap to the Compose MapView.
3. Use the MapView onDrawStatusChanged callback to receive DrawStatus updates and forward them to the ViewModel.
4. The ViewModel exposes a flow that indicates whether the map is drawing; the UI observes that flow and shows/hides UI elements accordingly.

## Relevant API

- DrawStatus
- MapView
- ArcGISMap

## Tags

draw, loading, map, render