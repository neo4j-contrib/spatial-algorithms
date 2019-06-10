package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class CRSChecker {
    public static CRS check(Polygon a, Polygon b) throws IllegalArgumentException {
        CRS x = getCRS(a);
        CRS y = getCRS(b);

        return check(x, y);
    }

    public static CRS check(Polygon polygon, LineSegment lineSegment) throws IllegalArgumentException {
        CRS x = getCRS(polygon);
        CRS y = getCRS(lineSegment);

        return check(x, y);
    }

    public static CRS check(Polygon polygon, Point point) throws IllegalArgumentException {
        CRS x = getCRS(polygon);
        CRS y = getCRS(point);

        return check(x, y);
    }

    public static CRS check(LineSegment lineSegment, Point point) throws IllegalArgumentException {
        CRS x = getCRS(lineSegment);
        CRS y = getCRS(point);

        return check(x, y);
    }

    public static CRS check(LineSegment a, LineSegment b) throws IllegalArgumentException {
        CRS x = getCRS(a);
        CRS y = getCRS(b);

        return check(x, y);
    }

    public static CRS check(Point p1, Point p2) throws IllegalArgumentException {
        CRS x = getCRS(p1);
        CRS y = getCRS(p2);

        return check(x, y);
    }

    private static CRS check(CRS x, CRS y) throws IllegalArgumentException {
        if (x != y) {
            throw new IllegalArgumentException("Incompatible Cooordinate Reference Systems");
        }
        return x;
    }

    private static CRS getCRS(Point point) {
        return point.getCRS();
    }

    private static CRS getCRS(Polygon polygon) {
        return polygon.getShells()[0].getPoints()[0].getCRS();
    }

    private static CRS getCRS(LineSegment lineSegment) {
        return lineSegment.getPoints()[0].getCRS();
    }
}
