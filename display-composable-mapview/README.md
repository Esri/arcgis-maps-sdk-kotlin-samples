# Display Composable MapView

Display a map using Jetpack Compose.

![Image of display composable mapview](display-composable-mapview.png)

## Use case

Android Jetpack Compose is designed to be more efficient and performant than XML layouts. It allows you to define UI in a more declarative and composable way. Compose is also designed to work seamlessly with Kotlin. Visit the Android doc, [why adopt Compose](https://developer.android.com/jetpack/compose/why-adopt)?

## How to use the sample

Run the sample to view the map. Pan and zoom to navigate the map.

## How it works

1. Create a `@Composable` function to wrap the `MapView`
2. Get the context using `LocalContext.current`
3. Set the `MapView` to be evaluated only once during composition with `remember { MapView(context) }`
5. Use `AndroidView` to wrap a classic Android view in a Compose UI. 
6. Set the modifier to the max size in the Compose UI tree using `modifier.fillMaxSize()`
7. Provide the Android view using `factory = { mapView }`
8. Add the composable content to the Activity using `setContent { }` 

## Relevant API

* ArcGISMap
* BasemapStyle
* MapView

## Tags

basemap, compose, jetpack, map
