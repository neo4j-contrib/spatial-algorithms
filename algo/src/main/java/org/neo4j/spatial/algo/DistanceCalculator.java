package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianDistance;
import org.neo4j.spatial.algo.wgs84.WGS84Distance;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

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
        } else {
            return getWGS84();
        }
    }

    public static Distance getCalculator(Polygon a) {
        if (a.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static Distance getCalculator(MultiPolyline a) {
        if (a.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static Distance getCalculator(Polyline a) {
        if (a.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static Distance getCalculator(LineSegment a) {
        if (a.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }

    public static Distance getCalculator(Point a) {
        if (a.getCRS() == CRS.Cartesian) {
            return getCartesian();
        } else {
            return getWGS84();
        }
    }
}
