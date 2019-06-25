package org.neo4j.spatial.core;

import java.util.Arrays;
import java.util.StringJoiner;

public class Vector {
    private double[] coordinates;

    public Vector(double... coordinates) {
        this.coordinates = coordinates;
    }

    public Vector(boolean asPoint, double... coordinates) {
        double[] p = Arrays.stream(coordinates).map(q -> q * Math.PI/180).toArray();
        this.coordinates = new double[]{Math.cos(p[1]) * Math.cos(p[0]), Math.cos(p[1]) * Math.sin(p[0]), Math.sin(p[1])};
    }

    /**
     * Converts a LongLat point to an n-vector
     * @param point
     */
    public Vector(Point point) {
        double[] p = Arrays.stream(point.getCoordinate()).map(q -> q * Math.PI/180).toArray();
        this.coordinates = new double[]{Math.cos(p[1]) * Math.cos(p[0]), Math.cos(p[1]) * Math.sin(p[0]), Math.sin(p[1])};
    }

    public double getCoordinate(int i) {
        return coordinates[i];
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public Vector add(Vector shifts) {
        double[] shifted = Arrays.copyOf(this.getCoordinates(), this.getCoordinates().length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] += shifts.getCoordinates()[i];
        }
        return new Vector(shifted);
    }

    public Vector subtract(Vector shifts) {
        double[] shifted = Arrays.copyOf(this.getCoordinates(), this.getCoordinates().length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] -= shifts.getCoordinates()[i];
        }
        return new Vector(shifted);
    }

    public Vector multiply(double scalar) {
        double[] shifted = Arrays.copyOf(this.getCoordinates(), this.getCoordinates().length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] *= scalar;
        }
        return new Vector(shifted);
    }

    public Vector divide(double scalar) {
        double[] shifted = Arrays.copyOf(this.getCoordinates(), this.getCoordinates().length);
        for (int i = 0; i < shifted.length; i++) {
            shifted[i] /= scalar;
        }
        return new Vector(shifted);
    }

    public double magnitude() {
        return Math.sqrt(coordinates[0] * coordinates[0] + coordinates[1] * coordinates[1] + coordinates[2] * coordinates[2]);
    }

    public double dot(Vector other) {
        if (this.getCoordinates().length != other.getCoordinates().length) {
            throw new IllegalArgumentException("Vectors do not have the same dimension");
        }

        double sum = 0;
        for (int i = 0; i < this.getCoordinates().length; i++) {
            sum += this.getCoordinates()[i] * other.getCoordinates()[i];
        }
        return sum;
    }

    public Vector cross(Vector other) {
        return new Vector(
                this.getCoordinate(1) * other.getCoordinate(2) - this.getCoordinate(2) * other.getCoordinate(1),
                this.getCoordinate(2) * other.getCoordinate(0) - this.getCoordinate(0) * other.getCoordinate(2),
                this.getCoordinate(0) * other.getCoordinate(1) - this.getCoordinate(1) * other.getCoordinate(0)
        );
    }

    public boolean equals(Vector other) {
        return Arrays.equals(this.coordinates, other.getCoordinates());
    }

    public Point toPoint() {
        return Point.point(CRS.WGS84, Math.atan2(this.coordinates[1], this.coordinates[0]) * 180 / Math.PI, Math.atan2(this.coordinates[2], Math.sqrt(Math.pow(this.coordinates[0], 2) + Math.pow(this.coordinates[1], 2))) * 180 / Math.PI);
    }

    public Vector normalize() {
        double magnitude = this.magnitude();

        if (magnitude == 0 || magnitude == 1) {
            return this;
        }

        double[] modifiedCoordinates = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            modifiedCoordinates[i] = coordinates[i] / magnitude;
        }

        return new Vector(modifiedCoordinates);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vector && this.equals((Vector) other);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "Vector{", "}");
        for (double coordinate : coordinates) {
            joiner.add(coordinate + "");
        }
        return joiner.toString();
    }
}
