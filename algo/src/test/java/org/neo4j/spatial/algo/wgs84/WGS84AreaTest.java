package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class WGS84AreaTest {

    @Test
    public void area() {
        Polygon.SimplePolygon simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 0, 1),
                Point.point(CRS.WGS84, 1, 0)
        );

        double actual = new WGS84Area().area(simple);
        double expected = 6.18e9;
        assertThat(actual, closeTo(expected, 0.01e9));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, -10, 10),
                Point.point(CRS.WGS84, 10, 10)
        );

        actual = new WGS84Area().area(simple);
        expected = 2.4e12;
        assertThat(actual, closeTo(expected, 0.1e12));
    }
}