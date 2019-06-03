package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.Within;
import org.neo4j.spatial.algo.cartesian.Intersect.Intersect;
import org.neo4j.spatial.algo.cartesian.Intersect.MCSweepLineIntersect;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import static java.lang.String.format;

public class Distance {

    /**
     * @param a
     * @param b
     * @return The minimum distance between two polygons. Returns 0 if one polygon is (partially) contained by the other
     */
    public static double distance(Polygon a, Polygon b) {
        boolean intersects = new MCSweepLineIntersect().doesIntersect(a, b);

        //Check if one polygon is (partially) contained by the other
        if (intersects) {
            return 0;
        } else  if (Within.within(a, b.getShells()[0].getPoints()[0]) || Within.within(b, a.getShells()[0].getPoints()[0])) {
            return 0;
        }

        double minDistance = Double.MAX_VALUE;

        LineSegment[] aLS = a.toLineSegments();

        LineSegment[] bLS = b.toLineSegments();

        for (LineSegment aLineSegment : aLS) {
            for (LineSegment bLineSegment : bLS) {
                double current = distance(aLineSegment, bLineSegment);
                if (current < minDistance) {
                    minDistance = current;
                }
            }
        }

        return minDistance;
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
        if (Within.within(polygon, point)) {
            return 0;
        }

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
        Point u = lineSegment.getPoints()[0];
        Point v = lineSegment.getPoints()[1];
        double[] a = new double[]{
                v.getCoordinate()[0] - u.getCoordinate()[0],
                v.getCoordinate()[1] - u.getCoordinate()[1],
        };
        double[] b = new double[]{
                point.getCoordinate()[0] - u.getCoordinate()[0],
                point.getCoordinate()[1] - u.getCoordinate()[1],
        };

        double dotProduct = AlgoUtil.dotProduct(a, b);
        double lengthSquared = a[0] * a[0] + a[1] * a[1];

        double t = Math.max(0, Math.min(1, dotProduct/lengthSquared));

        Point projection = v.subtract(u.getCoordinate()).multiply(t).add(u.getCoordinate());

        return distance(projection, point);
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
        double[] c1 = p1.getCoordinate();
        double[] c2 = p2.getCoordinate();
        return distance(c1, c2);
    }

    public static double distance(double[] c1, double[] c2) {
        if (c1.length != c2.length) {
            throw new IllegalArgumentException(format("Cannot calculate distance between points of different dimension: %d != %d", c1.length, c2.length));
        }
        double dsqr = 0;
        for (int i = 0; i < c1.length; i++) {
            double diff = c1[i] - c2[i];
            dsqr += diff * diff;
        }
        return Math.sqrt(dsqr);
    }

}
