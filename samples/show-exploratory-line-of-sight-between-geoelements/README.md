# Show exploratory line of sight between geoelements

Show an exploratory line of sight between two moving objects.

![Image of Show Exploratory Line of Sight Between Geoelements](show-exploratory-line-of-sight-between-geoelements.png)

## Use case

An exploratory line of sight between geoelements (i.e. observer and target) will not remain constant whilst one or both are on the move.

An `ExploratoryGeoElementLineOfSight` is therefore useful in cases where visibility between two geoelements requires monitoring over a period of time in a partially obstructed field of view (such as buildings in a city).

Note: This analysis is a form of "exploratory analysis", which means the results are calculated on the current scale of the data, and the results are generated very quickly but not persisted.

## How to use the sample

An exploratory line of sight will display between a point on the Empire State Building (observer) and a taxi (target).
The taxi will drive around a block and the exploratory line of sight should automatically update.
The taxi will be highlighted and blinking when it is visible. A red segment on the line means the view between observer and target is obstructed, whereas cyan means the view is unobstructed. You can change the observer height with the slider to see how it affects the target's visibility.

## How it works

1. Instantiate an `AnalysisOverlay` and add it to the `SceneView`'s analysis overlays collection.
2. Instantiate an `ExploratoryGeoElementLineOfSight`, passing in observer and target `GeoElement`s (features or graphics). Add the exploratory line of sight to the analysis overlay's analysis collection.
3. To get the target visibility when it changes, react to the target visibility changing on the `ExploratoryGeoElementLineOfSight` instance.

## Relevant API

* AnalysisOverlay
* ExploratoryGeoElementLineOfSight
* ExploratoryLineOfSightTargetVisibility

## Additional information

This sample uses the GeoView-Compose Toolkit module to be able to implement a composable SceneView.

## Tags

3D, geoview-compose, exploratory line of sight, visibility, visibility analysis
