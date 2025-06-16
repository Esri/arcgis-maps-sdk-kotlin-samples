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
            //TODO - not dealt with z values

            val srBuilder = SpatialReferenceBuilder(geometry.spatialReference?.wkid ?: 0)
            srBuilder.resolution = srBuilder.resolution / 5
            srBuilder.tolerance = srBuilder.tolerance / 5
            val customSr = srBuilder.toSpatialReference()

            var retVal: Geometry? = null
            when (geometry) {
                is Point -> {
                    retVal = Point(
                        geometry.x, geometry.y,
                        spatialReference = customSr
                    )
                }

                is Envelope -> {
                    retVal = Envelope(
                        geometry.xMin, geometry.yMin,
                        geometry.xMax, geometry.yMax,
                        spatialReference = customSr
                    )
                }

                else -> throw (Throwable("Unsupported geometry type"))
            }

//            assertThat(retVal).isNotNull()
//            assertThat(retVal!!.spatialReference).isNotNull()
//            assertThat(retVal.spatialReference?.tolerance).isEqualTo(srBuilder.tolerance)

            return retVal
        }
    }
}