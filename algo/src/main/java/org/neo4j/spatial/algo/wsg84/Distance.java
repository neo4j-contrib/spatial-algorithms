package org.neo4j.spatial.algo.wsg84;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

import java.util.Arrays;

public interface Distance {
    /**
     * @param a
     * @param b
     * @return The minimum distance between two polygons. Returns 0 if one polygon is (partially) contained by the other
     */
    public static double distance(Polygon a, Polygon b) {
        //TODO implement this method
        return 0;
    }

    /**
     * @param polygon
     * @param lineSegment
     * @return The minimum distance between a polygon and line segment. Returns 0 if the line segment is (partially) contained by the polygon
     */
    public static double distance(Polygon polygon, LineSegment lineSegment) {
        LineSegment[] lineSegments = polygon.toLineSegments();

        double minDistance = Double.MAX_VALUE;

        for (LineSegment currentLineSegment : lineSegments) {
            double current = distance(currentLineSegment, lineSegment);

            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    /**
     * @param polygon
     * @param point
     * @return The minimum distance between a polygon and point. Returns 0 if point is within the polygon
     */
    public static double distance(Polygon polygon, Point point) {
        //TODO if point is inside the polygon, the distance is 0

        LineSegment[] lineSegments = polygon.toLineSegments();

        double minDistance = Double.MAX_VALUE;

        for (LineSegment lineSegment : lineSegments) {
            double current = distance(lineSegment, point);
            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    /**
     * @param lineSegment
     * @param point
     * @return The minimum distance between a line segment and a point
     */
    public static double distance(LineSegment lineSegment, Point point) {
        double[] a1 = Arrays.stream(lineSegment.getPoints()[0].getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] a2 = Arrays.stream(lineSegment.getPoints()[1].getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] p = Arrays.stream(point.getCoordinate()).map(q -> q * Math.PI/180).toArray();

        //To n-vector
        Vector u1 = new Vector(Math.cos(a1[1]) * Math.cos(a1[0]), Math.cos(a1[1]) * Math.sin(a1[0]), Math.sin(a1[1]));
        Vector u2 = new Vector(Math.cos(a2[1]) * Math.cos(a2[0]), Math.cos(a2[1]) * Math.sin(a2[0]), Math.sin(a2[1]));
        Vector v = new Vector(Math.cos(p[1]) * Math.cos(p[0]), Math.cos(p[1]) * Math.sin(p[0]), Math.sin(p[1]));

        Vector gc = u1.cross(u2);

        //Check whether the point is within the extent of the line segment
        Vector u1v = v.subtract(u1);
        Vector u2v = v.subtract(u2);
        Vector u1u2 = u2.subtract(u1);
        Vector u2u1 = u1.subtract(u2);

        //These dot products tell us whether the point is on the same side as a point of the line segment compared to the remaining point of the line segment
        double extent1 = u1v.dot(u1u2);
        double extent2 = u2v.dot(u2u1);

        boolean isSameHemisphere = v.dot(u1) >= 0 && v.dot(u2) >= 0;

        boolean withinExtend = extent1 >= 0 && extent2 >= 0 && isSameHemisphere;

        if (withinExtend && !u1.equals(u2)) {
            Vector c1 = u1.cross(u2); // n1×n2 = vector representing great circle through the line segments
            Vector c2 = v.cross(c1);  // n0×c1 = vector representing great circle through the point normal to c1
            Vector n = c1.cross(c2);  // c2×c1 = nearest point on c1 to n0

            return distance(v, n);
        } else {
            double d1 = distance(v, u1);
            double d2 = distance(v, u2);

            return d1 < d2 ? d1 : d2;
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between two line segments
     */
    public static double distance(LineSegment a, LineSegment b) {
        Point intersect = Intersect.intersect(a, b);
        if (intersect != null) {
            return 0;
        }

        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < a.getPoints().length; i++) {
            double distance = distance(b, a.getPoints()[i]);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        for (int i = 0; i < b.getPoints().length; i++) {
            double distance = distance(a, b.getPoints()[i]);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }

    /**
     * @param p1
     * @param p2
     * @return The minimum distance between two points
     */
    public static double distance(Point p1, Point p2) {
        double radius = 6371e3;

        //To radians
        double[] a = Arrays.stream(p1.getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] b = Arrays.stream(p2.getCoordinate()).map(p -> p * Math.PI/180).toArray();

        //To n-vector
        Vector u = new Vector(Math.cos(a[1]) * Math.cos(a[0]), Math.cos(a[1]) * Math.sin(a[0]), Math.sin(a[1]));
        Vector v = new Vector(Math.cos(b[1]) * Math.cos(b[0]), Math.cos(b[1]) * Math.sin(b[0]), Math.sin(b[1]));

        //Distance (in meters)
        return radius * Math.atan2(u.cross(v).magnitude(), u.dot(v));
    }

    /**
     * @param u
     * @param v
     * @return The minimum distance between two vectors representing points
     */
    public static double distance(Vector u, Vector v) {
        double radius = 6371e3;

        //Distance (in meters)
        return radius * Math.atan2(u.cross(v).magnitude(), u.dot(v));
    }
}
