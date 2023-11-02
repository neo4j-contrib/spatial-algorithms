package org.neo4j.spatial.neo4j;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.graphdb.*;

import java.util.List;
import java.util.Objects;

public abstract class GraphBuilder {
    protected Node main;
    protected List<List<Node>> polylines;
    protected Transaction tx;

    public GraphBuilder(Transaction tx, Node main, List<List<Node>> polylines) {
        this.tx = tx;
        this.main = main;
        this.polylines = polylines;
    }

    abstract void build();

    /**
     * Connect unconnected way nodes of a polygon/polyline via a special relation relating to the OSMRelation
     */
    protected void connectPolylines(RelationshipType nextPolyRelType, int offset) {
        long relationOsmId = (long) main.getProperty("relation_osm_id");

        for (List<Node> polyline : polylines) {

            for (int i = offset; i < polyline.size() - offset; i++) {
                Node a = polyline.get(i);
                Node b = polyline.get((i + 1) % polyline.size());

                if (Objects.equals(a.getElementId(), b.getElementId())) {
                    continue;
                }

                // If we already have a nextPolyRel relationship to the other node, update it
                Relationship nextPolyRel = findPolyRel(a, b, nextPolyRelType);
                if (nextPolyRel != null) {

                    // If the relationship does not contain this relation ID, add it
                    long[] ids = (long[]) nextPolyRel.getProperty("relation_osm_ids");
                    if (!ArrayUtils.contains(ids, relationOsmId)) {
                        long[] idsModified = new long[ids.length + 1];
                        System.arraycopy(ids, 0, idsModified, 0, ids.length);
                        idsModified[idsModified.length - 1] = relationOsmId;

                        nextPolyRel.setProperty("relation_osm_ids", idsModified);
                    }
                    break;
                }

                // If we have no NEXT relationship, make a NEXT_IN_POLYGON relationship instead
                Relationship relationship = findNextRelationship(a, b);
                if (relationship == null) {
                    Relationship relation = a.createRelationshipTo(b, nextPolyRelType);
                    relation.setProperty("relation_osm_ids", new long[]{relationOsmId});
                }
            }
        }
    }

    private Relationship findPolyRel(Node from, Node to, RelationshipType relType) {
        Relationship found = null;
        for (Relationship relationship : from.getRelationships(Direction.OUTGOING, relType)) {
            if (to == relationship.getOtherNode(from)) {
                found = relationship;
            }
        }
        return found;
    }

    private Relationship findNextRelationship(Node from, Node to) {
        Relationship found = null;
        for (Relationship relationship : from.getRelationships(Relation.NEXT)) {
            if (to == relationship.getOtherNode(from)) {
                found = relationship;
            }
        }
        return found;
    }
}
