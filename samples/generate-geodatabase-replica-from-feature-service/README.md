# Generate geodatabase replica from feature service

Generate a local geodatabase replica from an online feature service.

![Image of generate geodatabase](generate-geodatabase-replica-from-feature-service.png)

## Use case

Generating geodatabase replica is the first step toward taking a feature service offline. It allows you to save features locally for offline display.

## How to use the sample

Zoom to any extent. Then click the generate button to generate a geodatabase of features from a feature service filtered to the current extent. A red outline will show the extent used. The job's progress is shown while the geodatabase is generated. When complete, the map will reload with only the layers in the geodatabase, clipped to the extent.

## How it works

1. Create a `GeodatabaseSyncTask` with the URL of the feature service and load it.
2. Create `GenerateGeodatabaseParameters` specifying the extent and whether to include attachments.
3. Create a `GenerateGeodatabaseJob` with `geodatabaseSyncTask.createGenerateGeodatabaseJob(parameters, downloadPath)`. Start the job with `job.start()`.
4. When the job is done, `job.result()` will return the geodatabase. Inside the geodatabase are feature tables which can be used to add feature layers to the map.
5. Call `geodatabaseSyncTask.unregisterGeodatabase(geodatabase)` after generation when you're not planning on syncing changes to the service.

## Relevant API

* GenerateGeodatabaseJob
* GenerateGeodatabaseParameters
* Geodatabase
* GeodatabaseSyncTask

## Additional information

This sample uses the GeoView-Compose Toolkit module to be able to implement a composable SceneView.

## Tags

disconnected, geoview-compose, local geodatabase, offline, replica, sync, toolkit
