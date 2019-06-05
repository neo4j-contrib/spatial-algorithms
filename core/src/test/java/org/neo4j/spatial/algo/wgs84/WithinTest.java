package org.neo4j.spatial.algo.wgs84;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
                Point.point(48.29432583, 65.76737339),
                Point.point(123.27303145, 71.47857599),
                Point.point(65.44555988, 80.39838594),
                Point.point(-6.52009111, 80.64643932),
                Point.point(-50.09349654, 73.62995695),
                Point.point(-0.1463217, 78.92423464),
                Point.point(30.10011133, 70.99400378),
                Point.point(41.1093494, 49.25033244),
                Point.point(14.22363116, 57.6362965),
                Point.point(-36.18709055, 49.17462881),
                Point.point(-43.37206698, -10.40603857),
                Point.point(28.4776973, 24.5569462),
                Point.point(102.52930918, 56.9474956),
                Point.point(104.7311568, 48.17991419),
                Point.point(86.88460245, 23.71089841),
                Point.point(17.81611937, -10.97541232),
                Point.point(124.77955877, 14.07636845),
                Point.point(111.10492621, 64.25007063)
        );

        Point[] points = new Point[]{
                Point.point(83.35005759, 38.65685424),
                Point.point(30.9692617, 56.27791145),
                Point.point(-8.08456178, 30.3630175),
                Point.point(59.01384711, 60.4081352),
                Point.point(-102.87989594, 14.24491639),
                Point.point(111.51052972, 1.53036986),
                Point.point(-23.38160837, 77.4177691)
        };

        boolean[] expected = new boolean[]{
                false,
                false,
                true,
                true,
                false,
                false,
                true
        };

        for (int i = 0; i < points.length; i++) {
            assertThat(Within.within(polygon, points[i]), equalTo(expected[i]));
        }
    }

    @Test
    public void simpleSquare() {
        Polygon.SimplePolygon polygon = Polygon.simple(
                Point.point(1, 0),
                Point.point(5, 2),
                Point.point(5, 6),
                Point.point(1, 4)
        );

        Point point = Point.point(3, 2);

        assertThat(Within.within(polygon, point), equalTo(true));
    }

    @Test
    public void dateTimeLine() {
        Polygon.SimplePolygon polygon = Polygon.simple(
                Point.point(175, -5),
                Point.point(185, -5),
                Point.point(185, 5),
                Point.point(175, 5)
        );

        Point a = Point.point(3, 2);
        Point b = Point.point(179, 2);

        assertThat(Within.within(polygon, a), equalTo(false));
        assertThat(Within.within(polygon, b), equalTo(true));
    }

    @Test
    public void northPoleException() {
        Polygon.SimplePolygon northPolygon = Polygon.simple(
                Point.point(-135, 85),
                Point.point(-45, 85),
                Point.point(45, 85),
                Point.point(135, 85)
        );

        Point a = Point.point(3, 88);

        exceptionGrabber.expect(IllegalArgumentException.class);
        exceptionGrabber.expectMessage("Polygon contains at least one pole");
        Within.within(northPolygon, a);
    }

    @Test
    public void southPoleException() {
        Polygon.SimplePolygon southPolygon = Polygon.simple(
                Point.point(-135, -85),
                Point.point(-45, -85),
                Point.point(45, -85),
                Point.point(135, -85)
        );

        Point a = Point.point(3, 88);

        exceptionGrabber.expect(IllegalArgumentException.class);
        exceptionGrabber.expectMessage("Polygon contains at least one pole");
        Within.within(southPolygon, a);
    }
}