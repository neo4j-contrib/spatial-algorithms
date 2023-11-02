package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.values.storable.CoordinateReferenceSystem;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class Neo4jPoint implements Point {
    private final Node node;
    private final static String property = "location";

    public Neo4jPoint(Node node) {
        this.node = node;
    }

    @Override
    public boolean equals(Point other) {
        return Arrays.equals(this.getCoordinate(), other.getCoordinate());
    }

    public boolean equals(Object other) {
        return other instanceof Point point && this.equals(point);
    }

    @Override
    public double[] getCoordinate() {
        org.neo4j.graphdb.spatial.Point location = (org.neo4j.graphdb.spatial.Point) this.node.getProperty(property);
        return location.getCoordinate().getCoordinate().clone();
    }

    public CRS getCRS() {
        org.neo4j.graphdb.spatial.CRS neo4jCRS = getNeo4jCRS();
        return CRSConverter.toInMemoryCRS(neo4jCRS);
    }

    public org.neo4j.graphdb.spatial.CRS getNeo4jCRS() {
        return ((org.neo4j.graphdb.spatial.Point) this.node.getProperty(property)).getCRS();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getCoordinate());
    }

    public String toString() {
        return format("Neo4jPoint%s", Arrays.toString(getCoordinate()));
    }
}
