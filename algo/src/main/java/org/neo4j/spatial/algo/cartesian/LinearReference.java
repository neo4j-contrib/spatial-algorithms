package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public interface LinearReference {
    /**
     * Finds the point on the polygon which is distance d from the start point of the polygon.
     *
     * @param polygon
     * @param d
     * @return The new point, and null if the distance is negative
     */
    public static Point reference(Polygon.SimplePolygon polygon, double d) {
        if (d < 0) {
            return null;
        }

        LineSegment[] lineSegments = polygon.toLineSegments();
        Point point = null;
        int index = 0;
        while (d >= 0) {
            Point p = lineSegments[index].getPoints()[0];
            Point q = lineSegments[index].getPoints()[1];
            double length = Distance.distance(p, q);

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
     * Finds the point on the line segment which is distance d from the start point of the line segment.
     *
     * @param lineSegment
     * @param d
     * @return The new point, and null if the distance is not in the range of the line segment
     */
    public static Point reference(LineSegment lineSegment, double d) {
        if (d < 0) {
            return null;
        }

        double[] p = lineSegment.getPoints()[0].getCoordinate();
        double[] q = lineSegment.getPoints()[1].getCoordinate();
        double length = Distance.distance(p, q);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        return Point.point(p[0] + fraction * (q[0] - p[0]), p[1] + fraction * (q[1] - p[1]));
    }
}
