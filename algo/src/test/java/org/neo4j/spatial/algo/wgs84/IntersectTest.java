package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.algo.wgs84.intersect.Intersect;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.*;

public class IntersectTest {

    @Test
    public void intersect() {
        Point p = Point.point(-50, 0);
        Point q = Point.point(-30, 0);
        Point r = Point.point(-40, -5);
        Point s = Point.point(-40, 3);

        LineSegment a = LineSegment.lineSegment(p, q);
        LineSegment b = LineSegment.lineSegment(r, s);

        Point actual = Intersect.intersect(a, b);
        Point expected = Point.point(-40, 0);
        assertPoint(actual, expected);
    }

    private static void assertPoint(Point actual, Point expected) {
        double[] a = actual.getCoordinate();
        double[] b = expected.getCoordinate();
        for (int i = 0; i < a.length; i++) {
            assertThat(a[i], closeTo(b[i], 0.1));
        }
    }
}