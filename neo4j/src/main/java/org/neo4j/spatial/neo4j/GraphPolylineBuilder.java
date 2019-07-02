package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;

public class GraphPolylineBuilder extends GraphBuilder {
    private static Label POLYLINE_LABEL = Label.label("Polyline");

    public GraphPolylineBuilder(Node main, List<List<Node>> polylines) {
        super(main, polylines);
    }

    public void build() {
        connectPolylines();
        connectToMain();
    }

    /**
     * Connect unconnected way nodes of a polyline via a special relation relating to the OSMRelation
     */
    private void connectPolylines() {
        long relationOsmId = (long) main.getProperty("relation_osm_id");

        for (List<Node> polyline : polylines) {

            pairwise:
            for (int i = 1; i < polyline.size() - 2; i++) {
                Node a = polyline.get(i);
                Node b = polyline.get(i + 1);

                for (Relationship relationship : a.getRelationships(Relation.NEXT_IN_POLYLINE, Direction.OUTGOING)) {
                    if (b.getId() != relationship.getEndNodeId()) {
                        continue;
                    }

                    long[] ids = (long[]) relationship.getProperty("relation_osm_ids");

                    for (long id : ids) {
                        if (id == relationOsmId) {
                            continue pairwise;
                        }
                    }
                    long[] idsModified = new long[ids.length + 1];
                    for (int j = 0; j < ids.length; j++) {
                        idsModified[j] = ids[j];
                    }
                    idsModified[idsModified.length - 1] = relationOsmId;

                    relationship.setProperty("relation_osm_ids", idsModified);
                    continue pairwise;
                }

                for (Relationship relationship : a.getRelationships(Relation.NEXT)) {
                    long otherId = relationship.getOtherNodeId(a.getId());
                    if (otherId == b.getId()) {
                        continue pairwise;
                    }
                }

                Relationship relation = a.createRelationshipTo(b, Relation.NEXT_IN_POLYLINE);
                relation.setProperty("relation_osm_ids", new long[]{relationOsmId});
            }
        }
    }

    private void connectToMain() {
        for (List<Node> polyline : polylines) {
            Node polylineNode = db.createNode(POLYLINE_LABEL);

            main.createRelationshipTo(polylineNode, Relation.POLYLINE_STRUCTURE);
            polylineNode.createRelationshipTo(polyline.get(0), Relation.POLYLINE_START);
        }
    }
}
