package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.PolygonUtil;

public class Neo4jSimpleGraphNodePolygon extends Neo4jSimpleGraphPolygon {

    public Neo4jSimpleGraphNodePolygon(Node main, long osmRelationId) {
        Node[] wayNodes = traverseGraph(main, osmRelationId);
        Point[] unclosed = extractPoints(wayNodes);
        this.points = PolygonUtil.closeRing(unclosed);

        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
    }

    private Point[] extractPoints(Node[] wayNodes) {
        Neo4jPoint[] points = new Neo4jPoint[wayNodes.length];
        for (int i = 0; i < points.length; i++) {
            Node node = wayNodes[i].getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();
            points[i] = new Neo4jPoint(node);
        }
        return points;
    }
}
