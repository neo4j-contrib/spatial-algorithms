package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.PolygonUtil;

import java.util.*;

import static java.lang.String.format;

public class Neo4jSimplePolygon implements Polygon.SimplePolygon {
    private final org.neo4j.graphdb.spatial.Point[] points;

    public Neo4jSimplePolygon(Node node, String property) {
        org.neo4j.graphdb.spatial.Point[] unclosed = (org.neo4j.graphdb.spatial.Point[]) node.getProperty(property);
        this.points = new PolygonUtil<org.neo4j.graphdb.spatial.Point>().closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    public Neo4jSimplePolygon(Node main, String property, String relationStart, String relationNext) {
        org.neo4j.graphdb.spatial.Point[] unclosed = traverseGraph(main, property, relationStart, relationNext);
        this.points = new PolygonUtil<org.neo4j.graphdb.spatial.Point>().closeRing(unclosed);
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
    public String toString() {
        return format("Neo4jSimplePolygon%s", Arrays.toString(points));
    }

    @Override
    public String toWKT() {
        StringJoiner viewer = new StringJoiner(",", "POLYGON((", "))");
        for (Point point : getPoints()) {
            viewer.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
        }
        return viewer.toString();
    }

    /**
     * Traverses through the graph starting at the main node
     *
     * @param main Starting node, which is not part of the polygon
     * @param property Name of the property containing the Point object
     * @param relationStart The name of the relation from the starting node to the first node of the polygon
     * @param relationNext The name of the relation between nodes of the polygon
     * @return An array containing the points of the polygon in order
     */
    private static org.neo4j.graphdb.spatial.Point[] traverseGraph(Node main, String property, String relationStart, String relationNext) {
        List<org.neo4j.graphdb.spatial.Point> resultList = new LinkedList<>();

        Node current = getRelationNode(main, relationStart);

        while (current != null) {
            resultList.add((org.neo4j.graphdb.spatial.Point) current.getProperty(property));

            current = getRelationNode(current, relationNext);
        }


        return resultList.toArray(new org.neo4j.graphdb.spatial.Point[0]);
    }


    /**
     * Returns the node on the other side of the outgoing relation if and only if the source node has the outgoing relation. Otherwise, it returns null.
     *
     * @param node The source node
     * @param relation The name of the relation
     * @return Node with incoming relation from the source, or null if no such node exists
     */
    private static Node getRelationNode(Node node, String relation) {
        if (!node.hasRelationship(RelationshipType.withName(relation), Direction.OUTGOING)) {
            return null;
        }

        return node.getSingleRelationship(RelationshipType.withName(relation), Direction.OUTGOING).getEndNode();
    }

    private static void assertAllSameDimension(org.neo4j.graphdb.spatial.Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].getCoordinate().getCoordinate().size() != points[i].getCoordinate().getCoordinate().size()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].getCoordinate().getCoordinate().size(), points[0].getCoordinate().getCoordinate().size()));
            }
        }
    }
}
