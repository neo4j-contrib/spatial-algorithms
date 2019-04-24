package org.neo4j.spatial.core;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolygonTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldAcceptValid2DCoordinates() {
        Point one = new Point(0, 0);
        Point two = new Point(1, 0);
        Point three = new Point(0, 1);
        Polygon polygon = Polygon.simple(one, two, three);
        assertThat(polygon.isSimple(), is(true));
        Polygon.SimplePolygon[] shells = polygon.getShells();
        Polygon.SimplePolygon[] holes = polygon.getHoles();
        assertThat(shells.length, equalTo(1));
        assertThat(holes.length, equalTo(0));
        assertThat(shells[0], equalTo(polygon));
        assertThat(polygon.dimension(), equalTo(one.dimension()));
        Point[] points = polygon.getPoints();
        assertThat(points.length, equalTo(4));
        assertThat(points[0], equalTo(points[points.length - 1]));
    }

    @Test
    public void shouldAcceptValid3DCoordinates() {
        Point one = new Point(0, 0, 0);
        Point two = new Point(1, 0, 0);
        Point three = new Point(0, 1, 0);
        Polygon polygon = Polygon.simple(one, two, three);
        assertThat(polygon.isSimple(), is(true));
        Polygon.SimplePolygon[] shells = polygon.getShells();
        Polygon.SimplePolygon[] holes = polygon.getHoles();
        assertThat(shells.length, equalTo(1));
        assertThat(holes.length, equalTo(0));
        assertThat(shells[0], equalTo(polygon));
        assertThat(polygon.dimension(), equalTo(one.dimension()));
        Point[] points = polygon.getPoints();
        assertThat(points.length, equalTo(4));
        assertThat(points[0], equalTo(points[points.length - 1]));
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

    @Test
    public void shouldMakeCompoundPolygonSquareWithHole() {
        Polygon.SimplePolygon shell = makeSquare(new double[]{0, 0}, 10);
        Polygon.SimplePolygon hole = makeSquare(new double[]{4, 4}, 2);
        Polygon.MultiPolygon polygon = shell.withHole(hole);
        assertThat(polygon.isSimple(), is(false));
        Polygon.SimplePolygon[] shells = polygon.getShells();
        Polygon.SimplePolygon[] holes = polygon.getHoles();
        assertThat(shells.length, equalTo(1));
        assertThat(holes.length, equalTo(1));
        assertThat(shells[0], equalTo(shell));
        assertThat(holes[0], equalTo(hole));
        assertThat(polygon.dimension(), equalTo(shell.points[0].dimension()));
        Point[] points = polygon.getPoints();
        assertThat(points.length, equalTo(10));
        assertThat(points[0], equalTo(points[4]));
        assertThat(points[5], equalTo(points[9]));
        Point[] shellPoints = shell.getPoints();
        assertThat(shellPoints.length, equalTo(5));
        assertThat(shellPoints[0], equalTo(shellPoints[4]));
        assertThat(shellPoints[0], equalTo(new Point(0, 0)));
        Point[] holePoints = hole.getPoints();
        assertThat(holePoints.length, equalTo(5));
        assertThat(holePoints[0], equalTo(holePoints[4]));
        assertThat(holePoints[0], equalTo(new Point(4, 4)));
    }
}
