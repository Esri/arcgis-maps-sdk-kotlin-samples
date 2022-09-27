/* Copyright 2022 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgisruntime.sample.cutgeometry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import arcgisruntime.ApiKey
import arcgisruntime.ArcGISRuntimeEnvironment
import arcgisruntime.geometry.*
import arcgisruntime.geometry.GeometryEngine.cut
import arcgisruntime.mapping.ArcGISMap
import arcgisruntime.mapping.BasemapStyle
import arcgisruntime.mapping.Viewpoint
import arcgisruntime.mapping.symbology.SimpleFillSymbol
import arcgisruntime.mapping.symbology.SimpleFillSymbolStyle
import arcgisruntime.mapping.symbology.SimpleLineSymbol
import arcgisruntime.mapping.symbology.SimpleLineSymbolStyle
import arcgisruntime.mapping.view.Graphic
import arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.sample.cutgeometry.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISRuntimeEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val mapView = activityMainBinding.mapView
        lifecycle.addObserver(mapView)

        // set the map to be displayed in this view
        mapView.map = ArcGISMap(BasemapStyle.ArcGISTopographic)

        // create a graphic overlay
        val graphicsOverlay = GraphicsOverlay
        mapView.graphicsOverlays.add(graphicsOverlay)

        // create a blue polygon graphic to cut
        val polygonGraphic = Graphic(
            createLakeSuperiorPolygon(),
            SimpleFillSymbol(
                SimpleFillSymbolStyle.Solid, 0x220000FF,
                SimpleLineSymbol(SimpleLineSymbolStyle.Solid, -0xffff01, 2F)
            )
        )
        graphicsOverlay.graphics.add(polygonGraphic)

        // create a red polyline graphic to cut the polygon
        val polylineGraphic = Graphic(
            createBorder(), SimpleLineSymbol(
                SimpleLineSymbolStyle.Dot,
                -0x10000, 3F
            )
        )
        graphicsOverlay.graphics.add(polylineGraphic)
        // zoom to show the polygon graphic
        mapView.setViewpoint(Viewpoint(polygonGraphic.geometry!!))

        val cutButton = activityMainBinding.cutButton

        // create a button to perform the cut operation
        cutButton.setOnClickListener {
            val parts: List<Geometry> =
                cut(polygonGraphic.geometry!!, polylineGraphic.geometry as Polyline)

            // create graphics for the US and Canada sides
            val canadaSide = Graphic(
                parts[0], SimpleFillSymbol(
                    SimpleFillSymbolStyle.BackwardDiagonal,
                    -0xff0100, SimpleLineSymbol(SimpleLineSymbolStyle.Null, -0x1, 0F)
                )
            )
            val usSide = Graphic(
                parts[1], SimpleFillSymbol(
                    SimpleFillSymbolStyle.ForwardDiagonal,
                    -0x100, SimpleLineSymbol(SimpleLineSymbolStyle.Null, -0x1, 0F)
                )
            )
            graphicsOverlay.graphics.addAll(listOf(canadaSide, usSide))
            cutButton.isEnabled = false
        }
    }

    /**
     * Creates a polyline along the US/Canada border over Lake Superior.
     *
     * @return polyline
     */
    private fun createBorder(): Polyline {
        val points = PointCollection(SpatialReference.webMercator()).apply {
            add(Point(-9981328.687124, 6111053.281447))
            add(Point(-9946518.044066, 6102350.620682))
            add(Point(-9872545.427566, 6152390.920079))
            add(Point(-9838822.617103, 6157830.083057))
            add(Point(-9446115.050097, 5927209.572793))
            add(Point(-9430885.393759, 5876081.440801))
            add(Point(-9415655.737420, 5860851.784463))
        }
        return Polyline(points)
    }

    /**
     * Creates a polygon of points around Lake Superior.
     *
     * @return polygon
     */
    private fun createLakeSuperiorPolygon(): Polygon {
        val points = PointCollection(SpatialReference.webMercator()).apply {
            add(Point(-10254374.668616, 5908345.076380))
            add(Point(-10178382.525314, 5971402.386779))
            add(Point(-10118558.923141, 6034459.697178))
            add(Point(-9993252.729399, 6093474.872295))
            add(Point(-9882498.222673, 6209888.368416))
            add(Point(-9821057.766387, 6274562.532928))
            add(Point(-9690092.583250, 6241417.023616))
            add(Point(-9605207.742329, 6206654.660191))
            add(Point(-9564786.389509, 6108834.986367))
            add(Point(-9449989.747500, 6095091.726408))
            add(Point(-9462116.153346, 6044160.821855))
            add(Point(-9417652.665244, 5985145.646738))
            add(Point(-9438671.768711, 5946341.148031))
            add(Point(-9398250.415891, 5922088.336339))
            add(Point(-9419269.519357, 5855797.317714))
            add(Point(-9467775.142741, 5858222.598884))
            add(Point(-9462924.580403, 5902686.086985))
            add(Point(-9598740.325877, 5884092.264688))
            add(Point(-9643203.813979, 5845287.765981))
            add(Point(-9739406.633691, 5879241.702350))
            add(Point(-9783061.694736, 5922896.763395))
            add(Point(-9844502.151022, 5936640.023354))
            add(Point(-9773360.570059, 6019099.583107))
            add(Point(-9883306.649729, 5968977.105610))
            add(Point(-9957681.938918, 5912387.211662))
            add(Point(-10055501.612742, 5871965.858842))
            add(Point(-10116942.069028, 5884092.264688))
            add(Point(-10111283.079633, 5933406.315128))
            add(Point(-10214761.742852, 5888134.399970))
            add(Point(-10254374.668616, 5901877.659929))
        }
        return Polygon(points)
    }
}
