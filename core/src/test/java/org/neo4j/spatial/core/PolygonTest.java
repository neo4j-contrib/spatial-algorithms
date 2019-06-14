package org.neo4j.spatial.core;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolygonTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldSee2EqualPolygonsWithUnequalStart() {
        Polygon.SimplePolygon[] variations = new Polygon.SimplePolygon[4];
        for (int i = 0; i < 4; i++) {
            variations[i] = makeSquareWithStart(i);
        }

        for (int i = 0; i < 3; i++) {
            assertThat("two squares should be treated as equal", variations[i], equalTo(variations[i+1]));
        }
    }

    @Test
    public void shouldTraversePolygon() {
        int n = 10;
        double[][] coordinates = getPoints(n);
        Point[] points = new Point[n];
        for (int i = 0; i < n; i++) {
            points[i] = Point.point(CRS.Cartesian, coordinates[i]);
        }

        Polygon.SimplePolygon simplePolygon = Polygon.simple(points);
        int idx;

        Point[] polygonPoints = simplePolygon.getPoints();
        for (int i = 0; i < polygonPoints.length; i++) {
            Assert.assertThat(polygonPoints[i], equalTo(points[i % n]));
            Assert.assertThat(polygonPoints[i], equalTo(points[i % n]));
        }
        Assert.assertThat(polygonPoints.length, equalTo(n+1)); //n+1 iterations

        simplePolygon.startTraversal(Point.point(CRS.Cartesian, 5, 0), Point.point(CRS.Cartesian, 5, 2));
        idx = 0;
        while (!simplePolygon.fullyTraversed()) {
            Point point = simplePolygon.getNextPoint();
            Assert.assertThat(point, equalTo(points[idx % n]));
            Assert.assertThat(point, equalTo(points[idx % n]));
            idx++;
        }
        Assert.assertThat(idx, equalTo(n+1)); //n+1 iterations


        simplePolygon.startTraversal(Point.point(CRS.Cartesian, 5, 0), Point.point(CRS.Cartesian, 0, 0));
        idx = n;
        while (!simplePolygon.fullyTraversed()) {
            Point point = simplePolygon.getNextPoint();
            Assert.assertThat(point, equalTo(points[idx % n]));
            Assert.assertThat(point, equalTo(points[idx % n]));
            idx--;
        }
        Assert.assertThat(idx, equalTo(-1)); //n+1 iterations


        simplePolygon.startTraversal(Point.point(CRS.Cartesian, 0, 8), Point.point(CRS.Cartesian, 5, 8));
        idx = 5;
        while (!simplePolygon.fullyTraversed()) {
            Point point = simplePolygon.getNextPoint();
            Assert.assertThat(point, equalTo(points[idx % n]));
            Assert.assertThat(point, equalTo(points[idx % n]));
            idx = ((idx-1) % (n) + (n)) % (n);
        }
        Assert.assertThat(idx, equalTo(4)); //n+1 iterations
    }

    private static Polygon.SimplePolygon makeSquareWithStart(int offset) {
        Point[] points = new Point[4];
        Point[] base = new Point[]{
                Point.point(CRS.Cartesian, -10,-10),
                Point.point(CRS.Cartesian, 10,-10),
                Point.point(CRS.Cartesian, 10,10),
                Point.point(CRS.Cartesian, -10,10)
        };

        for (int i = 0; i < points.length; i++) {
            points[i] = base[(i + offset) % base.length];
        }

        return Polygon.simple(points);
    }

    private double[][] getPoints(int n) {
        double[][] points = new double[n][2];

        for (int i = 0; i < n; i++) {
            int half = n / 2;
            if (i < half) {
                points[i] = new double[]{5, i*2};
            } else {
                points[i] = new double[]{0, n - (i+1-half)*2};
            }
        }
        return points;
    }
}
