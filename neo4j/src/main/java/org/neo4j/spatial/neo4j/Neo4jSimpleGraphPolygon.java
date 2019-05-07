package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.PolygonUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import static java.lang.String.format;

public class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    private final Neo4jPoint[] points;

    public Neo4jSimpleGraphPolygon(Node main, String property, String relationStart, String relationNext) {
        Neo4jPoint[] unclosed = traverseGraph(main, property, relationStart, relationNext);
        this.points = new PolygonUtil<Neo4jPoint>().closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    @Override
    public int dimension() {
        return this.points[0].dimension();
    }

    @Override
    public Point[] getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleGraphPolygon%s", Arrays.toString(points));
    }

    @Override
    public String toWKT() {
        StringJoiner viewer = new StringJoiner(",", "POLYGON((", "))");
        for (Point point : getPoints()) {
            viewer.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
        }
        return viewer.toString();
    }

    public CRS getCRS() {
        return this.points[0].getCRS();
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
    private static Neo4jPoint[] traverseGraph(Node main, String property, String relationStart, String relationNext) {
        List<Neo4jPoint> resultList = new LinkedList<>();
        RelationshipType startRel = RelationshipType.withName(relationStart);
        RelationshipType nextRel = RelationshipType.withName(relationNext);
        Node start = main.getSingleRelationship(startRel, Direction.OUTGOING).getEndNode();

        Traverser t = new MonoDirectionalTraversalDescription().depthFirst().relationships(nextRel, Direction.OUTGOING).traverse(start);
        for (Node n : t.nodes()) {
            resultList.add(new Neo4jPoint(n, property));
        }


        return resultList.toArray(new Neo4jPoint[0]);
    }

    private static void assertAllSameDimension(Neo4jPoint... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }
}
