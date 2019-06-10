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

    public boolean equals(Point other) {
        return Arrays.equals(this.getCoordinate(), other.getCoordinate());
    }

    public boolean equals(Object other) {
        return other instanceof Point && this.equals((Point) other);
    }

    @Override
    public double[] getCoordinate() {
        org.neo4j.graphdb.spatial.Point location = (org.neo4j.graphdb.spatial.Point) this.node.getProperty(property);
        List<Double> coordinateList = location.getCoordinate().getCoordinate();

        return coordinateList.stream().mapToDouble(d -> d).toArray();
    }

    public CRS getCRS() {
        org.neo4j.graphdb.spatial.CRS neo4jCRS = getNeo4jCRS();
        return CRSConverter.toInMemoryCRS(neo4jCRS);
    }

    public org.neo4j.graphdb.spatial.CRS getNeo4jCRS() {
        return ((org.neo4j.graphdb.spatial.Point) this.node.getProperty(property)).getCRS();
    }

    public String toString() {
        return format("Neo4jPoint%s", Arrays.toString(getCoordinate()));
    }
}