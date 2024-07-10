# Find closest facility from point

Find a route to the closest facility from a location.

![Image of Find Closest Facility from Point](find_closest_facility_from_point.png)

## Use case

Quickly and accurately determining the most efficient route between a location and a facility is a frequently encountered task. For example, a paramedic may need to know which hospital in the vicinity offers the possibility of getting an ambulance patient critical medical care in the shortest amount of time. Solving for the closest hospital to the ambulance's location using an impedance of "travel time" would provide this information.

## How to use the sample

Tap near any of the hospitals and a route will be displayed from that tapped location to the nearest hospital.

## How it works

1. Create a `ClosestFacilityTask` using an URL from an online network analysis service.
2. Get `ClosestFacilityParameters` from task, `closestFacilityTask.createDefaultParameters()`.
3. Add the list of facilities to parameters, `closestFacilityParameters.setFacilities(facilitiesList)`.
4. Add the incident to parameters, `closestFacilityParameters.setIncidents(listOf(incidentPoint))`.
5. Get `ClosestFacilityResult` from solving task with parameters, `closestFacilityTask.solveClosestFacility(closestFacilityParameters)`.
6. Get index list of closet facilities to incident, `closestFacilityResult.getRankedFacilityIndexes(incidentIndex)`.
7. Get index of closest facility, `rankedFacilitiesList[0]`.
8. Find closest facility route, `closestFacilityResult.getRoute(closestFacilityIndex, incidentIndex)`.
9. Display route to `MapView`:
   * Create `Graphic` from route geometry, `Graphic(geometry = route?.routeGeometry, symbol = routeSymbol)`.
   * Add graphic to a `GraphicsOverlay` and add it to the MapView's graphic overlays collection.

## Relevant API

* ClosestFacilityParameters
* ClosestFacilityResult
* ClosestFacilityRoute
* ClosestFacilityTask
* Facility
* Graphic
* GraphicsOverlay
* Incident
* MapView

## Additional information

This sample uses the GeoViewCompose Toolkit module to be able to implement a Composable MapView.

## Tags

geoviewcompose, incident, network analysis, route, search
