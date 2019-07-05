package org.neo4j.spatial.algo.wgs84;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class WGS84ConvexHullTest {
    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    @Test
    public void convexHullNorthAmerica() {
        Point[] points = new Point[]{
                Point.point(CRS.WGS84, -123.652078353, 34.4631422394),
                Point.point(CRS.WGS84, -112.901188304, 27.9167652027),
                Point.point(CRS.WGS84, -114.629929055, 43.0618607491),
                Point.point(CRS.WGS84, -84.461065423, 44.9862292279),
                Point.point(CRS.WGS84, -99.0171313976, 44.9915726287),
                Point.point(CRS.WGS84, -117.626249931, 45.8299948881),
                Point.point(CRS.WGS84, -120.307693412, 39.2852858127),
                Point.point(CRS.WGS84, -107.769173317, 43.367942457),
                Point.point(CRS.WGS84, -80.5816825012, 26.6648883829),
                Point.point(CRS.WGS84, -122.527247086, 33.3960719124),
                Point.point(CRS.WGS84, -75.084431266, 37.2887649757),
                Point.point(CRS.WGS84, -111.708077695, 26.5599058992),
                Point.point(CRS.WGS84, -76.3986174534, 49.9343511415),
                Point.point(CRS.WGS84, -71.3341319746, 36.1057501556),
                Point.point(CRS.WGS84, -83.6817951187, 26.5893202877),
                Point.point(CRS.WGS84, -101.88432285, 31.4515658613),
                Point.point(CRS.WGS84, -99.0573499948, 45.9281207342),
                Point.point(CRS.WGS84, -107.556880072, 48.7634740288),
                Point.point(CRS.WGS84, -119.43718492, 31.0437284945),
                Point.point(CRS.WGS84, -121.063240876, 25.1173777056),
                Point.point(CRS.WGS84, -95.4020847524, 36.8136753742),
                Point.point(CRS.WGS84, -77.3589386311, 36.2605555289),
                Point.point(CRS.WGS84, -99.434763213, 26.5552012843),
                Point.point(CRS.WGS84, -111.770995625, 32.9720318737),
                Point.point(CRS.WGS84, -79.6740392382, 30.4084884516),
                Point.point(CRS.WGS84, -85.865762392, 39.3536985271),
                Point.point(CRS.WGS84, -70.772446775, 25.9975103658),
                Point.point(CRS.WGS84, -115.21294474, 41.4863870399),
                Point.point(CRS.WGS84, -108.655230766, 33.4222854046),
                Point.point(CRS.WGS84, -95.8554238114, 43.3752266628),
                Point.point(CRS.WGS84, -112.344901059, 46.9460023634),
                Point.point(CRS.WGS84, -115.064239079, 41.9123285944),
                Point.point(CRS.WGS84, -95.4413272766, 34.9985504412),
                Point.point(CRS.WGS84, -93.2909540449, 34.9689601243),
                Point.point(CRS.WGS84, -116.40457363, 45.2156017168),
                Point.point(CRS.WGS84, -92.2141627228, 38.5431901178),
                Point.point(CRS.WGS84, -84.8215842041, 37.6656871658),
                Point.point(CRS.WGS84, -121.58483849, 35.051150534),
                Point.point(CRS.WGS84, -88.7138256406, 29.3544113634),
                Point.point(CRS.WGS84, -76.6345531998, 44.3546463654),
                Point.point(CRS.WGS84, -81.65644028, 29.3533735275),
                Point.point(CRS.WGS84, -123.298862403, 27.2608119635),
                Point.point(CRS.WGS84, -77.0560655896, 40.6135242481),
                Point.point(CRS.WGS84, -67.5262210399, 33.7560823775),
                Point.point(CRS.WGS84, -90.6268459049, 43.3147644633),
                Point.point(CRS.WGS84, -78.1609616988, 37.0324301284),
                Point.point(CRS.WGS84, -118.423935532, 31.7786144265),
                Point.point(CRS.WGS84, -110.512589495, 49.1709738758),
                Point.point(CRS.WGS84, -74.9593084184, 33.9626282677),
                Point.point(CRS.WGS84, -75.7974215341, 47.3671840722),
                Point.point(CRS.WGS84, -86.0041119894, 27.4399908524),
                Point.point(CRS.WGS84, -71.0995240607, 41.7469904595),
                Point.point(CRS.WGS84, -68.8705400381, 40.5741623178),
                Point.point(CRS.WGS84, -106.450639954, 31.3379214336),
                Point.point(CRS.WGS84, -91.5608885987, 44.8782696626),
                Point.point(CRS.WGS84, -90.9777229487, 36.7635787431),
                Point.point(CRS.WGS84, -117.421495778, 45.3606922179),
                Point.point(CRS.WGS84, -118.921943549, 46.0705705857),
                Point.point(CRS.WGS84, -122.410969355, 25.1293344933),
                Point.point(CRS.WGS84, -69.7414529558, 28.4017622369),
                Point.point(CRS.WGS84, -92.3809552668, 35.0279966796),
                Point.point(CRS.WGS84, -78.8933431124, 36.2043732493),
                Point.point(CRS.WGS84, -118.398791431, 33.6424240382),
                Point.point(CRS.WGS84, -107.951524619, 45.7445266651),
                Point.point(CRS.WGS84, -111.94044011, 27.3951891114),
                Point.point(CRS.WGS84, -74.4458700616, 41.705418532),
                Point.point(CRS.WGS84, -109.420752257, 45.4131458557),
                Point.point(CRS.WGS84, -87.0149064808, 30.8063635673),
                Point.point(CRS.WGS84, -77.187910258, 45.8441882563),
                Point.point(CRS.WGS84, -108.608472006, 48.5262735327),
                Point.point(CRS.WGS84, -91.3515736464, 42.8683852067),
                Point.point(CRS.WGS84, -111.698239164, 39.8576739449),
                Point.point(CRS.WGS84, -67.408567877, 43.0685431768),
                Point.point(CRS.WGS84, -100.548671158, 43.926373779),
                Point.point(CRS.WGS84, -98.0517397635, 29.3494620888),
                Point.point(CRS.WGS84, -74.2373498763, 49.0225865346),
                Point.point(CRS.WGS84, -95.735085758, 35.1876842461),
                Point.point(CRS.WGS84, -100.632042887, 39.4907340449),
                Point.point(CRS.WGS84, -73.8050993807, 46.8027114658),
                Point.point(CRS.WGS84, -91.3035842365, 42.1149121926),
                Point.point(CRS.WGS84, -92.3035842365, 42.1149121926),
                Point.point(CRS.WGS84, -116.879247003, 30.1789613908),
                Point.point(CRS.WGS84, -82.1702840671, 25.3838618864),
                Point.point(CRS.WGS84, -122.899633545, 27.73655328),
                Point.point(CRS.WGS84, -114.15172545, 49.3586581746),
                Point.point(CRS.WGS84, -85.2826053186, 43.3570076448),
                Point.point(CRS.WGS84, -74.7976392574, 29.5184990555),
                Point.point(CRS.WGS84, -67.5676728578, 30.9284725957),
                Point.point(CRS.WGS84, -84.9299391909, 27.3753234572),
                Point.point(CRS.WGS84, -105.757598619, 25.3084850871),
                Point.point(CRS.WGS84, -69.5002757708, 43.8795475154),
                Point.point(CRS.WGS84, -79.6866842812, 42.3207594209),
                Point.point(CRS.WGS84, -102.602883489, 38.8648020159),
                Point.point(CRS.WGS84, -108.793544137, 29.9246750544),
                Point.point(CRS.WGS84, -118.118544898, 33.1081827877),
                Point.point(CRS.WGS84, -66.5373330415, 29.448750712),
                Point.point(CRS.WGS84, -81.1560549999, 32.9792540973),
                Point.point(CRS.WGS84, -123.947228907, 49.5131863856),
                Point.point(CRS.WGS84, -101.34841715, 43.2203030915),
                Point.point(CRS.WGS84, -79.9459902761, 32.0947487733),
                Point.point(CRS.WGS84, -96.4899452844, 44.3662499843),
        };
        Polygon.SimplePolygon expected = Polygon.simple(
                Point.point(CRS.WGS84, -67.408567877,43.0685431768),
                Point.point(CRS.WGS84, -74.2373498763,49.0225865346),
                Point.point(CRS.WGS84, -76.3986174534,49.9343511415),
                Point.point(CRS.WGS84, -123.947228907,49.5131863856),
                Point.point(CRS.WGS84, -123.652078353,34.4631422394),
                Point.point(CRS.WGS84, -123.298862403,27.2608119635),
                Point.point(CRS.WGS84, -122.410969355,25.1293344933),
                Point.point(CRS.WGS84, -121.063240876,25.1173777056),
                Point.point(CRS.WGS84, -105.757598619,25.3084850871),
                Point.point(CRS.WGS84, -82.1702840671,25.3838618864),
                Point.point(CRS.WGS84, -70.772446775,25.9975103658),
                Point.point(CRS.WGS84, -66.5373330415,29.448750712),
                Point.point(CRS.WGS84, -67.408567877,43.0685431768)
        );

        Polygon.SimplePolygon actual = WGS84ConvexHull.convexHull(points);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void convexHullNorthPole() {
        Point[] points = new Point[]{
                Point.point(CRS.WGS84, 0, 85),
                Point.point(CRS.WGS84, 90, 85),
                Point.point(CRS.WGS84, 90, 90),
                Point.point(CRS.WGS84, 0, 87),
                Point.point(CRS.WGS84, -90, 88),
                Point.point(CRS.WGS84, 180, 85),
                Point.point(CRS.WGS84, -90, 85),
        };
        Polygon.SimplePolygon expected = Polygon.simple(
                Point.point(CRS.WGS84, 0, 85),
                Point.point(CRS.WGS84, 90, 85),
                Point.point(CRS.WGS84, 180, 85),
                Point.point(CRS.WGS84, -90, 85)
        );

        Polygon.SimplePolygon actual = WGS84ConvexHull.convexHull(points);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void noConvexHullBothPoles() {
        Point[] points = new Point[]{
                Point.point(CRS.WGS84, 0, 85),
                Point.point(CRS.WGS84, 90, 85),
                Point.point(CRS.WGS84, 90, 90),
                Point.point(CRS.WGS84, 0, 87),
                Point.point(CRS.WGS84, -90, 88),
                Point.point(CRS.WGS84, 180, 85),
                Point.point(CRS.WGS84, -90, 85),
                Point.point(CRS.WGS84, 0, -85),
                Point.point(CRS.WGS84, 90, -85),
                Point.point(CRS.WGS84, 90, -90),
                Point.point(CRS.WGS84, 0, -87),
                Point.point(CRS.WGS84, -90, -88),
                Point.point(CRS.WGS84, 180, -85),
                Point.point(CRS.WGS84, -90, -85),
        };

        exceptionGrabber.expect(IllegalArgumentException.class);
        exceptionGrabber.expectMessage("Points do not lie all on the same hemisphere");
        WGS84ConvexHull.convexHull(points);
    }
}