package org.neo4j.spatial.algo.cartesian;

import org.junit.Test;
import org.neo4j.spatial.algo.LinearReferenceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CartesianLinearReferenceTest {

    @Test
    public void referenceLineSegment() {
        LineSegment l = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 10, 10));

        assertThat(LinearReferenceCalculator.reference(l, 0), equalTo(Point.point(CRS.Cartesian, 0, 0)));
        assertThat(LinearReferenceCalculator.reference(l, Math.sqrt(200)/2), equalTo(Point.point(CRS.Cartesian, 5, 5)));
        assertThat(LinearReferenceCalculator.reference(l, Math.sqrt(200)), equalTo(Point.point(CRS.Cartesian, 10, 10)));
        assertThat(LinearReferenceCalculator.reference(l, 100), is(nullValue()));
        assertThat(LinearReferenceCalculator.reference(l, -1), is(nullValue()));
    }

    @Test
    public void referencePolygon() {
        Polygon.SimplePolygon p = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );

        //Forward
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[1], 0), equalTo(Point.point(CRS.Cartesian, -10, -10)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[1], 10), equalTo(Point.point(CRS.Cartesian, 0, -10)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[1], 25), equalTo(Point.point(CRS.Cartesian, 10, -5)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[1], 125), equalTo(Point.point(CRS.Cartesian, 5, 10)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[1], -1), is(nullValue()));

        //Backward
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[3], 0), equalTo(Point.point(CRS.Cartesian, -10, -10)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[3], 10), equalTo(Point.point(CRS.Cartesian, -10, 0)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[3], 25), equalTo(Point.point(CRS.Cartesian, -5, 10)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[3], 125), equalTo(Point.point(CRS.Cartesian, 10, 5)));
        assertThat(LinearReferenceCalculator.reference(p, p.getPoints()[0], p.getPoints()[3], -1), is(nullValue()));
    }
}