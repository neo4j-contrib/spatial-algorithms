package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Polygon;

public class CCWCalculator {
    private static org.neo4j.spatial.algo.cartesian.CCW cartesian;
    private static org.neo4j.spatial.algo.wgs84.CCW wgs84;

    private static CCW getCartesian() {
        if (cartesian == null) {
            cartesian = new org.neo4j.spatial.algo.cartesian.CCW();
        }
        return cartesian;
    }

    private static CCW getWGS84() {
        if (wgs84 == null) {
            wgs84 = new org.neo4j.spatial.algo.wgs84.CCW();
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
}
