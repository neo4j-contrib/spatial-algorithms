package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class WithinCalculator {
    public static boolean within(Polygon polygon, Point point) {
        if (CRSChecker.check(polygon, point) == CRS.Cartesian) {
            return org.neo4j.spatial.algo.cartesian.Within.within(polygon, point);
        } else {
            return org.neo4j.spatial.algo.wgs84.Within.within(polygon, point);
        }
    }

    public static boolean within(Polygon.SimplePolygon polygon, Point point) {
        if (CRSChecker.check(polygon, point) == CRS.Cartesian) {
            return org.neo4j.spatial.algo.cartesian.Within.within(polygon, point);
        } else {
            return org.neo4j.spatial.algo.wgs84.Within.within(polygon, point);
        }
    }
}
