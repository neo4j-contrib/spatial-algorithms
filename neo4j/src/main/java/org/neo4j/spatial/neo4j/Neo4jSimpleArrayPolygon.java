package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;

import static java.lang.String.format;

public class Neo4jSimpleArrayPolygon implements Polygon.SimplePolygon {
    private final org.neo4j.graphdb.spatial.Point[] points;

    public Neo4jSimpleArrayPolygon(Node node, String property) {
        org.neo4j.graphdb.spatial.Point[] unclosed = (org.neo4j.graphdb.spatial.Point[]) node.getProperty(property);
        //TODO Maybe save as InMemoryPoint?
        this.points = closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    @Override
    public int dimension() {
        return this.points[0].getCoordinate().getCoordinate().size();
    }

    @Override
    public Point[] getPoints() {
        Point[] result = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            result[i] = Point.point(points[i].getCoordinate().getCoordinate().stream().mapToDouble(d -> d).toArray());
        }
        return result;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleArrayPolygon%s", Arrays.toString(points));
    }

    private static void assertAllSameDimension(org.neo4j.graphdb.spatial.Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].getCoordinate().getCoordinate().size() != points[i].getCoordinate().getCoordinate().size()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].getCoordinate().getCoordinate().size(), points[0].getCoordinate().getCoordinate().size()));
            }
        }
    }

    public org.neo4j.graphdb.spatial.Point[] closeRing(org.neo4j.graphdb.spatial.Point... points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("Cannot close ring of less than 2 points");
        }
        org.neo4j.graphdb.spatial.Point first = points[0];
        org.neo4j.graphdb.spatial.Point last = points[points.length - 1];
        if (first.equals(last)) {
            return points;
        } else {
            org.neo4j.graphdb.spatial.Point[] closed = Arrays.copyOf(points, points.length + 1);
            closed[points.length] = points[0];
            return closed;
        }
    }
}
