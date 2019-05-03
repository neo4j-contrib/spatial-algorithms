package org.neo4j.spatial.core;

import java.util.Arrays;
import java.util.StringJoiner;

import static java.lang.String.format;

public interface LineSegment {
    static LineSegment lineSegment(Point a, Point b) {
        return new InMemoryLineSegment(a, b);
    }

    Point[] getPoints();

    /**
     * Returns a copy of the shared point of the two line segments if it exists, else returns null
     *
     * @param a
     * @param b
     * @return Copy of the shared point of the line segments if it exists, else null
     */
    static Point sharedPoint(LineSegment a, LineSegment b) {
        for (Point aPoint : a.getPoints()) {
            for (Point bPoint : b.getPoints()) {
                if (aPoint.equals(bPoint)) {
                    return Point.point(aPoint.getCoordinate());
                }
            }
        }

        return null;
    }

    /**
     * The difference between x-values for the two endpoints of the line segment
     *
     * @param segment
     * @return The difference between x-values for the two endpoints of the line segment
     */
    static double dX(LineSegment segment) {
        return segment.getPoints()[1].getCoordinate()[0] - segment.getPoints()[0].getCoordinate()[0];
    }

    int dimension();

    String toWKT();
}

class InMemoryLineSegment implements LineSegment {
    private final Point[] points;

    public InMemoryLineSegment(Point a, Point b) {
        if (a.dimension() != b.dimension()) {
            throw new IllegalArgumentException("Cannot create line segment from points with different dimensions");
        }
        this.points = new Point[]{a,b};
    }

    public boolean equals(LineSegment other) {
        int a, b;
        a = b = -1;
        for (int i = 0; i < 2; i++) {
            if (this.points[0].equals(other.getPoints()[i])) {
                a = i;
            }
            if (this.points[1].equals(other.getPoints()[i])) {
                b = i;
            }
        }

        return a >= 0 && b >= 0 && a != b;
    }

    public boolean equals(Object other) {
        return other instanceof LineSegment && this.equals((LineSegment) other);
    }

    @Override
    public Point[] getPoints() {
        return points;
    }

    @Override
    public int dimension() {
        return points[0].dimension();
    }

    @Override
    public String toWKT() {
        StringJoiner viewer = new StringJoiner(",", "LINESTRING(", ")");
        for (Point point : points) {
            viewer.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
        }
        return viewer.toString();
    }

    public String toString() {
        return format("InMemoryLineSegment%s", Arrays.toString(points));
    }
}