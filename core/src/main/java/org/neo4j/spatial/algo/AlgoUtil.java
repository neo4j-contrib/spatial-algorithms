package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;

public class AlgoUtil {
    public static final double EPSILON = 0.000000003;

    /**
     * Computes the direction of the of the a-b-c turn by computing the z-component of ab x ac
     *
     * @param a starting point
     * @param b turning point
     * @param c ending point
     * @return Integer value with the following property:
     *          ccw lt 0: clockwise turn;
     *          ccw eq 0: collinear;
     *          ccw gt 1: counterclockwise turn
     */
    public static int ccw(Point a, Point b, Point c) {
        return ccw(a.getCoordinate(), b.getCoordinate(), c.getCoordinate());
    }

    /**
     * Computes the direction of the of the a-b-c turn by computing the z-component of ab x ac
     *
     * @param a starting coordinate
     * @param b turning coordinate
     * @param c ending coordinate
     * @return Integer value with the following property:
     *          ccw &lt; 0: clockwise turn;
     *          ccw == 0: collinear;
     *          ccw &gt; 1: counterclockwise turn
     */
    public static int ccw(double[] a, double[] b, double[] c) {
        double z = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
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

    public static boolean equal(double a, double b, double epsilon) {
        return Math.abs(a-b) < epsilon;
    }

    public static boolean lessOrEqual(double a, double b) {
        return a-b <= EPSILON;
    }

    public static boolean equal(double[] a, double[] b) {
        return equal(a[0], b[0]) && equal(a[1], b[1]);
    }

    public static double[] rotate(double[] c, double angle) {
        double x = c[0];
        double y = c[1];

        double rotatedX = x * Math.cos(angle) - y * Math.sin(angle);
        double rotatedY = y * Math.cos(angle) + x * Math.sin(angle);

        return new double[]{rotatedX, rotatedY};
    }
}
