package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.Area;
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
        polygon.startTraversal();
        Vector currentGC;
        Vector prev = new Vector(polygon.getNextPoint());
        Vector current = new Vector(polygon.getNextPoint());
        Vector previousGC = prev.cross(current);

        prev = current;

        Vector firstGC = previousGC;
        Vector normal = prev;
        double sumAngles = 0;

        int n = 0;

        while (!polygon.fullyTraversed()) {
            current = new Vector(polygon.getNextPoint());

            if (prev.equals(current)) {
                continue;
            }

            currentGC = prev.cross(current);

            sumAngles += WGSUtil.angleTo(previousGC, normal, currentGC);

            prev = current;
            previousGC = currentGC;
            n++;
        }

        sumAngles += WGSUtil.angleTo(previousGC, normal, firstGC);

        double sumTheta = n * Math.PI - Math.abs(sumAngles);
        double sphericalExcess = sumTheta - ((n-2) * Math.PI);
        return sphericalExcess * WGSUtil.RADIUS * WGSUtil.RADIUS;
    }
}
