package org.neo4j.spatial.core;

import org.neo4j.spatial.algo.AlgoUtil;

import java.util.Arrays;

public class PolygonUtil<T extends Point> {
    public T[] closeRing(T... points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("Cannot close ring of less than 2 points");
        }
        T first = points[0];
        T last = points[points.length - 1];
        if (AlgoUtil.equal(first, last)) {
            return points;
        } else {
            T[] closed = Arrays.copyOf(points, points.length + 1);
            closed[points.length] = points[0];
            return closed;
        }
    }

    public T[] openRing(T[] points) {
        T[] copy = Arrays.copyOf(points, points.length - 1);
        return copy;
    }
}
