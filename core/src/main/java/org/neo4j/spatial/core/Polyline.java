package org.neo4j.spatial.core;

import java.util.Arrays;
import java.util.StringJoiner;

import static java.lang.String.format;

public interface Polyline {
    static Polyline polyline(Point... points) {
        return new InMemoryPolyline(points);
    }

    static void assertAllSameDimension(Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }

    /**
     * Converts the polygon into an array of LineSegments describing this polygon
     *
     * @return Array of line segments describing the polygon
     */
    default LineSegment[] toLineSegments() {
        Point[] points = getPoints();
        LineSegment[] output = new LineSegment[points.length - 1];

        for (int i = 0; i < output.length; i++) {
            Point a = points[i];
            Point b = points[i+1];
            output[i] = LineSegment.lineSegment(a, b);
        }

        return output;
    }

    int dimension();

    boolean isSimple();

    Point[] getPoints();

    /**
     * Outputs the WKT string of the polygon
     *
     * @return The WKT string describing the polygon
     */
    default String toWKT() {
        return "LINESTRING(" + toWKTPointString(false) + ")";
    }

    /**
     * @param hole True if the polygon represents a hole
     * @return Produces a WKT-representation of the polygon without the suffix and in the correct order
     */
    default String toWKTPointString(boolean hole) {
        Point[] points = getPoints();
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (int i = 0; i < points.length; i++) {
            joiner.add(points[i].getCoordinate()[0] + " " + points[i].getCoordinate()[1]);
        }

        return joiner.toString();
    }
    class InMemoryPolyline implements Polyline {
        private Point[] points;

        private InMemoryPolyline(Point... points) {
            this.points = points;
            if (this.points.length < 2) {
                throw new IllegalArgumentException("Polyline cannot have less than 2 points");
            }
            Polyline.assertAllSameDimension(this.points);
        }

        @Override
        public int dimension() {
            return this.points[0].dimension();
        }

        @Override
        public Point[] getPoints() {
            return this.points;
        }

        @Override
        public boolean isSimple() {
            return true;
        }

        @Override
        public String toString() {
            return format("InMemoryPolyline%s", Arrays.toString(points));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Polyline && this.equals((Polyline) other);
        }

        public boolean equals(Polyline other) {
            Point[] otherPoints = other.getPoints();
            if (points.length != otherPoints.length) {
                return false;
            }
            for (int i = 0; i < points.length; i++) {
                if (!points[i].equals(otherPoints[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(points);
        }
    }
}
