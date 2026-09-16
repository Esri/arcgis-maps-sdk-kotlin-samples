# Geocode offline

Geocode addresses to locations and reverse geocode locations to addresses offline.

![Image of offline geocode](geocode-offline.png)

## Use case

You can use an address locator file to geocode addresses and locations. For example, you could provide offline geocoding capabilities to field workers repairing critical infrastructure in a disaster when network availability is limited.

## How to use the sample

Type the address in the Search menu option or select from the list to `Geocode` the address and view the result on the map. Tap the location you want to reverse geocode. Select the pin to highlight the `PictureMarkerSymbol` (i.e. single tap on the pin).

## How it works

1. Use the path of a .loc file to create a `LocatorTask` object.
2. Set up `GeocodeParameters` and call `geocode` to get geocode results.
3. Call `reversegeocode` passing the tapped location on the map to find the address with reverse geocoding.

## Relevant API

* GeocodeParameters
* GeocodeResult
* LocatorTask
* ReverseGeocodeParameters

## Offline data

This sample uses [locator data](https://www.arcgis.com/home/item.html?id=22c3083d4fa74e3e9b25adfc9f8c0496) and [supporting geocoding data](https://www.arcgis.com/home/item.html?id=3424d442ebe54f3cbf34462382d3aebe). They are downloaded from ArcGIS Online automatically.

## Tags

geocode, geocoder, locator, offline, package, query, search
