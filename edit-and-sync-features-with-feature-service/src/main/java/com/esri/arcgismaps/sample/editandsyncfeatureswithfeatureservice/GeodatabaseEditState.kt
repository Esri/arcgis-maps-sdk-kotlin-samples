package com.esri.arcgismaps.sample.editandsyncfeatureswithfeatureservice

/**
 * Enum class to track editing of features
 */
enum class GeodatabaseEditState {
    NOT_READY,  // Geodatabase has not yet been generated
    EDITING,  // A feature is in the process of being moved
    READY // The geodatabase is ready for synchronization or further edits
}