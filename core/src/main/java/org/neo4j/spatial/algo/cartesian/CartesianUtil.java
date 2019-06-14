package org.neo4j.spatial.algo.cartesian;

import static java.lang.String.format;

public class CartesianUtil {
    public static double distance(double[] c1, double[] c2) {
        if (c1.length != c2.length) {
            throw new IllegalArgumentException(format("Cannot calculate distance between points of different dimension: %d != %d", c1.length, c2.length));
        }
        double dsqr = 0;
        for (int i = 0; i < c1.length; i++) {
            double diff = c1[i] - c2[i];
            dsqr += diff * diff;
        }
        return Math.sqrt(dsqr);
    }
}
