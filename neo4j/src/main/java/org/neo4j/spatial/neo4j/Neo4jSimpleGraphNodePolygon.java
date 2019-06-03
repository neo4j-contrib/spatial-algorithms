package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.PolygonUtil;

public class Neo4jSimpleGraphNodePolygon extends Neo4jSimpleGraphPolygon {

    public Neo4jSimpleGraphNodePolygon(Node main, long osmRelationId) {
        super(main, osmRelationId);
        Node[] wayNodes = traverseGraph(main);
        Point[] unclosed = extractPoints(wayNodes);
        this.points = PolygonUtil.closeRing(unclosed);

        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
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
        return new Neo4jPoint(node);
    }

    @Override
    public Point getNextPoint() {
        super.traversing = true;
        Point point = extractPoint(pointer);
        pointer = getNextNode(pointer);
        return point;
    }
}
