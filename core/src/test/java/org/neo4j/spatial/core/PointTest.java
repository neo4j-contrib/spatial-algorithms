package org.neo4j.spatial.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PointTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldAcceptValidCoordinates() {
        Point point = Point.point(1, 2, 3);
        Point other = Point.point(1, 2, 3.001);
        Point same = Point.point(1, 2, 3.000);
        assertThat(point.getCoordinate(), equalTo(new double[]{1, 2, 3}));
        assertThat(point, not(equalTo(other)));
        assertThat(point, equalTo(same));
    }

    @Test
    public void shouldCompareDifferentDimensionedPoints() {
        Point point = Point.point(1, 2, 3);
        Point other = Point.point(1, 2);
        assertThat(point, not(equalTo(other)));
    }

    @Test
    public void shouldNotAcceptInvalidCoordinates() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot create point with zero dimensions");
        Point.point();
    }

    @Test
    public void shouldComparePoints() {
        Point point = Point.point(0, 0, 0);

        // comparing with obvious cases
        assertThat(point.ternaryCompareTo(Point.point(1, 1, 1)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(-1, -1, -1)), equalTo(1));
        assertThat(point.ternaryCompareTo(Point.point(0, 0, 0)), equalTo(0));
        assertThat(point.ternaryCompareTo(Point.point(1, 1)), is(nullValue()));
        assertThat(point.ternaryCompareTo(Point.point(1, 1, -1)), is(nullValue()));

        // if some coordinates are zero, we can still comapre on others
        assertThat(point.ternaryCompareTo(Point.point(1, 1, 0)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(1, 0, 0)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(0, 1, 0)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(-1, -1, 0)), equalTo(1));
        assertThat(point.ternaryCompareTo(Point.point(-1, 0, 0)), equalTo(1));
        assertThat(point.ternaryCompareTo(Point.point(0, -1, 0)), equalTo(1));

        // negative 0 should not change the results
        assertThat(point.ternaryCompareTo(Point.point(-0, 1, 0)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(0, -1, -0)), equalTo(1));

        // comparing with large numbers
        assertThat(point.ternaryCompareTo(Point.point(100000, 100000, 100000)), equalTo(-1));
        assertThat(point.ternaryCompareTo(Point.point(-100000, -100000, -100000)), equalTo(1));

        // reverse compare
        assertThat(Point.point(100000, 100000, 100000).ternaryCompareTo(point), equalTo(1));
        assertThat(Point.point(-100000, -100000, -100000).ternaryCompareTo(point), equalTo(-1));
        assertThat(Point.point(100000, 100000, 100000).ternaryCompareTo(Point.point(1,1,1)), equalTo(1));
        assertThat(Point.point(-100000, -100000, -100000).ternaryCompareTo(Point.point(-1,-1,-1)), equalTo(-1));
    }
}
