package org.neo4j.spatial.algo.cartesian;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class CartesianAreaTest {

    @Test
    public void area() {
        Polygon.SimplePolygon simple = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );

        double actual = new CartesianArea().area(simple);
        double expected = 400;
        assertThat(actual, equalTo(expected));

        simple = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );

        actual = new CartesianArea().area(simple);
        expected = 200;
        assertThat(actual, equalTo(expected));
    }
}