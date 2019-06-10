package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class DistanceCalculator {
    private static org.neo4j.spatial.algo.cartesian.Distance cartesian;
    private static org.neo4j.spatial.algo.wgs84.Distance wgs84;

    private static Distance getCartesian() {
        if (cartesian == null) {
            cartesian = new org.neo4j.spatial.algo.cartesian.Distance();
        }
        return cartesian;
    }

    private static Distance getWGS84() {
        if (wgs84 == null) {
            wgs84 = new org.neo4j.spatial.algo.wgs84.Distance();
        }
        return wgs84;
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between two polygons. Returns 0 if one polygon is (partially) contained by the other
     */
    public static double distance(Polygon a, Polygon b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param polygon
     * @param lineSegment
     * @return The minimum distance between a polygon and line segment. Returns 0 if the line segment is (partially) contained by the polygon
     */
    public static double distance(Polygon polygon, LineSegment lineSegment) {
        if (CRSChecker.check(polygon, lineSegment) == CRS.Cartesian) {
            return getCartesian().distance(polygon, lineSegment);
        } else {
            return getWGS84().distance(polygon, lineSegment);
        }
    }

    /**
     * @param polygon
     * @param point
     * @return The minimum distance between a polygon and point. Returns 0 if point is within the polygon
     */
    public static double distance(Polygon polygon, Point point) {
        if (CRSChecker.check(polygon, point) == CRS.Cartesian) {
            return getCartesian().distance(polygon, point);
        } else {
            return getWGS84().distance(polygon, point);
        }
    }

    /**
     * @param lineSegment
     * @param point
     * @return The minimum distance between a line segment and a point
     */
    public static double distance(LineSegment lineSegment, Point point) {
        if (CRSChecker.check(lineSegment, point) == CRS.Cartesian) {
            return getCartesian().distance(lineSegment, point);
        } else {
            return getWGS84().distance(lineSegment, point);
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between two line segments
     */
    public static double distance(LineSegment a, LineSegment b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param p1
     * @param p2
     * @return The minimum distance between two points
     */
    public static double distance(Point p1, Point p2) {
        if (CRSChecker.check(p1, p2) == CRS.Cartesian) {
            return getCartesian().distance(p1, p2);
        } else {
            return getWGS84().distance(p1, p2);
        }
    }
}
