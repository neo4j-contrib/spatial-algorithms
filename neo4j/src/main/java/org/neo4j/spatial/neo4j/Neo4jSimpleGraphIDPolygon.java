package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.PolygonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Neo4jSimpleGraphIDPolygon extends Neo4jSimpleGraphPolygon {

    public Neo4jSimpleGraphIDPolygon(Node main, long osmRelationId, KernelTransaction ktx) {
        Node[] wayNodes = traverseGraph(main, osmRelationId);
        Point[] unclosed = extractPoints(wayNodes, ktx);
        this.points = PolygonUtil.closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    Point[] extractPoints(Node[] wayNodes, KernelTransaction ktx) {
        Neo4jIDPoint[] points = new Neo4jIDPoint[wayNodes.length];
        for (int i = 0; i < points.length; i++) {
            Node node = wayNodes[i].getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();
            points[i] = new Neo4jIDPoint(node.getId(), ktx);
        }
        return points;
    }
}
