package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianDistance;
import org.neo4j.spatial.algo.wgs84.WGS84Distance;
import org.neo4j.spatial.core.*;

public class DistanceCalculator {
    private static CartesianDistance cartesian;
    private static WGS84Distance wgs84;

    private static Distance getCartesian() {
        if (cartesian == null) {
            cartesian = new CartesianDistance();
        }
        return cartesian;
    }

    private static Distance getWGS84() {
        if (wgs84 == null) {
            wgs84 = new WGS84Distance();
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
     * @param polygon
     * @param polyline
     * @return The minimum distance between a polygon and polyline. Returns 0 if the polyline intersects with or is (partially) containted by the polygon
     */
    double distance(Polygon polygon, Polyline polyline) {
        if (CRSChecker.check(polygon, polyline) == CRS.Cartesian) {
            return getCartesian().distance(polygon, polyline);
        } else {
            return getWGS84().distance(polygon, polyline);
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between two multipolylines. Returns 0 if they distance
     */
    public double distance(MultiPolyline a, MultiPolyline b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between a multipolyline and an polyline. Returns 0 if they distance
     */
    public double distance(MultiPolyline a, Polyline b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between a multipolyline and a line segment. Returns 0 if they distance
     */
    public double distance(MultiPolyline a, LineSegment b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param a
     * @param b
     * @return The minimum distance between two polylines. Returns 0 if they distance
     */
    double distance(Polyline a, Polyline b) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            return getCartesian().distance(a, b);
        } else {
            return getWGS84().distance(a, b);
        }
    }

    /**
     * @param polyline
     * @param lineSegment
     * @return The minimum distance between a polyline and line segment. Returns 0 if they distance
     */
    double distance(Polyline polyline, LineSegment lineSegment) {
        if (CRSChecker.check(polyline, lineSegment) == CRS.Cartesian) {
            return getCartesian().distance(polyline, lineSegment);
        } else {
            return getWGS84().distance(polyline, lineSegment);
        }
    }

    /**
     * @param polyline
     * @param point
     * @return The minimum distance between a polyline and point
     */
    double distance(Polyline polyline, Point point) {
        if (CRSChecker.check(polyline, point) == CRS.Cartesian) {
            return getCartesian().distance(polyline, point);
        } else {
            return getWGS84().distance(polyline, point);
        }
    }

    /**
     * @param lineSegment
     * @return The distance between the two end points of a line segment
     */
    public static double distance(LineSegment lineSegment) {
        if (CRSChecker.check(lineSegment) == CRS.Cartesian) {
            return getCartesian().distance(lineSegment);
        } else {
            return getWGS84().distance(lineSegment);
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
