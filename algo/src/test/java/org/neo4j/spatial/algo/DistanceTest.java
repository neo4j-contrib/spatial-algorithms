package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.Distance;
import org.neo4j.spatial.core.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class DistanceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotWorkWithInvalidPoints() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot calculate distance between points of different dimension: 3 != 2");
        Distance.distance(Point.point(1, 2, 3), Point.point(3, 4));
    }

    @Test
    public void shouldWorkWithValid2DPoints() {
        assertThat(Distance.distance(Point.point(0, 0), Point.point(0, 0)), equalTo(0.0));
        assertThat(Distance.distance(Point.point(0, 0), Point.point(1, 0)), equalTo(1.0));
        assertThat(Distance.distance(Point.point(0, 0), Point.point(0, 1)), equalTo(1.0));
        assertThat(Distance.distance(Point.point(1, 0), Point.point(0, 0)), equalTo(1.0));
        assertThat(Distance.distance(Point.point(0, 1), Point.point(0, 0)), equalTo(1.0));
        assertThat(Distance.distance(Point.point(0, 0), Point.point(1, 1)), closeTo(1.414, 0.001));
        assertThat(Distance.distance(Point.point(-1, -1), Point.point(1, 1)), closeTo(2.828, 0.001));
    }

    @Test
    public void shouldWorkWithLineSegmentAndPoint() {
        LineSegment a = LineSegment.lineSegment(Point.point(0, 0), Point.point(10, 10));

        assertThat(Distance.distance(a, Point.point(0, -5)), equalTo(5.0));
        assertThat(Distance.distance(a, Point.point(10, 20)), equalTo(10.0));
        assertThat(Distance.distance(a, Point.point(5, 5)), equalTo(0.0));
        assertThat(Distance.distance(a, Point.point(10, 0)), closeTo(7.07106781186547, 0.0001));
    }

    @Test
    public void shouldWorkWithTwoLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(0, 0), Point.point(10, 10));

        Point[][] lineSegments = new Point[][]{
                {Point.point(0, 10), Point.point(10, 0)},
                {Point.point(-10, 0), Point.point(10, 20)},
                {Point.point(20, 0), Point.point(20, 20)}
        };

        double[] expected = new double[]{
                0,
                7.07106781186547,
                10.0
        };

        for (int i = 0; i < lineSegments.length; i++) {
            LineSegment b = LineSegment.lineSegment(lineSegments[i][0], lineSegments[i][1]);
            assertThat(Distance.distance(a, b), closeTo(expected[i], 0.0001));
        }
    }

    @Test
    public void shouldWorkWithSimplePolygonAndPoint() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10,-10),
                Point.point(10,-10),
                Point.point(1, 0),
                Point.point(10,10),
                Point.point(0,20),
                Point.point(-10,10),
                Point.point(-1, 0)
        );

        assertThat(Distance.distance(a, Point.point(-20, -10)), equalTo(10.0));
        assertThat(Distance.distance(a, Point.point(-0, -9)), equalTo(0.0));
    }

    @Test
    public void shouldWorkWithMultiPolygonAndPoint() {
        MultiPolygon a = new MultiPolygon();
        Point[][] polygonsA = new Point[][]{
                {
                        Point.point(-10, -10),
                        Point.point(10, -10),
                        Point.point(10, 10),
                        Point.point(-10, 10),
                        Point.point(-10, -10)
                },
                {
                        Point.point(-9, -9),
                        Point.point(9, -9),
                        Point.point(9, 9),
                        Point.point(-9, 9),
                        Point.point(-9, -9)
                },
                {
                        Point.point(-8, -8),
                        Point.point(8, -8),
                        Point.point(8, 8),
                        Point.point(-8, 8)
                },
                {
                        Point.point(-7, -7),
                        Point.point(7, -7),
                        Point.point(7, 7),
                        Point.point(-7, 7)
                }
        };

        for (Point[] points : polygonsA) {
            a.insertPolygon(Polygon.simple(points));
        }

        assertThat(Distance.distance(a, Point.point(0, 0)), equalTo(7.0));
        assertThat(Distance.distance(a, Point.point(7.5, 0)), equalTo(0.0));
        assertThat(Distance.distance(a, Point.point(8, 0)), equalTo(0.0));
        assertThat(Distance.distance(a, Point.point(8.5, 0)), equalTo(0.5));
    }
}
