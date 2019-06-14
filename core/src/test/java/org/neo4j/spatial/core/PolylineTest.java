package org.neo4j.spatial.core;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolylineTest {
    @Test
    public void shouldTraversePolyline() {
        int n = 10;
        double[][] coordinates = getPoints(n);
        Point[] points = new Point[n];
        for (int i = 0; i < n; i++) {
            points[i] = Point.point(CRS.Cartesian, coordinates[i]);
        }

        Polyline polyline = Polyline.polyline(points);
        int idx;

        Point[] polygonPoints = polyline.getPoints();
        for (int i = 0; i < polygonPoints.length; i++) {
            assertThat(polygonPoints[i], equalTo(points[i]));
        }
        assertThat(polygonPoints.length, equalTo(n)); //n iterations

        polyline.startTraversal(Point.point(CRS.Cartesian, 5, 0), Point.point(CRS.Cartesian, 5, 2));
        idx = 0;
        while (!polyline.fullyTraversed()) {
            Point point = polyline.getNextPoint();
            assertThat(point, equalTo(points[idx]));
            idx++;
        }
        assertThat(idx, equalTo(n)); //n iterations


        polyline.startTraversal(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 0, 2));
        idx = n-1;
        while (!polyline.fullyTraversed()) {
            Point point = polyline.getNextPoint();
            assertThat(point, equalTo(points[idx]));
            idx--;
        }
        assertThat(idx, equalTo(-1)); //n iterations


        polyline.startTraversal(Point.point(CRS.Cartesian, 0, 8), Point.point(CRS.Cartesian, 5, 8));
        idx = 5;
        while (!polyline.fullyTraversed()) {
            Point point = polyline.getNextPoint();
            System.out.println(point);
            assertThat(point, equalTo(points[idx]));
            idx--;
        }
        assertThat(idx, equalTo(-1)); //6 iterations
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