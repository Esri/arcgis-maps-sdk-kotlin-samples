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
import arcgisruntime.geometry.GeometryEngine
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
        val graphicsOverlay = GraphicsOverlay()
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
        polygonGraphic.geometry?.let { graphicGeometry ->
            mapView.setViewpoint(Viewpoint(graphicGeometry))
        }

        activityMainBinding.cutButton.setOnClickListener {
            // cut the graphic along the polyline to create 2 graphic parts
            polygonGraphic.geometry?.let { graphicGeometry ->
                val parts: List<Geometry> =
                    GeometryEngine.cut(
                        graphicGeometry,
                        polylineGraphic.geometry as Polyline
                    )

                // create graphics for the US and Canada sides
                val canadaSide = Graphic(
                    parts[0], SimpleFillSymbol(
                        SimpleFillSymbolStyle.BackwardDiagonal,
                        Color.GREEN, SimpleLineSymbol(SimpleLineSymbolStyle.Null, Color.BLUE, 0F)
                    )
                )
                val usSide = Graphic(
                    parts[1], SimpleFillSymbol(
                        SimpleFillSymbolStyle.ForwardDiagonal,
                        Color.YELLOW, SimpleLineSymbol(SimpleLineSymbolStyle.Null, Color.BLUE, 0F)
                    )
                )

                graphicsOverlay.graphics.addAll(listOf(canadaSide, usSide))
                activityMainBinding.cutButton.isEnabled = false
            }
        }
    }

    /**
     * Creates a polyline along the US/Canada border over Lake Superior.
     *
     * @return polyline
     */
    private fun createBorder(): Polyline {
        val borderPolylineBuilder = PolylineBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(-9981328.687124, 6111053.281447))
            addPoint(Point(-9946518.044066, 6102350.620682))
            addPoint(Point(-9872545.427566, 6152390.920079))
            addPoint(Point(-9838822.617103, 6157830.083057))
            addPoint(Point(-9446115.050097, 5927209.572793))
            addPoint(Point(-9430885.393759, 5876081.440801))
            addPoint(Point(-9415655.737420, 5860851.784463))
        }
        return borderPolylineBuilder.toGeometry() as Polyline
    }

    /**
     * Creates a polygon of points around Lake Superior.
     *
     * @return polygon
     */
    private fun createLakeSuperiorPolygon(): Polygon {
        val lakeSuperiorPolygonBuilder = PolygonBuilder(SpatialReference.webMercator()).apply {
            addPoint(Point(-10254374.668616, 5908345.076380))
            addPoint(Point(-10178382.525314, 5971402.386779))
            addPoint(Point(-10118558.923141, 6034459.697178))
            addPoint(Point(-9993252.729399, 6093474.872295))
            addPoint(Point(-9882498.222673, 6209888.368416))
            addPoint(Point(-9821057.766387, 6274562.532928))
            addPoint(Point(-9690092.583250, 6241417.023616))
            addPoint(Point(-9605207.742329, 6206654.660191))
            addPoint(Point(-9564786.389509, 6108834.986367))
            addPoint(Point(-9449989.747500, 6095091.726408))
            addPoint(Point(-9462116.153346, 6044160.821855))
            addPoint(Point(-9417652.665244, 5985145.646738))
            addPoint(Point(-9438671.768711, 5946341.148031))
            addPoint(Point(-9398250.415891, 5922088.336339))
            addPoint(Point(-9419269.519357, 5855797.317714))
            addPoint(Point(-9467775.142741, 5858222.598884))
            addPoint(Point(-9462924.580403, 5902686.086985))
            addPoint(Point(-9598740.325877, 5884092.264688))
            addPoint(Point(-9643203.813979, 5845287.765981))
            addPoint(Point(-9739406.633691, 5879241.702350))
            addPoint(Point(-9783061.694736, 5922896.763395))
            addPoint(Point(-9844502.151022, 5936640.023354))
            addPoint(Point(-9773360.570059, 6019099.583107))
            addPoint(Point(-9883306.649729, 5968977.105610))
            addPoint(Point(-9957681.938918, 5912387.211662))
            addPoint(Point(-10055501.612742, 5871965.858842))
            addPoint(Point(-10116942.069028, 5884092.264688))
            addPoint(Point(-10111283.079633, 5933406.315128))
            addPoint(Point(-10214761.742852, 5888134.399970))
            addPoint(Point(-10254374.668616, 5901877.659929))
        }
        return lakeSuperiorPolygonBuilder.toGeometry() as Polygon
    }
}
