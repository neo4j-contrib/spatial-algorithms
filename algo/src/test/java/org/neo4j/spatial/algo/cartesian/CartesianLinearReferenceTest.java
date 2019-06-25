package org.neo4j.spatial.algo.cartesian;

import org.junit.Test;
import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.algo.LinearReferenceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CartesianLinearReferenceTest {

    @Test
    public void referenceLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 10, 10));

        LinearReference calculator = LinearReferenceCalculator.getCalculator(CRS.Cartesian);

        assertThat(calculator.reference(l, 0), equalTo(Point.point(CRS.Cartesian, 0, 0)));
        assertThat(calculator.reference(l, Math.sqrt(200)/2), equalTo(Point.point(CRS.Cartesian, 5, 5)));
        assertThat(calculator.reference(l, Math.sqrt(200)), equalTo(Point.point(CRS.Cartesian, 10, 10)));
        assertThat(calculator.reference(l, 100), is(nullValue()));
        assertThat(calculator.reference(l, -1), is(nullValue()));
    }

    @Test
    public void referencePolygon() {
        Polygon.SimplePolygon p = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );

        LinearReference calculator = LinearReferenceCalculator.getCalculator(CRS.Cartesian);

        //Forward
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 0), equalTo(Point.point(CRS.Cartesian, -10, -10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 10), equalTo(Point.point(CRS.Cartesian, 0, -10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 25), equalTo(Point.point(CRS.Cartesian, 10, -5)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 125), equalTo(Point.point(CRS.Cartesian, 5, 10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], -1), is(nullValue()));

        //Backward
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[3], 0), equalTo(Point.point(CRS.Cartesian, -10, -10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[3], 10), equalTo(Point.point(CRS.Cartesian, -10, 0)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[3], 25), equalTo(Point.point(CRS.Cartesian, -5, 10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[3], 125), equalTo(Point.point(CRS.Cartesian, 10, 5)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[3], -1), is(nullValue()));
    }

    @Test
    public void referencePolyline() {
        Polyline p = Polyline.polyline(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );

        LinearReference calculator = LinearReferenceCalculator.getCalculator(CRS.Cartesian);

        //Forward
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 0), equalTo(Point.point(CRS.Cartesian, -10, -10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 10), equalTo(Point.point(CRS.Cartesian, 0, -10)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 25), equalTo(Point.point(CRS.Cartesian, 10, -5)));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], 125), is(nullValue()));
        assertThat(calculator.reference(p, p.getPoints()[0], p.getPoints()[1], -1), is(nullValue()));

        //Backward
        assertThat(calculator.reference(p, p.getPoints()[3], p.getPoints()[2], 0), equalTo(Point.point(CRS.Cartesian, -10, 10)));
        assertThat(calculator.reference(p, p.getPoints()[3], p.getPoints()[2], 10), equalTo(Point.point(CRS.Cartesian, 0, 10)));
        assertThat(calculator.reference(p, p.getPoints()[3], p.getPoints()[2], 25), equalTo(Point.point(CRS.Cartesian, 10, 5)));
        assertThat(calculator.reference(p, p.getPoints()[3], p.getPoints()[2], 125), is(nullValue()));
        assertThat(calculator.reference(p, p.getPoints()[3], p.getPoints()[2], -1), is(nullValue()));
    }
}