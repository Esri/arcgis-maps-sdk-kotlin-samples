# Edit feature attachments

Add, delete, and download attachments for features from a service.

![Image of edit feature attachments](edit-feature-attachments.png)

## Use case

Attachments provide a flexible way to manage additional information that is related to your features. Attachments allow you to add files to individual features, including: PDFs, text documents, or any other type of file. For example, if you have a feature representing a building, you could use attachments to add multiple photographs of the building taken from several angles, along with PDF files containing the building's deed and tax information.

## How to use the sample

Tap a feature on the map to open a bottom sheet displaying the number of attachments. Select an entry from the list to download and view the attachment in the gallery. Tap on the add attachment button to add an attachment or long press to delete.

## How it works

1. Create a `ServiceFeatureTable` from a URL.
2. Create a `FeatureLayer` object from the service feature table.
3. Select features from the feature layer with `selectFeature`.
4. To fetch the feature's attachments, cast to an `ArcGISFeature` and use `ArcGISFeature.fetchAttachments()`.
5. To add an attachment to the selected ArcGISFeature, create an attachment and use `ArcGISFeature.addAttachment()`.
6. To delete an attachment from the selected ArcGISFeature, use the `ArcGISFeature.deleteAttachment()`.
7. After a change, apply the changes to the server using `ServiceFeatureTable.applyEdits()`.

## Relevant API

* ArcGISFeature.deleteAttachment
* ArcGISFeature.fetchAttachments
* Attachment.fetchData
* FeatureLayer
* ServiceFeatureTable
* ServiceFeatureTable.applyEdits
* ServiceFeatureTable.updateFeature

## Additional information

Attachments can only be added to and accessed on service feature tables when their hasAttachments property is true.

## Tags

edit and manage data, image, jpeg, pdf, picture, png, txt
