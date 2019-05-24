package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphPolygonBuilder {
    private static Label POLYGON_LABEL = Label.label("Polygon");
    private static Label SHELL_LABEL = Label.label("Shell");
    private static Label HOLE_LABEL = Label.label("Hole");

    private GraphDatabaseService db;

    private Node main;
    private List<List<Node>> polystrings;

    public GraphPolygonBuilder(Node main, List<List<Node>> polystrings) {
        this.main = main;
        this.polystrings = polystrings;

        this.db = main.getGraphDatabase();
    }

    public void build() {
        connectPolystrings();
        MultiPolygon root = buildMultiPolygon();

        for (MultiPolygon.MultiPolygonNode child : root.getChildren()) {
            buildGraphPolygon(main, (Neo4jMultiPolygonNode) child);
        }
    }

    /**
     * Connect unconnected way nodes of a polygon via a special relation relating to the OSMRelation
     */
    private void connectPolystrings() {
        long relationOsmId = (long) main.getProperty("relation_osm_id");

        for (List<Node> polystring : polystrings) {

            pairwise:
            for (int i = 0; i < polystring.size(); i++) {
                Node a = polystring.get(i);
                Node b = polystring.get((i + 1) % polystring.size());

                for (Relationship relationship : a.getRelationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)) {
                    if (b.getId() != relationship.getOtherNodeId(a.getId())) {
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

                Relationship relation = a.createRelationshipTo(b, Relation.NEXT_IN_POLYGON);
                relation.setProperty("relation_osm_ids", new long[]{relationOsmId});
            }
        }
    }

    /**
     * Build a multipolygon from the closed polystrings
     *
     * @return The root of the multipolygon
     */
    private MultiPolygon buildMultiPolygon() {

        long relationOsmId = (long) main.getProperty("relation_osm_id");
        MultiPolygon root = new MultiPolygon();

        Polygon.SimplePolygon[] polygons = new Polygon.SimplePolygon[polystrings.size()];

        for (int i = 0; i < polystrings.size(); i++) {
            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphNodePolygon(polystrings.get(i).get(0), relationOsmId);
            polygons[i] = polygon;
        }

        for (int i = 0; i < polygons.length; i++) {
            root.insertMultiPolygonNode(new Neo4jMultiPolygonNode(polygons[i], getWay(polystrings.get(i))));
        }

        return root;
    }

    /**
     * Build the multipolygon tree in Neo4j.
     *
     * @param parent The parent node
     * @param node   The current polygon node
     */
    private void buildGraphPolygon(Node parent, Neo4jMultiPolygonNode node) {
        Label label = node.getType() == MultiPolygon.PolygonType.SHELL ? SHELL_LABEL : HOLE_LABEL;
        Node polygonNode = db.createNode(POLYGON_LABEL, label);

        parent.createRelationshipTo(polygonNode, Relation.POLYGON_STRUCTURE);
        polygonNode.createRelationshipTo(node.getStartWay(), Relation.POLYGON_START);

        for (MultiPolygon.MultiPolygonNode child : node.getChildren()) {
            buildGraphPolygon(polygonNode, (Neo4jMultiPolygonNode) child);
        }
    }

    /**
     * @param polystring
     * @return The OSMWay node belonging to the polystring
     */
    private Node getWay(List<Node> polystring) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("n", polystring.get(0).getId());
        parameters.put("m", main.getId());

        Result result = db.execute("MATCH (n:OSMWayNode)<-[:NEXT*0..]-(:OSMWayNode)<-[:FIRST_NODE]-(w:OSMWay)<-[:MEMBER]-(m:OSMRelation) WHERE id(n)=$n AND id(m)=$m RETURN w", parameters);

        if (result.hasNext()) {
            return (Node) result.next().get("w");
        }

        return null;
    }
}
