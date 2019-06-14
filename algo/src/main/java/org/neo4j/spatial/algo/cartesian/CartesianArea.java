package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.core.Polygon;

public class CartesianArea extends Area {
    @Override
    public double area(Polygon.SimplePolygon polygon) {
        return Math.abs(CartesianCCW.shoelace(polygon.getPoints())/2);
    }
}
