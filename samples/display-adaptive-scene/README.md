# Display adaptive scene

Demonstrates a modern adaptive SceneView layout built with Compose Material3 adaptive panes.

## What this sample shows

- A **main SceneView pane** that remains the primary focus area.
- A **supporting controls pane** with real-time scene controls (switches and sliders).
- A **floating widget pane** that can be shown/hidden from the supporting pane and dragged within the scene pane.

## Adaptive behavior

- Uses `NavigableSupportingPaneScaffold` to adapt between compact and larger windows.
- On compact layouts, users can open/close controls with a dedicated button.
- On larger layouts, scene and controls can be shown side-by-side.
- The floating widget stays non-modal so scene changes remain visible while interacting with controls.

## Controls

- Atmosphere effect toggle.
- Camera heading slider.
- Camera pitch slider.
- Camera distance slider.
- Reset camera action.

