# Show line of sight between geoelements

Show a line of sight between two moving objects.

![Image of Show Line of Sight Between Geoelements](show-line-of-sight-between-geoelements.png)

## Use case

A line of sight between geoelements (i.e. observer and target) will not remain constant whilst one or both are on the move.

A line of sight is therefore useful in cases where visibility between two geoelements requires monitoring over a period of time in a partially obstructed field of view (such as buildings in a city).

## How to use the sample

A line of sight will display between a point on the Empire State Building (observer) and a taxi (target). The taxi will drive around a block and the line of sight should automatically update. The taxi will be highlighted and blinking when it is visible. A red segment on the line means the view between observer and target is obstructed, whereas cyan means the view is unobstructed. You can change the observer height with the slider to see how it affects the target's visibility.

## How it works

1. Instantiate an `AnalysisOverlay` and add it to the `SceneView`'s analysis overlays collection.
2. Instantiate a `GeoElementLineOfSight`, passing in observer and target `GeoElement`s (features or graphics). Add the line of sight to the analysis overlay's analysis collection.
3. To get the target visibility when it changes, react to the target visibility changing on the `GeoElementLineOfSight` instance.

## Relevant API

* AnalysisOverlay
* GeoElementLineOfSight
* LineOfSight.TargetVisibility

## Additional information

This sample uses the GeoViewCompose Toolkit module to be able to implement a Composable SceneView.

## Tags

3D, geoviewcompose, line of sight, visibility, visibility analysis
