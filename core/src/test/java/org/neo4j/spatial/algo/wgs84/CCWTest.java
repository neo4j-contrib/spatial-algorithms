package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class CCWTest {

    @Test
    public void isCCW() {
        Polygon.SimplePolygon simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );

        boolean actual = new CCW().isCCW(simple);
        boolean expected = true;
        assertThat(actual, equalTo(expected));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );

        actual = new CCW().isCCW(simple);
        expected = true;
        assertThat(actual, equalTo(expected));

        simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, -10, 10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, 10, -10)
        );

        actual = new CCW().isCCW(simple);
        expected = false;
        assertThat(actual, equalTo(expected));
    }
}