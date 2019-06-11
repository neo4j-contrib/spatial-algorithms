package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

public abstract class LinearReference {
    /**
     * Finds the point on the polygon which is distance d from the start point of the polygon.
     *
     * @param polygon
     * @param d
     * @return The new point, and null if the distance is negative
     */
    public Point reference(Polygon.SimplePolygon polygon, double d) {
        if (d < 0) {
            return null;
        }

        LineSegment[] lineSegments = polygon.toLineSegments();
        Point point = null;
        int index = 0;
        while (d >= 0) {
            Point p = lineSegments[index].getPoints()[0];
            Point q = lineSegments[index].getPoints()[1];
            double length = DistanceCalculator.distance(p, q);

            if (length < d) {
                d -= length;
            } else {
                point = reference(lineSegments[index], d);
                break;
            }
            index = (index + 1) % lineSegments.length;
        }

        return point;
    }

    /**
     * Finds the point on the polyline which is distance d from the start point of the polyline.
     *
     * @param polyline
     * @param d
     * @return The new point, and null if the distance is negative or is greater than the length of the polyline
     */
    public Point reference(Polyline polyline, double d) {
        if (d < 0) {
            return null;
        }

        LineSegment[] lineSegments = polyline.toLineSegments();
        Point point = null;
        for (LineSegment lineSegment : lineSegments) {
            Point p = lineSegment.getPoints()[0];
            Point q = lineSegment.getPoints()[1];
            double length = DistanceCalculator.distance(p, q);

            if (length < d) {
                d -= length;
            } else {
                point = reference(lineSegment, d);
                break;
            }
        }

        return point;
    }

    /**
     * Finds the point on the line segment which is distance d from the start point of the line segment.
     *
     * @param lineSegment
     * @param d
     * @return The new point, and null if the distance is not in the range of the line segment
     */
    public abstract Point reference(LineSegment lineSegment, double d);
}
