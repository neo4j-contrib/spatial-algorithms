package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;

public class Neo4jMultiPolygonNode extends MultiPolygon.MultiPolygonNode {
    private Node startWay;

    public Neo4jMultiPolygonNode(Polygon.SimplePolygon polygon, Node startWay) {
        super(polygon);
        this.startWay = startWay;
    }

    public Node getStartWay() {
        return startWay;
    }

    @Override
    public String toString() {
        return "Neo4jMultiPolygonNode{" + super.getPolygon() + '}';
    }
}