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

    public static CCW getCalculator(CRS crs) {
        if (crs == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static CCW getCalculator(Polygon.SimplePolygon polygon) {
        if (polygon.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static CCW getCalculator(Point[] points) {
        if (points[0].getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }
}
