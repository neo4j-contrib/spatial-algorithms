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

    public static Distance getCalculator(CRS crs) {
        if (crs == CRS.Cartesian) {
            return getCartesian();
        } else if (crs == CRS.WGS84) {
            return getWGS84();
        } else {
            throw new IllegalArgumentException("The coordinate reference system is not supported for distance calculations: " + crs);
        }
    }

    public static Distance getCalculator(HasCRS geometry) {
        return getCalculator(geometry.getCRS());
    }
}
