package org.neo4j.spatial.core;

import java.util.Arrays;

import static java.lang.String.format;

public interface Point {
    static Point point(double... coordinate) {
        return new InMemoryPoint(coordinate);
    }

    double[] getCoordinate();

    default int dimension() {
        return getCoordinate().length;
    }

    /**
     * If the points are not comparable, this method returns null. That can happen if the points have different
     * dimensions, or if the coordinates are not all greater, all less than or all the same.
     */
    default Integer ternaryCompareTo(Point other) {
        Integer result = null;
        if (this.getCoordinate().length != other.getCoordinate().length) return null;
        for (int i = 0; i < this.getCoordinate().length; i++) {
            double diff = this.getCoordinate()[i] - other.getCoordinate()[i];
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

    default Point withShift(double... shifts) {
        double[] shifted = Arrays.copyOf(this.getCoordinate(), this.getCoordinate().length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] += shifts[i];
        }
        return Point.point(shifted);
    }

    default String toWKT() {
        return "POINT(" + getCoordinate()[0] + " " + getCoordinate()[1] + ")";
    }
}

class InMemoryPoint implements Point {
    private final double[] coordinate;

    public InMemoryPoint(double... coordinate) {
        if (coordinate.length < 1) {
            throw new IllegalArgumentException("Cannot create point with zero dimensions");
        }
        this.coordinate = coordinate;
    }

    public boolean equals(Point other) {
        return Arrays.equals(this.coordinate, other.getCoordinate());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Point && this.equals((Point) other);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinate);
    }

    @Override
    public double[] getCoordinate() {
        return coordinate;
    }

    public String toString() {
        return format("InMemoryPoint%s", Arrays.toString(coordinate));
    }
}