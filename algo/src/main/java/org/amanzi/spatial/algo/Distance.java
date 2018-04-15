package org.amanzi.spatial.algo;

import org.amanzi.spatial.core.Point;

import static java.lang.String.format;

public class Distance {

    public static double distance(Point p1, Point p2) {
        double[] c1 = p1.getCoordinate();
        double[] c2 = p2.getCoordinate();
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
