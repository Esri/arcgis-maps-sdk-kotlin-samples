/*
 * COPYRIGHT 1995-2025 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */
package com.esri.arcgismaps.sample.sampleslib.components

import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.geometry.SpatialReferenceBuilder

class CustomSrUtils {
    companion object {
        /**
         * Returns a custom spatial reference with a resolution and tolerance
         * that are 1/5 of the original spatial reference's resolution and tolerance.
         *
         * @param geometry The geometry to create a custom spatial reference for.
         * @return A new [SpatialReference] with adjusted resolution and tolerance.
         */
        fun createCustomPrecisionGeometry(geometry: Geometry): Geometry {
            val customSr = createCustomPrecisionSpatialReference(geometry.spatialReference!!, geometry.hasZ)
            var retVal: Geometry? = null
            when (geometry) {
                is Point -> {
                    retVal = if (geometry.hasZ) {
                        // If the point has Z, we need to create a new Point with the custom SR
                        Point(
                            geometry.x, geometry.y, geometry.z,
                            spatialReference = customSr
                        )
                    } else {
                        // If the point does not have Z, we can just use x and y
                        Point(
                            geometry.x, geometry.y,
                            spatialReference = customSr
                        )
                    }
                }

                is Envelope -> {
                    retVal = if (geometry.hasZ) {
                        // If the envelope has Z, we need to create a new Envelope with the custom SR
                        Envelope(
                            geometry.xMin, geometry.yMin, geometry.xMax, geometry.yMax,
                            geometry.zMin, geometry.zMax,
                            spatialReference = customSr
                        )
                    } else {
                        // If the envelope does not have Z, we can just use xMin, yMin, xMax, yMax
                        Envelope(
                            geometry.xMin, geometry.yMin, geometry.xMax, geometry.yMax,
                            spatialReference = customSr
                        )
                    }
                }

                else -> throw (Throwable("Unsupported geometry type"))
            }

            return retVal
        }

        public fun createCustomPrecisionSpatialReference(spatialReference: SpatialReference, customVertical: Boolean): SpatialReference {
            val srBuilder = SpatialReferenceBuilder(spatialReference.wkid, spatialReference.verticalWkid)
            srBuilder.resolution = srBuilder.resolution / 5
            srBuilder.tolerance = srBuilder.tolerance / 4
            if (customVertical) {
                srBuilder.verticalResolution = srBuilder.verticalResolution / 3
                srBuilder.verticalTolerance = srBuilder.verticalTolerance / 2
            }
            return srBuilder.toSpatialReference()
        }
    }
}