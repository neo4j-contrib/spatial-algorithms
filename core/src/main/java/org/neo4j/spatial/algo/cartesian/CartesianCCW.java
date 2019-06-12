package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.CCW;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class CartesianCCW implements CCW {
    @Override
    public boolean isCCW(Polygon.SimplePolygon polygon) {
        return shoelace(polygon) < 0;
    }

    /**
     * @return Twice the area of the polygon using the shoelace algorithm
     */
    public static double shoelace(Polygon.SimplePolygon polygon) {
        Point[] points = polygon.getPoints();
        double sum = 0;

        for (int i = 0; i < points.length-1; i++) {
            double[] a = points[i].getCoordinate();
            double[] b = points[i + 1].getCoordinate();

            sum += (b[0] - a[0]) * (b[1] + a[0]);
        }
        return sum;
    }
}
