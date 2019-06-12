package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianLinearReference;
import org.neo4j.spatial.algo.wgs84.WGS84LinearReference;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class LinearReferenceCalculator {
    private static CartesianLinearReference cartesian;
    private static WGS84LinearReference wgs84;

    private static LinearReference getCartesian() {
        if (cartesian == null) {
            cartesian = new CartesianLinearReference();
        }
        return cartesian;
    }

    private static LinearReference getWGS84() {
        if (wgs84 == null) {
            wgs84 = new WGS84LinearReference();
        }
        return wgs84;
    }

    /**
     * Finds the point on the polygon which is distance d from the start point of the polygon.
     *
     * @param polygon
     * @param d
     * @return The new point, and null if the distance is negative
     */
    public static Point reference(Polygon.SimplePolygon polygon, double d) {
        if (CRSChecker.check(polygon) == CRS.Cartesian) {
            return getCartesian().reference(polygon, d);
        } else {
            return getWGS84().reference(polygon, d);
        }
    }

    /**
     * Finds the point on the line segment which is distance d from the start point of the line segment.
     *
     * @param lineSegment
     * @param d
     * @return The new point, and null if the distance is not in the range of the line segment
     */
    public static Point reference(LineSegment lineSegment, double d) {
        if (CRSChecker.check(lineSegment) == CRS.Cartesian) {
            return getCartesian().reference(lineSegment, d);
        } else {
            return getWGS84().reference(lineSegment, d);
        }
    }
}
