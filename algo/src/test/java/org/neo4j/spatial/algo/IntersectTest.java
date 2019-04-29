package org.neo4j.spatial.algo;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.*;

public class IntersectTest {
    @Test
    public void shouldFindIntersectionBetweenLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(0, 0), Point.point(10,10));
        LineSegment b = LineSegment.lineSegment(Point.point(0, 10), Point.point(10,0));
        assertThat(Intersect.intersect(a, b), equalTo(Point.point(5,5)));

        a = LineSegment.lineSegment(Point.point(0, 0), Point.point(10,10));
        b = LineSegment.lineSegment(Point.point(0, 10), Point.point(10,10));
        assertThat(Intersect.intersect(a, b), equalTo(Point.point(10,10)));

        a = LineSegment.lineSegment(Point.point(0, 0), Point.point(0,10));
        b = LineSegment.lineSegment(Point.point(0, 5), Point.point(0,15));
        assertThat(Intersect.intersect(a, b), equalTo(Point.point(0,5)));

        a = LineSegment.lineSegment(Point.point(0, 0), Point.point(5,5));
        b = LineSegment.lineSegment(Point.point(-5, -5), Point.point(10,10));
        assertThat(Intersect.intersect(a, b), equalTo(Point.point(0,0)));
    }

    @Test
    public void shouldFindIntersectionBetweenLineSegmentsWithAccuracy() {
        LineSegment a = LineSegment.lineSegment(Point.point(0, 0), Point.point(1, 1));
        LineSegment b = LineSegment.lineSegment(Point.point(0, 1e-2), Point.point(1, 0.5));
        double[] actual = Intersect.intersect(a, b).getCoordinate();
        double[] test = Point.point(0.0196078, 0.0196078).getCoordinate();
        assertThat(actual[0], closeTo(test[0], 1e-6));
        assertThat(actual[1], closeTo(test[1], 1e-6));
    }

    @Test
    public void shouldNotFindIntersectionBetweenLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(0, 0), Point.point(10, 10));
        LineSegment b = LineSegment.lineSegment(Point.point(0, 10), Point.point(-10, 0));
        assertThat(Intersect.intersect(a, b), is(nullValue()));

        a = LineSegment.lineSegment(Point.point(0, 0), Point.point(1,1));
        b = LineSegment.lineSegment(Point.point(0,1e-10), Point.point(1,1+1e-10));
        assertThat(Intersect.intersect(a, b), is(nullValue()));
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(-15, -15),
                Point.point(15, -15),
                Point.point(15, 15)
        );

        assertThat(Intersect.intersect(a, b), org.hamcrest.Matchers.arrayContainingInAnyOrder(Point.point(-10,-10), Point.point(10,10)));

        a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        b = Polygon.simple(
                Point.point(-10, -15),
                Point.point(15, -15),
                Point.point(15, 15),
                Point.point(-10, 15)
        );

        assertThat(Intersect.intersect(a, b), org.hamcrest.Matchers.arrayContainingInAnyOrder(Point.point(-10,-10)));

        a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        b = Polygon.simple(
                Point.point(-15, -10),
                Point.point(15, -10),
                Point.point(15, 15),
                Point.point(-15, 15)
        );

        assertThat(Intersect.intersect(a, b), org.hamcrest.Matchers.arrayContainingInAnyOrder(Point.point(-10,-10)));
    }

    @Test
    public void shouldNotFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(-100, -150),
                Point.point(150, -150),
                Point.point(150, 150)
        );

        assertThat(Intersect.intersect(a, b), org.hamcrest.Matchers.emptyArray());
    }
}