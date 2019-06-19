package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReferenceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class WGS84LinearReferenceTest {

    @Test
    public void referenceLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.WGS84, 0.119, 52.205), Point.point(CRS.WGS84, 2.351, 48.857));

        assertPoint(LinearReferenceCalculator.reference(l, 0), Point.point(CRS.WGS84, 0.119, 52.205));
        assertPoint(LinearReferenceCalculator.reference(l, 101069.790997), Point.point(CRS.WGS84, 0.7072, 51.3723));
        assertThat(LinearReferenceCalculator.reference(l, 1000000000), is(nullValue()));
        assertThat(LinearReferenceCalculator.reference(l, -1), is(nullValue()));
    }

    private static void assertPoint(Point actual, Point expected) {
        double[] a = actual.getCoordinate();
        double[] b = expected.getCoordinate();
        for (int i = 0; i < a.length; i++) {
            assertThat(a[i], closeTo(b[i], 0.1));
        }
    }

    @Test
    public void referenceLineSegment2() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.WGS84, 0, 85), Point.point(CRS.WGS84, 180, 85));

        for (int k = 1; k < 1000; k++) {
            double d = DistanceCalculator.distance(l);
            Point p = LinearReferenceCalculator.reference(l, d/k);

            double[] expected = new double[] {
                    d/k,
                    (k-1)*d/k
            };

            for (int i = 0; i < 2; i++) {
                double actual = DistanceCalculator.distance(l.getPoints()[i], p);
                assertThat(actual, closeTo(expected[i], 1e-8));
            }
        }
    }
}