package org.neo4j.spatial.algo.cartesian;

import org.junit.Test;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class LinearReferenceTest {

    @Test
    public void referenceLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(0, 0), Point.point(10, 10));

        assertThat(LinearReference.reference(l, 0), equalTo(Point.point(0, 0)));
        assertThat(LinearReference.reference(l, Math.sqrt(200)/2), equalTo(Point.point(5, 5)));
        assertThat(LinearReference.reference(l, Math.sqrt(200)), equalTo(Point.point(10, 10)));
        assertThat(LinearReference.reference(l, 100), is(nullValue()));
        assertThat(LinearReference.reference(l, -1), is(nullValue()));
    }

    @Test
    public void referencePolygon() {
        Polygon.SimplePolygon p = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );

        assertThat(LinearReference.reference(p, 0), equalTo(Point.point(-10, -10)));
        assertThat(LinearReference.reference(p, 10), equalTo(Point.point(0, -10)));
        assertThat(LinearReference.reference(p, 25), equalTo(Point.point(10, -5)));
        assertThat(LinearReference.reference(p, 125), equalTo(Point.point(5, 10)));
        assertThat(LinearReference.reference(p, -1), is(nullValue()));
    }
}