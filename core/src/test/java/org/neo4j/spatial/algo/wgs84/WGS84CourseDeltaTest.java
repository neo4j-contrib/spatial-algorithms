package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class WGS84CourseDeltaTest {

    @Test
    public void shouldCalculateCourseDeltaForSimplePolygons() {
        // Given an anti-clockwise square polygon
        Polygon.SimplePolygon simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );

        double result = WGSUtil.courseDelta(simple.getPoints());
        assertThat(result, equalTo(360.0));

        // Given an anti-clockwise triangular polygon
        simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );

        result = WGSUtil.courseDelta(simple.getPoints());
        assertThat(result, equalTo(360.0));

        // Given a clockwise square polygon
        simple = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, -10, 10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, 10, -10)
        );

        result = WGSUtil.courseDelta(simple.getPoints());
        assertThat(result, equalTo(-360.0));

        // Given a clockwise triangular polygon
        simple = Polygon.simple(
                Point.point(CRS.WGS84, 0, 0),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, -10, -10)
        );

        result = WGSUtil.courseDelta(simple.getPoints());
        assertThat(result, equalTo(-360.0));
    }
}