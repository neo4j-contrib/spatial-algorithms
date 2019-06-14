package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.ArrayUtil;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.spatial.algo.CCWCalculator;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.PolygonUtil;

public class Neo4jSimpleGraphIDPolygon extends Neo4jSimpleGraphPolygon {
    private KernelTransaction ktx;

    public Neo4jSimpleGraphIDPolygon(Node main, long osmRelationId, KernelTransaction ktx) {
        super(main, osmRelationId);
        this.ktx = ktx;
    }

    @Override
    public Point[] getPoints() {
        Node[] wayNodes = traverseWholePolygon(main);
        Point[] unclosed = extractPoints(wayNodes);
        Point[] points = PolygonUtil.closeRing(unclosed);

        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        if (!CCWCalculator.isCCW(points)) {
            ArrayUtil.reverse(points);
        }
        return points;
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
        pointer = getNextNode(pointer);
        return extractPoint(pointer);
    }
}
