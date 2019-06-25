package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.CartesianWithin;
import org.neo4j.spatial.algo.wgs84.WGS84Within;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class WithinCalculator {
    public static boolean within(Polygon polygon, Point point) {
        if (polygon.getCRS() == CRS.Cartesian) {
            return CartesianWithin.within(polygon, point);
        } else {
            return WGS84Within.within(polygon, point);
        }
    }

    public static boolean within(Polygon.SimplePolygon polygon, Point point) {
        if (polygon.getCRS() == CRS.Cartesian) {
            return CartesianWithin.within(polygon, point);
        } else {
            return WGS84Within.within(polygon, point);
        }
    }
}
