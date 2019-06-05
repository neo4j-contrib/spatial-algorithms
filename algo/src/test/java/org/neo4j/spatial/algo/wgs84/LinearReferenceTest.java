package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class LinearReferenceTest {

    @Test
    public void referenceLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(0.119, 52.205), Point.point(2.351, 48.857));

        assertPoint(LinearReference.reference(l, 0), Point.point(0.119, 52.205));
        assertPoint(LinearReference.reference(l, 101069.790997), Point.point(0.7072, 51.3723));
        assertThat(LinearReference.reference(l, 1000000000), is(nullValue()));
        assertThat(LinearReference.reference(l, -1), is(nullValue()));
    }

    private static void assertPoint(Point actual, Point expected) {
        double[] a = actual.getCoordinate();
        double[] b = expected.getCoordinate();
        for (int i = 0; i < a.length; i++) {
            assertThat(a[i], closeTo(b[i], 0.1));
        }
    }
}