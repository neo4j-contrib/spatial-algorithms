package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class WGS84DistanceTest {

    private static final double circumference = 40000000;
    private static final double oneDegreeDistance = circumference / 360.0;
    private static final double oneByOneDiagonal = Math.sqrt(2 * oneDegreeDistance * oneDegreeDistance);
    private static final Distance calculator = DistanceCalculator.getCalculator(CRS.WGS84);

    @Test
    public void shouldCalculateDistanceInMetersBetweenTwoPointsOnEquator() {
        Point p = Point.point(CRS.WGS84, 5, 0);
        Point q = Point.point(CRS.WGS84, 6, 0);

        // error of several meters due to approximation of circumference and WGS84 ellipsoid
        assertThat(calculator.distance(p, q), closeTo(oneDegreeDistance, 100.0));
    }

    @Test
    public void shouldCalculateDistanceInMetersBetweenTwoPointsFarFromEquator() {
        Point p = Point.point(CRS.WGS84, 0.119, 52.205);
        Point q = Point.point(CRS.WGS84, 2.351, 48.857);
        assertThat(calculator.distance(p, q), closeTo(404279.16, 0.1));
    }

    @Test
    public void shouldCalculateDistanceBetweenPointAtSameLatitudeAsLineSegmentEndPoints() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.WGS84, 1.0, 51.0), Point.point(CRS.WGS84, 2.0, 51.0));

        Point p = Point.point(CRS.WGS84, 1.9, 51.0);
        assertThat("Distance should not be zero due to line following great circle", calculator.distance(l, p), closeTo(42.71, 0.1));

        p = Point.point(CRS.WGS84, 1.0, 51.0);
        assertThat("Distance should be zero for point at end point of line segment", calculator.distance(l, p), closeTo(0.0, 0.1));
    }

    @Test
    public void shouldCalculateDistanceBetweenPointOnEquatorAndEquatorLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 0.0), Point.point(CRS.WGS84, 1.0, 0.0));

        Point p = Point.point(CRS.WGS84, 0.0, 0.0);
        assertThat("Distance should be zero on the equator", calculator.distance(l, p), closeTo(0.0, 0.1));

        p = Point.point(CRS.WGS84, 0.0, 1.0);
        assertThat("Distance should be circumference/360 for one degreee above equator", calculator.distance(l, p), closeTo(oneDegreeDistance, 100.0));

        p = Point.point(CRS.WGS84, 0.0, -1.0);
        assertThat("Distance should be circumference/360 for one degreee below equator", calculator.distance(l, p), closeTo(oneDegreeDistance, 100.0));

        p = Point.point(CRS.WGS84, 1.0, 1.0);
        assertThat("Distance should be circumference/360 for one degreee above outer point", calculator.distance(l, p), closeTo(oneDegreeDistance, 100.0));

        p = Point.point(CRS.WGS84, 2.0, 1.0);
        assertThat("Distance should be much greater than circumference/360 for one degreee above and to the right of outer point", calculator.distance(l, p), closeTo(oneByOneDiagonal, 115.0));
    }

    @Test
    public void shouldCalculateDistanceOfZeroForIntersectingLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 51.0), Point.point(CRS.WGS84, 2.0, 51.0));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.WGS84, 0.5, 50.0), Point.point(CRS.WGS84, 0.5, 52.0));

        assertThat("Distance should be zero for intersecting line segments", calculator.distance(a, b), equalTo(0.0));
    }

    @Test
    public void shouldCalculateDistanceBetweenTwoParallelLineSegementsOneDegreeApart() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 51.0), Point.point(CRS.WGS84, 1.0, 51.0));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 50.0), Point.point(CRS.WGS84, 1.0, 50.0));

        assertThat("Distance should be circumference/360 for one degreee above parallel line segment", calculator.distance(a, b), closeTo(oneDegreeDistance, 100.0));
    }

    @Test
    public void shouldCalculateDistanceBetweenEquatorAndPolarLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 0.0), Point.point(CRS.WGS84, 1.0, 0.0));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 90.0), Point.point(CRS.WGS84, 1.0, 90.0));

        assertThat("Distance should be circumference/4 for distance between equatorial and polar line segments", calculator.distance(a, b), closeTo(circumference / 4, circumference / 5000));
    }

    @Test
    public void distancesShouldBeGreaterWhenLineSegmentsAreLongerWhenFarFromEquator() {
        LineSegment longLineAt51 = LineSegment.lineSegment(Point.point(CRS.WGS84, -10.0, 51.0), Point.point(CRS.WGS84, 10.0, 51.0));
        LineSegment longLineAt50 = LineSegment.lineSegment(Point.point(CRS.WGS84, -10.0, 50.0), Point.point(CRS.WGS84, 10.0, 50.0));
        LineSegment shortLineAt51 = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 51.0), Point.point(CRS.WGS84, 1.0, 51.0));
        LineSegment shortLineAt50 = LineSegment.lineSegment(Point.point(CRS.WGS84, -1.0, 50.0), Point.point(CRS.WGS84, 1.0, 50.0));
        double shortLineDistance = calculator.distance(shortLineAt51, shortLineAt50);
        double longLineDistance = calculator.distance(longLineAt51, longLineAt50);
        double longAndShortLineDistance = calculator.distance(longLineAt51, shortLineAt50);

        assertThat("Short-line distance should be circumference/360 for one degree above parallel line segment", shortLineDistance, closeTo(oneDegreeDistance, 100.0));
        assertThat("Long-line distance should be circumference/360 for one degree above parallel line segment", longLineDistance, closeTo(oneDegreeDistance, 1000.0));
        assertThat("Mixed short and long-line distance should be much larger than short-line distances for one degreee above parallel line segment", longAndShortLineDistance, greaterThan(shortLineDistance * 1.4));
        assertThat("Mixed short and long-line distance should be larger than circumference/360 for one degree above parallel line segment", longAndShortLineDistance, closeTo(oneDegreeDistance * 1.4, 3000.0));
    }

    @Test
    public void distanceAndEndpointSimplePolygons() {
        Polygon a = Polygon.simple(
                Point.point(CRS.WGS84, 1, 1),
                Point.point(CRS.WGS84, 2, 1),
                Point.point(CRS.WGS84, 2, 2),
                Point.point(CRS.WGS84, 1, 2));

        Polygon b = Polygon.simple(
                Point.point(CRS.WGS84, -1, -1),
                Point.point(CRS.WGS84, -2, -1),
                Point.point(CRS.WGS84, -2, -2),
                Point.point(CRS.WGS84, -1, -2));

        Map<String, Object> result = calculator.distanceAndEndpoints(a, b).asMap();

        assertThat((Double) result.get("distance"), closeTo(2 * oneByOneDiagonal, 250));
        assertThat(Arrays.asList(result.get("start"), result.get("end")),
                containsInAnyOrder(Point.point(CRS.WGS84, 1, 1), Point.point(CRS.WGS84, -1, -1)));
    }

    @Test
    public void distanceAndEndpointInterpolatedPoints() {
        Polygon a = Polygon.simple(
                Point.point(CRS.WGS84, -1, 1),
                Point.point(CRS.WGS84, 1, 1),
                Point.point(CRS.WGS84, 1, 2),
                Point.point(CRS.WGS84, -1, 2));

        Polygon b = Polygon.simple(
                Point.point(CRS.WGS84, -1, -1),
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 1, -1),
                Point.point(CRS.WGS84, 0, -2)
                );

        Map<String, Object> result = calculator.distanceAndEndpoints(a, b).asMap();

        assertThat((Double)result.get("distance"), closeTo(oneDegreeDistance, 250));
        assertThat(((Point)result.get("start")).getCoordinate()[0], closeTo(0, 0.000001));
        assertThat(((Point)result.get("start")).getCoordinate()[1], closeTo(1, 0.001));
        assertThat(result.get("end"), equalTo(Point.point(CRS.WGS84, 0, 0)));
    }

}