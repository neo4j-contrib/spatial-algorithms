package org.neo4j.spatial.core;

import org.neo4j.spatial.algo.CRSChecker;
import org.neo4j.spatial.algo.cartesian.CartesianUtil;
import org.neo4j.spatial.algo.wgs84.WGSUtil;

import java.util.Arrays;
import java.util.StringJoiner;

import static java.lang.String.format;

public interface Polyline {
    static Polyline polyline(Point... points) {
        return new InMemoryPolyline(points);
    }

    static int assertAllSameDimension(Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
        return points[0].dimension();
    }

    static CRS assertAllSameCRS(Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].getCRS() != points[i].getCRS()) {
                throw new IllegalArgumentException(format("Point[%d] has different coordinate reference system to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
        return points[0].getCRS();
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

    CRS getCRS();

    int dimension();

    boolean isSimple();

    Point[] getPoints();

    Point getNextPoint();

    void startTraversal(Point startPoint, Point directionPoint);

    void startTraversal();

    boolean fullyTraversed();

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
        private int pointer;
        private int direction;
        private boolean traversing;
        private CRS crs;

        private InMemoryPolyline(Point... points) {
            this.points = points;
            if (this.points.length < 2) {
                throw new IllegalArgumentException("Polyline cannot have less than 2 points");
            }
            Polyline.assertAllSameDimension(this.points);
            crs = Polyline.assertAllSameCRS(this.points);
        }

        @Override
        public CRS getCRS() {
            return crs;
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
        public Point getNextPoint() {
            this.traversing = true;
            Point point = points[pointer];
            pointer = pointer + direction;
            return point;
        }

        @Override
        public void startTraversal(Point startPoint, Point directionPoint) {
            this.traversing = false;
            double minDistance = Double.MAX_VALUE;
            int minIdx = 0;
            for (int i = 0; i < this.points.length; i++) {
                double currentDistance = distance(startPoint, points[i]);
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minIdx = i;
                }
            }

            this.pointer = minIdx;

            int forwardIdx = minIdx + 1;
            int backwardsIdx = minIdx - 1;
            double forwardDistance = Double.MAX_VALUE;
            double backwardDistance = Double.MAX_VALUE;

            if (forwardIdx < points.length) {
                forwardDistance = distance(directionPoint, points[forwardIdx]);
            }

            if (backwardsIdx >= 0) {
                backwardDistance = distance(directionPoint, points[backwardsIdx]);
            }
            if (forwardDistance < backwardDistance) {
                this.direction = 1;
            } else {
                this.direction = -1;
            }
        }

        private double distance(Point start, Point point) {
            if (CRSChecker.check(start, point) == CRS.Cartesian) {
                return CartesianUtil.distance(start.getCoordinate(), point.getCoordinate());
            } else {
                Vector u = new Vector(start);
                Vector v = new Vector(point);
                return WGSUtil.distance(u, v);
            }
        }

        @Override
        public void startTraversal() {
            this.traversing = false;
            this.pointer = 0;
            this.direction = 1;
        }

        @Override
        public boolean fullyTraversed() {
            return (pointer < 0 || pointer >= points.length) && this.traversing;
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
