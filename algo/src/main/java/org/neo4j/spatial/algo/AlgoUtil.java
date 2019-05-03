package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;

public class AlgoUtil {
    private static final double EPSILON = 0.00000000000001;

    /**
     * Computes the direction of the of the a-b-c turn by computing the z-component of ab x ac
     *
     * @param a starting point
     * @param b turning point
     * @param c ending point
     * @return Integer value with the following property:
     *          ccw < 0: clockwise turn;
     *          ccw = 0: collinear;
     *          ccw > 1: counterclockwise turn
     */
    public static int ccw(Point a, Point b, Point c) {
        double z = (b.getCoordinate()[0] - a.getCoordinate()[0]) * (c.getCoordinate()[1] - a.getCoordinate()[1]) - (b.getCoordinate()[1] - a.getCoordinate()[1]) * (c.getCoordinate()[0] - a.getCoordinate()[0]);
        return z == 0 ? 0 : (z < 0 ? -1 : 1);
    }

    public static boolean equal(double a, double b) {
        return Math.abs(a-b) < EPSILON;
    }
}
