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
    private KernelTransaction ktx;

    public Neo4jSimpleGraphIDPolygon(Node main, long osmRelationId, KernelTransaction ktx) {
        super(main, osmRelationId);
        Node[] wayNodes = traverseGraph(main);
        Point[] unclosed = extractPoints(wayNodes);
        this.points = PolygonUtil.closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
        this.ktx = ktx;
    }

    private Point[] extractPoints(Node[] wayNodes) {
        Point[] points = new Point[wayNodes.length];
        for (int i = 0; i < points.length; i++) {
            points[i] = extractPoint(wayNodes[i]);
        }
        return points;
    }

    @Override
    Point extractPoint(Node wayNode) {
        Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();
        return new Neo4jIDPoint(node.getId(), ktx);
    }

    @Override
    public Point getNextPoint() {
        super.traversing = true;
        Point point = extractPoint(pointer);
        pointer = getNextNode(pointer);
        return point;
    }
}
