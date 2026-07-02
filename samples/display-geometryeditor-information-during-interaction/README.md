# Display GeometryEditor information during interaction

See information about the previewed geometry during an interaction using the geometry editor.

![Image of geometry editor with rotation and scaling selection handles visible](display-geometryeditor-information-during-interaction.png)

## Use case

A field worker can see information about the geometry being created or edited during an editing interaction. This information can be used provide feedback to the user about the geometry so that they can see the effects of their interaction on the geometry as the interaction progresses.

## How to use the sample

Tap a graphic to edit its geometry by moving, rotating, or scaling the geometry. During the interaction, information about the changes will be displayed to provide feedback to the user.

Use the buttons in the settings view to undo or redo changes made to the geometry. Use the cancel and done buttons to discard and save changes.

## How it works

1. Create a `MapViewProxy` for interacting with the composable `MapView`.
2. Create a `MapView` with MapView using `MapView(mapViewProxy = MapViewProxy, geometryEditor = GeometryEditor, ...)`.
3. Add an event handler to listen to `GeometryEditor.interactionPreviewChanged`.
    * This event can be used to get information on the state of the geometry during an interaction with the `GeometryEditorInteractionPreview` parameter.
        * The `PreviewGeometry` represents the geometry's state at that moment.
        * The `InteractionType` can be used to determine the type of interaction that is occurring (`create`, `move`, `rotate`, `scale`).
        * The `InteractionElement` can be used to determine the element being interacted with (`GeometryEditorVertex`, `GeometryEditorPart`, `GeometryEditorGeometry`).
4. Start the `GeometryEditor` using `GeometryEditor.start(Geometry)` to edit the geometry of one of the graphics.
    * To retrieve the geometry of the graphic that is being used to visualize it, follow these steps:
       * Use `MapViewProxy.identifyGraphicsOverlays(...)` to identify graphics at the location of a tap.
       * Find the desired `IdentifyGraphicsOverlayResult` in the list returned by `MapViewProxy.identifyGraphicsOverlays(...)`.
       * Find the desired graphic in the `IdentifyGraphicsOverlayResult.graphics` list.
       * Access the geometry associated with the `Graphic` using `Graphic.geometry` - this will be used in the `GeometryEditor.start(Geometry)` method.
5. Check to see if undo and redo are possible during an editing session using `GeometryEditor.canUndo` and `GeometryEditor.canRedo`. If it's possible, use `GeometryEditor.undo()` and `GeometryEditor.redo()`.
6. Call `GeometryEditor.stop()` to finish the editing session, and use the geometry returned from this method to update the existing `Graphics.geometry`.

## Relevant API

* Geometry
* GeometryEditor
* GeometryEditorInteractionPreview
* GeometryEditorInteractionType
* GeometryEditor.interactionPreviewChanged
* Graphic
* GraphicsOverlay

## Additional information

The `GeometryEditor.InteractionPreviewChanged` event fires continuously during an interaction, therefore it's not recommended to use it as a trigger for resource intensive actions.

## Tags

draw, edit, geometry editor, interaction preview