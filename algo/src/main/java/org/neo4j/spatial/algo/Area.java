package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;

public abstract class Area {
    /**
     * @param polygon
     * @return The area of the polygon
     */
    public double area(MultiPolygon polygon) {
        double area = 0;

        for (MultiPolygon.MultiPolygonNode child : polygon.getChildren()) {
            area += area(child);
        }

        return area;
    }

    private double area(MultiPolygon.MultiPolygonNode node) {
        double area = area(node.getPolygon());

        for (MultiPolygon.MultiPolygonNode child : node.getChildren()) {
            area -= area(child);
        }

        return area;
    }

    /**
     * @param polygon
     * @return The area of the simple polygon
     */
    public abstract double area(Polygon.SimplePolygon polygon);
}