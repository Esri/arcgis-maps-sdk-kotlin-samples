# Query features with arcade expression

Query features on a map using an Arcade expression.

![QueryFeaturesWithArcadeExpression](query-features-with-arcade-expression.png)

## Use case

Arcade is a portable, lightweight, and secure expression language used to create custom content in ArcGIS applications. Like other expression languages, it can perform mathematical calculations, manipulate text, and evaluate logical statements. It also supports multi-statement expressions, variables, and flow control statements. What makes Arcade particularly unique when compared to other expression and scripting languages is its inclusion of feature and geometry data types. This sample uses an Arcade expression to query the number of crimes in a neighborhood in the last 60 days.

## How to use the sample

Tap on any neighborhood to see the number of crimes in the last 60 days in a TextView.

## How it works

1. Create a `PortalItem` using the URL and ID.
2. Create an `ArcGISMap` using the portal item.
3. Create a `MapViewProxy` to handle user interaction with the map view.
4. Provide behaviour for the `MapView`'s `onSingleTapConfirmed` parameter to react to taps on the map.
5. Identify the visible layer where it is tapped using `mapViewProxy.identify()` and get the feature from the result.
6. Create the following `ArcadeExpression`:

   ```kotlin
   expressionValue = "var crimes = FeatureSetByName(\$map, 'Crime in the last 60 days');\n" +
    "return Count(Intersects(\$feature, crimes));"
   ```

7. Create an `ArcadeEvaluator` using the Arcade expression and `ArcadeProfile.FormCalculation`.
8. Create a map of profile variables with the following key-value pairs:

   ```kotlin
    mapOf<String, Any>("\$feature" to feature, "\$map" to mapView.map)
   ```

9. Call `ArcadeEvaluator.evaluate()` on the Arcade evaluator object and pass the profile variables map.
10. Get the `ArcadeEvaluationResult.result`.
11. Convert the result to a numerical value (`Double`) and pass it to the UI.

## Relevant API

* ArcadeEvaluationResult
* ArcadeEvaluator
* ArcadeExpression
* ArcadeProfile
* Portal
* PortalItem

## About the data

This sample uses the [Crimes in Police Beats Sample](https://www.arcgis.com/home/item.html?id=539d93de54c7422f88f69bfac2aebf7d) ArcGIS Online Web Map which contains 2 layers for city beats borders, and crimes in the last 60 days as recorded by the Rochester, NY police department.

## Additional information

This sample uses the `geoview-compose` module of the [ArcGIS Maps SDK for Kotlin Toolkit](https://developers.arcgis.com/kotlin/toolkit/) to implement a Composable MapView.

Visit [Getting Started](https://developers.arcgis.com/arcade/) on the *ArcGIS Developer* website to learn more about Arcade expressions.

## Tags

Arcade evaluator, Arcade expression, compose, identify layers, portal, portal item, query, toolkit
