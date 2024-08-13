# Edit features using feature forms

Display and edit feature attributes using feature forms

![Image of edit features using feature forms](edit-features-using-feature-forms.png)

## Use case

Feature forms help enhance the accuracy, efficiency, and user experience of attribute editing in your application.  Forms can be authored as part of the WebMap using Field Maps Designer or using Web Map Viewer. This allows for a simplified user experience to edit feature attribute data on the web-map.  

## How to use the sample

Tap a feature on the feature form map to open a bottom sheet displaying the list of form elements. Select through the list of elements to view the contingent value field groups and edit elements to update the field values. Tap the submit icon to commit the changes on the web map.

## How it works

1. Add a feature form enabled web-map to the MapView using `PortalItem` URL and itemID.
2. When the map is tapped, perform an identity operation to check if the tapped location is an `ArcGISFeature` and the `FeatureLayer.featureFormDefinition` is not null, indicating the feature layer does have an associated feature form definition.
3. Create a `FeatureForm()` object using the identified `ArcGISFeature` and the `FeatureLayer.featureFormDefinition`.
4. On the screen within a bottom sheet, use the `FeatureForm` Toolkit component to display the feature form configuration by providing the created `featureForm` object.
5. Optionally, you can add a `validationErrorVisibility` option to the `FeatureForm` Toolkit component that determines the behavior of when the validation errors are visible.
6. Once edits are added to the form fields, check to verify that there are no validation errors using `featureForm.validationErrors`. The list will be empty if there are no errors.
7. To commit edits on the service geodatabase:
    1. Call `featureForm.finishEditing()` to save edits to the database.
    2. Retrieve the backing service feature table's geodatabase using `(featureForm.feature.featureTable as? ServiceFeatureTable).serviceGeodatabase`.
    3. Verify the service geodatabase can commit changes back to the service using `serviceGeodatabase.serviceInfo?.canUseServiceGeodatabaseApplyEdits`
    4. If apply edits are allowed, call `serviceGeodatabase.applyEdits()` to apply local edits to the online service.

## Relevant API

* ArcGISFeature
* FeatureForm
* FeatureLayer
* FieldFormElement
* GroupFormElement
* ServiceFeatureTable

## Additional information

This sample uses the FeatureForm and GeoViewCompose Toolkit modules to be able to implement a Composable MapView which displays a Composable FeatureForm UI.

## Tags

compose, edits, feature, featureforms, form, geoviewcompose, jetpack, toolkit
