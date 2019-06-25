package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianLinearReference;
import org.neo4j.spatial.algo.wgs84.WGS84LinearReference;
import org.neo4j.spatial.core.CRS;
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

    public static LinearReference getCalculator(CRS crs) {
        if (crs == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static LinearReference getCalculator(Polygon.SimplePolygon polygon) {
        if (polygon.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }
}
