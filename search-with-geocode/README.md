# Search with geocode

Find the location for an address or places of interest near a location or within a specific area.

![Screenshot for search with geocode sample](search-with-geocode.png)

## Use case

You can input an address into the app's search bar and zoom to the address location. If you do not know the specific address, you can get suggestions and locations for places of interest (POIs) with a natural language query for what the place has ("food"), the type of place ("gym"), or the generic place name ("Coffee"), rather than the specific address. Additionally, you can filter the results to a specific area.

## How to use the sample

Enter an address and optionally choose from the list of suggestions to show its location as a pin graphic. Tap on a result pin to display the address. If you pan away from the result area, a "Repeat Search Here" button will appear. Tap it to query again for the currently viewed area on the map.

## How it works

1. Create a `LocatorSearchSource` using the toolkit.
2. Create a `SearchView` using the toolkit with the locator search source.
3. Perform a search.
4. Identify a result pin graphic in the graphics overlay and show a callout with its address, using the `callout(placement:content:)` map view modifier.
5. The search toolkit component handles the geocoding mechanism under the hood.
    * If not specified, the component creates a `LocatorSearchSource` with the default Esri geocoding service.
    * It creates default `GeocodeParameters` for the locator task and specifies the geocode's attributes.
    * It also creates `SuggestParameters` to get place of interest (POI) suggestions based on a place name query.
    * It supports searching a specific area and "Repeat search here" behaviors out of out-of-the-box. Configure these behaviors when you initialize the search view.

## Relevant API

* GeocodeParameters
* LocatorTask
* SearchResult
* SearchSuggestion
* SuggestParameters

## Additional information

This sample uses the World Geocoding Service. For more information, see [Geocoding service](https://developers.arcgis.com/documentation/mapping-apis-and-services/search/services/geocoding-service/) from ArcGIS Developer website.

## Tags

address, businesses, geocode, locations, locator, places of interest, POI, point of interest, search, suggestions, toolkit
