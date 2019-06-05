package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;

public class AlgoUtil {
//    public static final double EPSILON = 0.00000000000001;
    public static final double EPSILON = 0.000000000001;

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

    public static double dotProduct(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors do not have the same dimension");
        }

        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static boolean equal(double a, double b) {
        return Math.abs(a-b) < EPSILON;
    }

    public static boolean lessOrEqual(double a, double b) {
        return a-b <= EPSILON;
    }

    public static boolean equal(Point a, Point b) {
        return equal(a.getCoordinate()[0], b.getCoordinate()[0]) && equal(a.getCoordinate()[1], b.getCoordinate()[1]);
    }

    public static double[] rotate(Point p, double angle) {
        double x = p.getCoordinate()[0];
        double y = p.getCoordinate()[1];

        double rotatedX = x * Math.cos(angle) - y * Math.sin(angle);
        double rotatedY = y * Math.cos(angle) + x * Math.sin(angle);

        return new double[]{rotatedX, rotatedY};
    }
}
