package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;

import java.util.List;

public class GraphPolylineBuilder extends GraphBuilder {
    private static Label POLYLINE_LABEL = Label.label("Polyline");

    public GraphPolylineBuilder(Transaction tx, Node main, List<List<Node>> polylines) {
        super(tx, main, polylines);
    }

    public void build() {
        // TODO: Figure out why the polyline builder needs this offset=1
        connectPolylines(Relation.NEXT_IN_POLYLINE, 1);
        connectToMain();
    }

    private void connectToMain() {
        for (List<Node> polyline : polylines) {
            Node polylineNode = tx.createNode(POLYLINE_LABEL);

            main.createRelationshipTo(polylineNode, Relation.POLYLINE_STRUCTURE);
            polylineNode.createRelationshipTo(polyline.get(0), Relation.POLYLINE_START);
        }
    }
}
