# Download preplanned map area

Take a map offline using a preplanned map area.

![Image of download preplanned map area](download-preplanned-map-area.png)

## Use case

Generating offline maps on demand for a specific area can be time consuming for users and a processing load on the server. If areas of interest are known ahead of time, a web map author can pre-create packages for these areas. This way, the generation only needs to happen once, making the workflow more efficient for users and servers.

An archeology team could define preplanned map areas for dig sites which can be taken offline for field use. To see the difference, compare this sample to the "Generate offline map" sample.

## How to use the sample

Select a map area from the Preplanned map areas list. The download progress will be shown and when a download is complete it will be displayed in the the map view.

## How it works

1. Open the online `ArcGISMap` from a `PortalItem` and display it.
2. Create an `OfflineMapTask` using the portal item.
3. Get the `PreplannedMapArea`s from the task.
4. To download a selected map area, create default `DownloadPreplannedOfflineMapParameters` from the `OfflineMapTask` using the selected preplanned map area.
5. Set the update mode of the preplanned map area.
6. Use the parameters and a local path to create a `DownloadPreplannedOfflineMapJob` from the task.
7. Start the `DownloadPreplannedOfflineMapJob`. Once it has completed, get the  `DownloadPreplannedOfflineMapResult`.
8. Get the `ArcGISMap` from the result and display it in the `MapView`.

## Relevant API

* DownloadPreplannedOfflineMapJob
* DownloadPreplannedOfflineMapParameters
* DownloadPreplannedOfflineMapResult
* OfflineMapTask
* PreplannedMapArea

## About the data

The [Naperville stormwater network map](https://arcgisruntime.maps.arcgis.com/home/item.html?id=acc027394bc84c2fb04d1ed317aac674) is based on ArcGIS Solutions for Stormwater and provides a realistic depiction of a theoretical stormwater network.

## Additional information

`PreplannedUpdateMode` can be used to set the way the preplanned map area receives updates in several ways:

* `NoUpdates` - No feature updates will be performed.
* `DownloadScheduledUpdates` - Scheduled, read-only updates will be downloaded from the online map area and applied to the local mobile geodatabases.
* `DownloadScheduledUpdatesAndUploadNewFeatures` - Scheduled, read-only updates are downloaded from the online map area and applied to the local mobile geodatabases. Newly added features can also be uploaded to the feature service.
* `SyncWithFeatureServices` - Changes, including local edits, will be synced directly with the underlying feature services.

For more information about offline workflows, see [Offline maps, scenes, and data](https://developers.arcgis.com/documentation/mapping-apis-and-location-services/offline/) in the *ArcGIS Developers* guide.

## Tags

map area, offline, pre-planned, preplanned
