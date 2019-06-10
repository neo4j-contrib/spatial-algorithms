package org.neo4j.spatial.core;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolygonTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldSee2EqualPolygonsWithUnequalStart() {
        Polygon.SimplePolygon[] variations = new Polygon.SimplePolygon[4];
        for (int i = 0; i < 4; i++) {
            variations[i] = makeSquareWithStart(i);
        }

        for (int i = 0; i < 3; i++) {
            assertThat("two squares should be treated as equal", variations[i], equalTo(variations[i+1]));
        }
    }

    private static Polygon.SimplePolygon makeSquareWithStart(int offset) {
        Point[] points = new Point[4];
        Point[] base = new Point[]{
                Point.point(CRS.Cartesian, -10,-10),
                Point.point(CRS.Cartesian, 10,-10),
                Point.point(CRS.Cartesian, 10,10),
                Point.point(CRS.Cartesian, -10,10)
        };

        for (int i = 0; i < points.length; i++) {
            points[i] = base[(i + offset) % base.length];
        }

        return Polygon.simple(points);
    }
}
