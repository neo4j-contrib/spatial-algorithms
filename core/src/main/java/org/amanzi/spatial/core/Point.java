package org.amanzi.spatial.core;

import java.util.Arrays;

import static java.lang.String.format;

public class Point {
    private final double[] coordinate;

    public Point(double... coordinate) {
        if (coordinate.length < 1) {
            throw new IllegalArgumentException("Cannot create point with zero dimensions");
        }
        this.coordinate = coordinate;
    }

    public int dimension() {
        return coordinate.length;
    }

    public boolean equals(Point other) {
        return Arrays.equals(this.coordinate, other.coordinate);
    }

    public boolean equals(Object other) {
        return other instanceof Point && this.equals((Point) other);
    }

    // TODO: move to algorithms package
    public double distance(Point other) {
        double dsqr = 0;
        for (int i = 0; i < this.coordinate.length; i++) {
            double diff = this.coordinate[i] - other.coordinate[i];
            dsqr += diff * diff;
        }
        return Math.sqrt(dsqr);
    }

    /**
     * If the points are not comparable, this method returns null. That can happen if the points have different
     * dimensions, or if the coordinates are not all greater, all less than or all the same.
     */
    public Integer ternaryCompareTo(Point other) {
        Integer result = null;
        if (this.coordinate.length != other.coordinate.length) return null;
        for (int i = 0; i < this.coordinate.length; i++) {
            double diff = this.coordinate[i] - other.coordinate[i];
            int ans = (diff > 0) ? 1 : ((diff < 0) ? -1 : 0);
            if (result == null) {
                result = ans;
            } else {
                if (result != ans) {
                    if (result * ans != 0) return null;
                    else if (result == 0) result = ans;
                }
            }
        }
        return result;
    }

    public double[] getCoordinate() {
        return coordinate;
    }

    public String toString() {
        return format("Point%s", Arrays.toString(coordinate));
    }

    public Point withShift(double... shifts) {
        double[] shifted = Arrays.copyOf(this.coordinate, this.coordinate.length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] += shifts[i];
        }
        return new Point(shifted);
    }
}