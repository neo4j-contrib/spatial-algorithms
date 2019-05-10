package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;

import static java.lang.String.format;

public class Distance {

    public static double distance(Point p1, Point p2) {
        double[] c1 = p1.getCoordinate();
        double[] c2 = p2.getCoordinate();
        return distance(c1, c2);
    }

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
