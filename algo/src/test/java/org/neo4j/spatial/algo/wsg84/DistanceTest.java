package org.neo4j.spatial.algo.wsg84;

import org.junit.Test;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

public class DistanceTest {

    @Test
    public void distancePointPoint() {
        Point p = Point.point(0.119, 52.205);
        Point q = Point.point(2.351, 48.857);

        assertThat(Distance.distance(p, q), closeTo(404279.16, 0.1));

        System.out.println(Distance.distance(p, q));
    }

    @Test
    public void distancePointLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(1.0, 51.0), Point.point(2.0, 51.0));

        Point p = Point.point(1.9, 51.0);
        assertThat(Distance.distance(l, p), closeTo(42.71, 0.1));

        p = Point.point(1.0, 51.0);
        assertThat(Distance.distance(l, p), closeTo(0.0, 0.1));
    }

    @Test
    public void distanceLineSegmentLineSegment() {
        LineSegment a = LineSegment.lineSegment(Point.point(1.0, 51.0), Point.point(2.0, 51.0));
        LineSegment b = LineSegment.lineSegment(Point.point(0.5, 50.0), Point.point(0.5, 52.0));

        assertThat(Distance.distance(a, b), equalTo(0.0));
    }
}