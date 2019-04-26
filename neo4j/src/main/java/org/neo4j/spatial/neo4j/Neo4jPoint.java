package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.Point;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class Neo4jPoint implements Point {
    private final org.neo4j.graphdb.spatial.Point point;

    public Neo4jPoint(Node node, String property) {
        this.point = (org.neo4j.graphdb.spatial.Point) node.getProperty(property);
    }

    public boolean equals(Point other) {
        return Arrays.equals(this.getCoordinate(), other.getCoordinate());
    }

    public boolean equals(Object other) {
        return other instanceof Point && this.equals((Point) other);
    }

    @Override
    public double[] getCoordinate() {
        List<Double> coordinateList = this.point.getCoordinate().getCoordinate();

        return coordinateList.stream().mapToDouble(d -> d).toArray();
    }

    public String toString() {
        return format("Neo4jPoint%s", Arrays.toString(getCoordinate()));
    }
}