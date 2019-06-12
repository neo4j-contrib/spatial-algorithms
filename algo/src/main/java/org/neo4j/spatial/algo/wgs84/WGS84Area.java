package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

public class WGS84Area extends Area {
    /**
     * Computes the area of the polygon according to Girard’s theorem
     * @param polygon
     * @return the area of the polygon
     */
    @Override
    public double area(Polygon.SimplePolygon polygon) {
        Point[] points = polygon.getPoints();
        int n = points.length - 1;
        Vector[] greatCircles = new Vector[n];

        for (int i = 0; i < n; i++) {
            Vector u = new Vector(points[i]);
            Vector v = new Vector(points[i+1]);
            greatCircles[i] = u.cross(v);
        }

        Vector n1 = new Vector(points[0]);
        double sumAngles = 0;

        for (int i = 0; i < n; i++) {
            sumAngles += WGSUtil.angleTo(greatCircles[i], n1, greatCircles[(i + 1) % n]);
        }

        double sumTheta = n * Math.PI - Math.abs(sumAngles);
        double sphericalExcess = sumTheta - (n-2) * Math.PI;
        return sphericalExcess * WGSUtil.RADIUS *WGSUtil.RADIUS;
    }
}
