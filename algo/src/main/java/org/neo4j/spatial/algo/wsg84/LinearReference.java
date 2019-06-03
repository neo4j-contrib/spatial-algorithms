package org.neo4j.spatial.algo.wsg84;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

import java.util.Arrays;

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

        Point p = lineSegment.getPoints()[0];
        Point q = lineSegment.getPoints()[1];
        double length = Distance.distance(p, q);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        //To radians
        double[] a = Arrays.stream(p.getCoordinate()).map(r -> r * Math.PI/180).toArray();
        double[] b = Arrays.stream(q.getCoordinate()).map(r -> r * Math.PI/180).toArray();

        //To n-vector
        Vector u = new Vector(Math.cos(a[1]) * Math.cos(a[0]), Math.cos(a[1]) * Math.sin(a[0]), Math.sin(a[1]));
        Vector v = new Vector(Math.cos(b[1]) * Math.cos(b[0]), Math.cos(b[1]) * Math.sin(b[0]), Math.sin(b[1]));

        Vector x = u.add(v.subtract(u).multiply(fraction));

        return Point.point(Math.atan2(x.getCoordinate(1), x.getCoordinate(0)) * 180 / Math.PI, Math.atan2(x.getCoordinate(2), Math.sqrt(Math.pow(x.getCoordinate(0), 2) + Math.pow(x.getCoordinate(1), 2))) * 180 / Math.PI);
    }
}
