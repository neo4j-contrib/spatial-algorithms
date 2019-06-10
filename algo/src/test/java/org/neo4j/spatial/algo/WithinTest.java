package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.Within;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class WithinTest {

    @Test
    public void shouldBeWithinSquare() {
        Polygon.SimplePolygon square = makeSquare(new double[]{-10, -10}, 20);
        assertThat(Within.within(square, Point.point(CRS.Cartesian, 0, 0)), equalTo(true));
        assertThat(Within.within(square, Point.point(CRS.Cartesian, -20, 0)), equalTo(false));
        assertThat(Within.within(square, Point.point(CRS.Cartesian, 20, 0)), equalTo(false));
        assertThat(Within.within(square, Point.point(CRS.Cartesian, 0, -20)), equalTo(false));
        assertThat(Within.within(square, Point.point(CRS.Cartesian, 0, 20)), equalTo(false));
    }

    private static double[] move(double[] coords, int dim, double move) {
        double[] moved = Arrays.copyOf(coords, coords.length);
        moved[dim] += move;
        return moved;
    }

    private static Polygon.SimplePolygon makeSquare(double[] bottomLeftCoords, double width) {
        Point bottomLeft = Point.point(CRS.Cartesian, bottomLeftCoords);
        Point bottomRight = Point.point(CRS.Cartesian, move(bottomLeftCoords, 0, width));
        Point topRight = Point.point(CRS.Cartesian, move(bottomRight.getCoordinate(), 1, width));
        Point topLeft = Point.point(CRS.Cartesian, move(topRight.getCoordinate(), 0, -width));
        return Polygon.simple(bottomLeft, bottomRight, topRight, topLeft);
    }

}
