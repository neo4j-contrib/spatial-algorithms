package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class WGS84AreaTest {

    @Test
    public void area() {
        Polygon.SimplePolygon simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 1, 0),
                Point.point(CRS.WGS84, 0, 1)
        );

        double actual = new WGS84Area().area(simple);
        double expected = 6.18e9;
        assertThat(actual, closeTo(expected, 0.01e9));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 1, 1),
                Point.point(CRS.WGS84, 1, 5),
                Point.point(CRS.WGS84, 3, 5),
                Point.point(CRS.WGS84, 3, 1),
                Point.point(CRS.WGS84, 2, 3)
        );

        actual = new WGS84Area().area(simple);
        expected = 74042699235.64375;
        assertThat(actual, closeTo(expected, 0.1e12));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 90, 0),
                Point.point(CRS.WGS84, 179, 0),
                Point.point(CRS.WGS84, -179, 0),
                Point.point(CRS.WGS84, -90, 0)
        );

        actual = new WGS84Area().area(simple);
        expected = 2 * Math.PI * WGSUtil.RADIUS * WGSUtil.RADIUS;
        assertThat(actual, closeTo(expected, 0.1e12));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 0, 85),
                Point.point(CRS.WGS84, 180, 85),
                Point.point(CRS.WGS84, 180, 0),
                Point.point(CRS.WGS84, 180, -85),
                Point.point(CRS.WGS84, 0, -85)
        );

        actual = new WGS84Area().area(simple);
        expected = 2 * Math.PI * WGSUtil.RADIUS * WGSUtil.RADIUS;
        assertThat(actual, closeTo(expected, 0.1e12));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 90, 0),
                Point.point(CRS.WGS84, 180, 0),
                Point.point(CRS.WGS84, 180, 90)
        );

        actual = new WGS84Area().area(simple);
        expected = Math.PI * WGSUtil.RADIUS * WGSUtil.RADIUS;
        assertThat(actual, closeTo(expected, 0.1e12));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 90, 0),
                Point.point(CRS.WGS84, 90, 90)
        );

        actual = new WGS84Area().area(simple);
        expected = Math.PI/2 * WGSUtil.RADIUS * WGSUtil.RADIUS;
        assertThat(actual, closeTo(expected, 0.1e12));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 45, 0),
                Point.point(CRS.WGS84, 45, 90)
        );

        actual = new WGS84Area().area(simple);
        expected = Math.PI/4 * WGSUtil.RADIUS * WGSUtil.RADIUS;
        assertThat(actual, closeTo(expected, 0.1e12));
    }
}