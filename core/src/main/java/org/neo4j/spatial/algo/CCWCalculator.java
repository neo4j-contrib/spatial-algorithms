package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianCCW;
import org.neo4j.spatial.algo.wgs84.WGS84CCW;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class CCWCalculator {
    private static CartesianCCW cartesian;
    private static WGS84CCW wgs84;

    private static CCW getCartesian() {
        if (cartesian == null) {
            cartesian = new CartesianCCW();
        }
        return cartesian;
    }

    private static CCW getWGS84() {
        if (wgs84 == null) {
            wgs84 = new WGS84CCW();
        }
        return wgs84;
    }

    /**
     * @param polygon
     * @return True iff the points of the given polygon are in CCW order
     */
    public static boolean isCCW(Polygon.SimplePolygon polygon) {
        if (CRSChecker.check(polygon) == CRS.Cartesian) {
            return getCartesian().isCCW(polygon);
        } else {
            return getWGS84().isCCW(polygon);
        }
    }

    /**
     * @param points
     * @return True iff the points are in CCW order
     */
    public static boolean isCCW(Point[] points) {
        if (CRSChecker.check(points[0]) == CRS.Cartesian) {
            return getCartesian().isCCW(points);
        } else {
            return getWGS84().isCCW(points);
        }
    }
}
