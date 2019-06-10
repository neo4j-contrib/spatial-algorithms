package org.neo4j.spatial.algo.wgs84;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class WithinTest {

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();


    @Test
    public void withinSimplePolygon() {
        Polygon.SimplePolygon polygon = Polygon.simple(
                Point.point(CRS.WGS84, 48.29432583, 65.76737339),
                Point.point(CRS.WGS84, 123.27303145, 71.47857599),
                Point.point(CRS.WGS84, 65.44555988, 80.39838594),
                Point.point(CRS.WGS84, -6.52009111, 80.64643932),
                Point.point(CRS.WGS84, -50.09349654, 73.62995695),
                Point.point(CRS.WGS84, -0.1463217, 78.92423464),
                Point.point(CRS.WGS84, 30.10011133, 70.99400378),
                Point.point(CRS.WGS84, 41.1093494, 49.25033244),
                Point.point(CRS.WGS84, 14.22363116, 57.6362965),
                Point.point(CRS.WGS84, -36.18709055, 49.17462881),
                Point.point(CRS.WGS84, -43.37206698, -10.40603857),
                Point.point(CRS.WGS84, 28.4776973, 24.5569462),
                Point.point(CRS.WGS84, 102.52930918, 56.9474956),
                Point.point(CRS.WGS84, 104.7311568, 48.17991419),
                Point.point(CRS.WGS84, 86.88460245, 23.71089841),
                Point.point(CRS.WGS84, 17.81611937, -10.97541232),
                Point.point(CRS.WGS84, 124.77955877, 14.07636845),
                Point.point(CRS.WGS84, 111.10492621, 64.25007063)
        );

        Point[] points = new Point[]{
                Point.point(CRS.WGS84, 83.35005759, 38.65685424),
                Point.point(CRS.WGS84, 30.9692617, 56.27791145),
                Point.point(CRS.WGS84, -8.08456178, 30.3630175),
                Point.point(CRS.WGS84, 59.01384711, 60.4081352),
                Point.point(CRS.WGS84, -102.87989594, 14.24491639),
                Point.point(CRS.WGS84, 111.51052972, 1.53036986),
                Point.point(CRS.WGS84, -23.38160837, 77.4177691) //Will be inside using cartesian method
        };

        boolean[] expected = new boolean[]{
                false,
                false,
                true,
                true,
                false,
                false,
                false
        };

        for (int i = 0; i < points.length; i++) {
            assertThat("Iteration " + i + " failed; " + points[i], Within.within(polygon, points[i]), equalTo(expected[i]));
        }
    }

    @Test
    public void simpleSquare() {
        Polygon.SimplePolygon polygon = Polygon.simple(
                Point.point(CRS.WGS84, 1, 0),
                Point.point(CRS.WGS84, 5, 2),
                Point.point(CRS.WGS84, 5, 6),
                Point.point(CRS.WGS84, 1, 4)
        );

        Point point = Point.point(CRS.WGS84, 3, 2);

        assertThat(Within.within(polygon, point), equalTo(true));
    }

    @Test
    public void dateTimeLine() {
        Polygon.SimplePolygon polygon = Polygon.simple(
                Point.point(CRS.WGS84, 175, -5),
                Point.point(CRS.WGS84, 185, -5),
                Point.point(CRS.WGS84, 185, 5),
                Point.point(CRS.WGS84, 175, 5)
        );

        Point a = Point.point(CRS.WGS84, 3, 2);
        Point b = Point.point(CRS.WGS84, 179, 2);

        assertThat(Within.within(polygon, a), equalTo(false));
        assertThat(Within.within(polygon, b), equalTo(true));
    }

    @Test
    public void northPoleException() {
        Polygon.SimplePolygon northPolygon = Polygon.simple(
                Point.point(CRS.WGS84, -135, 85),
                Point.point(CRS.WGS84, -45, 85),
                Point.point(CRS.WGS84, 45, 85),
                Point.point(CRS.WGS84, 135, 85)
        );

        Point a = Point.point(CRS.WGS84, 3, 88);

        exceptionGrabber.expect(IllegalArgumentException.class);
        exceptionGrabber.expectMessage("Polygon contains at least one pole");
        Within.within(northPolygon, a);
    }

    @Test
    public void southPoleException() {
        Polygon.SimplePolygon southPolygon = Polygon.simple(
                Point.point(CRS.WGS84, -135, -85),
                Point.point(CRS.WGS84, -45, -85),
                Point.point(CRS.WGS84, 45, -85),
                Point.point(CRS.WGS84, 135, -85)
        );

        Point a = Point.point(CRS.WGS84, 3, 88);

        exceptionGrabber.expect(IllegalArgumentException.class);
        exceptionGrabber.expectMessage("Polygon contains at least one pole");
        Within.within(southPolygon, a);
    }
}