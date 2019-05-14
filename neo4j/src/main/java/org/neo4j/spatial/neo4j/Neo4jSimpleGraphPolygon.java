package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.PolygonUtil;

import javax.management.relation.Relation;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    private final Neo4jPoint[] points;

    public Neo4jSimpleGraphPolygon(Node main, String property, RelationshipType relationStart, RelationshipCombination[] relationNext) {
        Neo4jPoint[] unclosed = traverseGraph(main, property, relationStart, relationNext);
        this.points = new PolygonUtil<Neo4jPoint>().closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    public Neo4jSimpleGraphPolygon(Node main, String property, RelationshipCombination[] relationNext) {
        Neo4jPoint[] unclosed = traverseGraph(main, property, null, relationNext);
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
    public boolean isSimple() {
        return true;
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
     * @param relationNext The names of the relations between nodes of the polygon
     * @return An array containing the points of the polygon in order
     */
    private Neo4jPoint[] traverseGraph(Node main, String property, RelationshipType relationStart, RelationshipCombination[] relationNext) {
        RelationshipType nodeRel = RelationshipType.withName("NODE");

        Node start = main;
        if (relationStart != null) {
            start = main.getSingleRelationship(relationStart, Direction.OUTGOING).getEndNode();
        }

        TraversalDescription traversalDescription = new MonoDirectionalTraversalDescription().breadthFirst()
                .relationships(nodeRel, Direction.OUTGOING)
                .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(nodeRel));

        for (RelationshipCombination combination : relationNext) {
            traversalDescription = traversalDescription.relationships(combination.getType(), combination.getDirection());
        }

        return traversalDescription.traverse(start).nodes().stream().map(n -> new Neo4jPoint(n, property)).toArray(Neo4jPoint[]::new);
    }

    private void assertAllSameDimension(Neo4jPoint... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }
}
