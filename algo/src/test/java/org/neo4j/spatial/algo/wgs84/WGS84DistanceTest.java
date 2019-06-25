package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

public class WGS84DistanceTest {

    @Test
    public void distancePointPoint() {
        Point p = Point.point(CRS.WGS84, 0.119, 52.205);
        Point q = Point.point(CRS.WGS84, 2.351, 48.857);

        Distance calculator = DistanceCalculator.getCalculator(CRS.WGS84);
        assertThat(calculator.distance(p, q), closeTo(404279.16, 0.1));
    }

    @Test
    public void distancePointLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.WGS84, 1.0, 51.0), Point.point(CRS.WGS84, 2.0, 51.0));

        Distance calculator = DistanceCalculator.getCalculator(CRS.WGS84);
        Point p = Point.point(CRS.WGS84, 1.9, 51.0);
        assertThat(calculator.distance(l, p), closeTo(42.71, 0.1));

        p = Point.point(CRS.WGS84, 1.0, 51.0);
        assertThat(calculator.distance(l, p), closeTo(0.0, 0.1));
    }

    @Test
    public void distanceLineSegmentLineSegment() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 51.0), Point.point(CRS.WGS84, 2.0, 51.0));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.WGS84, 0.5, 50.0), Point.point(CRS.WGS84, 0.5, 52.0));

        Distance calculator = DistanceCalculator.getCalculator(CRS.WGS84);
        assertThat(calculator.distance(a, b), equalTo(0.0));
    }
}