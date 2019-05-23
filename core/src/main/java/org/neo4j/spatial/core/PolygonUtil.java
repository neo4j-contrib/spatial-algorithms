package org.neo4j.spatial.core;

import org.neo4j.spatial.algo.AlgoUtil;

import java.util.Arrays;

public class PolygonUtil {
    public static Point[] closeRing(Point... points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("Cannot close ring of less than 2 points");
        }
        Point first = points[0];
        Point last = points[points.length - 1];
        if (AlgoUtil.equal(first, last)) {
            return points;
        } else {
            Point[] closed = Arrays.copyOf(points, points.length + 1);
            closed[points.length] = points[0];
            return closed;
        }
    }

    public static Point[] openRing(Point[] points) {
        Point[] copy = Arrays.copyOf(points, points.length - 1);
        return copy;
    }
}
