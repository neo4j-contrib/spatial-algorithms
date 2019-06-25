package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianArea;
import org.neo4j.spatial.algo.wgs84.WGS84Area;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Polygon;

public class AreaCalculator {
    private static CartesianArea cartesian;
    private static WGS84Area wgs84;

    private static Area getCartesian() {
        if (cartesian == null) {
            cartesian = new CartesianArea();
        }
        return cartesian;
    }

    private static Area getWGS84() {
        if (wgs84 == null) {
            wgs84 = new WGS84Area();
        }
        return wgs84;
    }

    public static Area getCalculator(Polygon polygon) {
        if (polygon.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static Area getCalculator(CRS crs) {
        if (crs == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }
}