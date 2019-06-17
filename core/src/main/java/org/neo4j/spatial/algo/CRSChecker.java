package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.*;

public class CRSChecker {
    public static CRS check(Point point) {
        return point.getCRS();
    }

    public static CRS check(Polyline polyline) {
        return polyline.getCRS();
    }

    public static CRS check(Polygon polygon) {
        return polygon.getCRS();
    }

    public static CRS check(LineSegment lineSegment) {
        return lineSegment.getCRS();
    }

    public static CRS check(Polygon a, Polygon b) throws IllegalArgumentException {
        CRS x = a.getCRS();
        CRS y = b.getCRS();

        return check(x, y);
    }

    public static CRS check(Polygon polygon, LineSegment lineSegment) throws IllegalArgumentException {
        CRS x = polygon.getCRS();
        CRS y = lineSegment.getCRS();

        return check(x, y);
    }

    public static CRS check(Polygon polygon, Point point) throws IllegalArgumentException {
        CRS x = polygon.getCRS();
        CRS y = point.getCRS();

        return check(x, y);
    }

    public static CRS check(Polygon polygon, Polyline polyline) throws IllegalArgumentException {
        CRS x = polygon.getCRS();
        CRS y = polyline.getCRS();

        return check(x, y);
    }

    public static CRS check(Polyline a, Polyline b) throws IllegalArgumentException {
        CRS x = a.getCRS();
        CRS y = b.getCRS();

        return check(x, y);
    }

    public static CRS check(Polyline polyline, LineSegment lineSegment) throws IllegalArgumentException {
        CRS x = polyline.getCRS();
        CRS y = lineSegment.getCRS();

        return check(x, y);
    }

    public static CRS check(Polyline polyline, Point point) throws IllegalArgumentException {
        CRS x = polyline.getCRS();
        CRS y = point.getCRS();

        return check(x, y);
    }

    public static CRS check(LineSegment lineSegment, Point point) throws IllegalArgumentException {
        CRS x = lineSegment.getCRS();
        CRS y = point.getCRS();

        return check(x, y);
    }

    public static CRS check(LineSegment a, LineSegment b) throws IllegalArgumentException {
        CRS x = a.getCRS();
        CRS y = b.getCRS();

        return check(x, y);
    }

    public static CRS check(Point p1, Point p2) throws IllegalArgumentException {
        CRS x = p1.getCRS();
        CRS y = p2.getCRS();

        return check(x, y);
    }

    private static CRS check(CRS x, CRS y) throws IllegalArgumentException {
        if (x != y) {
            throw new IllegalArgumentException("Incompatible Cooordinate Reference Systems");
        }
        return x;
    }
}
