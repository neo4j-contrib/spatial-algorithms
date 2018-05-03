package org.amanzi.spatial.algo;

import org.amanzi.spatial.core.Point;
import org.amanzi.spatial.core.Polygon;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class WithinTest {

    @Test
    public void shouldBeWithinSquare() {
        Polygon.SimplePolygon square = makeSquare(new double[]{-10, -10}, 20);
        assertThat(Within.within(square, new Point(0, 0)), equalTo(true));
        assertThat(Within.within(square, new Point(-20, 0)), equalTo(false));
        assertThat(Within.within(square, new Point(20, 0)), equalTo(false));
        assertThat(Within.within(square, new Point(0, -20)), equalTo(false));
        assertThat(Within.within(square, new Point(0, 20)), equalTo(false));
    }

    @Ignore
    // TODO still some bugs with touching logic
    public void shouldBeTouchingSquare() {
        Polygon.SimplePolygon square = makeSquare(new double[]{-10, -10}, 20);
        for (boolean touching : new boolean[]{false, true}) {
            assertThat(Within.within(square, new Point(-10, -20), touching), equalTo(false));
            assertThat(Within.within(square, new Point(-10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(-10, 0), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(-10, 10), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(-10, 20), touching), equalTo(false));
            assertThat(Within.within(square, new Point(-20, -10), touching), equalTo(false));
            assertThat(Within.within(square, new Point(-10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(0, -10), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(10, -10), touching), equalTo(touching));
            assertThat(Within.within(square, new Point(20, -10), touching), equalTo(false));
        }
    }

    private static double[] move(double[] coords, int dim, double move) {
        double[] moved = Arrays.copyOf(coords, coords.length);
        moved[dim] += move;
        return moved;
    }

    private static Polygon.SimplePolygon makeSquare(double[] bottomLeftCoords, double width) {
        Point bottomLeft = new Point(bottomLeftCoords);
        Point bottomRight = new Point(move(bottomLeftCoords, 0, width));
        Point topRight = new Point(move(bottomRight.getCoordinate(), 1, width));
        Point topLeft = new Point(move(topRight.getCoordinate(), 0, -width));
        return Polygon.simple(bottomLeft, bottomRight, topRight, topLeft);
    }

}
