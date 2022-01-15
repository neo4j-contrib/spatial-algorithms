package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphPolygonBuilder extends GraphBuilder {
    private static Label POLYGON_LABEL = Label.label("Polygon");
    private static Label SHELL_LABEL = Label.label("Shell");
    private static Label HOLE_LABEL = Label.label("Hole");

    public GraphPolygonBuilder(Transaction tx, Node main, List<List<Node>> polylines) {
        super(tx, main, polylines);
    }

    public void build() {
        connectPolylines(Relation.NEXT_IN_POLYGON, 0);
        MultiPolygon root = buildMultiPolygon();

        for (MultiPolygon.MultiPolygonNode child : root.getChildren()) {
            buildGraphPolygon(main, (Neo4jMultiPolygonNode) child);
        }
    }

    /**
     * Build a multipolygon from the closed polylines
     *
     * @return The root of the multipolygon
     */
    private MultiPolygon buildMultiPolygon() {

        long relationOsmId = (long) main.getProperty("relation_osm_id");
        MultiPolygon root = new MultiPolygon();

        Polygon.SimplePolygon[] polygons = new Polygon.SimplePolygon[polylines.size()];

        for (int i = 0; i < polylines.size(); i++) {
            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphNodePolygon(polylines.get(i).get(0), relationOsmId);
            polygons[i] = polygon;
        }

        for (int i = 0; i < polygons.length; i++) {
            root.insertMultiPolygonNode(new Neo4jMultiPolygonNode(polygons[i], getWay(polylines.get(i))));
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
        Node polygonNode = tx.createNode(POLYGON_LABEL, label);

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

        Result result = tx.execute("MATCH (n:OSMWayNode)<-[:NEXT*0..]-(:OSMWayNode)<-[:FIRST_NODE]-(w:OSMWay)<-[:MEMBER*]-(m:OSMRelation) WHERE id(n)=$n AND id(m)=$m RETURN w", parameters);

        if (result.hasNext()) {
            return (Node) result.next().get("w");
        }
        System.out.println("Failed to find a way node between relation " + main + " and OSMWayNode " + polystring.get(0));

        return null;
    }
}
