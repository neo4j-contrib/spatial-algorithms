package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.core.Polygon;

public class Area extends org.neo4j.spatial.algo.Area {
    @Override
    public double area(Polygon.SimplePolygon polygon) {
        return Math.abs(CCW.shoelace(polygon)/2);
    }
}
