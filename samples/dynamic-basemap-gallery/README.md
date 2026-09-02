# Dynamic base map gallery

Dynamic base map gallery

![Image of cut geometry](cut-geometry.png)

## Use case

Multi-use and/or international applications benefit from the ability to change a basemap's style or localize the basemap.

## How to use the sample

Press "Basemap" to display a gallery of all styles available in the basemap styles service. Select a style using the "Style" picker. Select a language or language strategy using the "Language" picker. Optionally selected a worldview using the "Worldview" picker. Disabled pickers indicate that the customization cannot be applied to the selected style.

## How it works

1. Call `BasemapStylesService.load()` and wrap each returned `BasemapStyleInfo` in a `BasemapGalleryItem` to populate the `BasemapGallery` toolkit composable.
2. Tap the floating action button to open a popup containing the `BasemapGallery`, and select a basemap style item.
3. Choose a language from the selected style's `BasemapStyleInfo.languages`, and optionally a worldview from its `BasemapStyleInfo.worldviews`.
4. On "Done", create `BasemapStyleParameters` with `languageStrategy = BasemapStyleLanguageStrategy.Specific(Locale.forLanguageTag(languageCode))` and, if chosen, `worldview`.
5. Create a `Basemap` from the selected `BasemapStyle` and the `BasemapStyleParameters`, and apply it with `ArcGISMap.setBasemap()`.

## Relevant API

* basemapgallery

## Additional information

This sample uses the basemapgallery Toolkit module to be able to implement a composable MapView.

## Tags

basemapgallery map
