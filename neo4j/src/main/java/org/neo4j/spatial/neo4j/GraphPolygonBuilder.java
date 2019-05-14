package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.spatial.core.Polygon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphPolygonBuilder {
    private Node main;
    private List<List<Node>> polystrings;

    private RelationshipType nextRel;
    private RelationshipType nextInPolygonRel;

    private GraphDatabaseService db;

    private static RelationshipType POLYGON_STRUCTURE_REL = RelationshipType.withName("POLYGON_STRUCTURE");
    private static RelationshipType POLYGON_START_REL = RelationshipType.withName("POLYGON_START");
    private static Label POLYGON_LABEL = Label.label("POLYGON");
    private static Label SHELL_LABEL = Label.label("SHELL");
    private static Label HOLE_LABEL = Label.label("HOLE");

    public GraphPolygonBuilder(Node main, List<List<Node>> polystrings) {
        this.main = main;
        this.polystrings = polystrings;

        this.db = main.getGraphDatabase();
    }

    public void build() {
        connectPolystrings();
        ForestRoot root = buildPolygonTree();

        for (TreeNode child : root.getChildren()) {
            buildGraphPolygon(main, child, 0);
        }
    }

    private void connectPolystrings() {
        long relationOsmId = (long) main.getProperty("relation_osm_id");
        nextRel = RelationshipType.withName("NEXT");
        nextInPolygonRel = RelationshipType.withName("NEXT_IN_POLYGON_" + relationOsmId);
        for (List<Node> polystring : polystrings) {

            pairwise:
            for (int i = 0; i < polystring.size() - 1; i++) {
                Node a = polystring.get(i);
                Node b = polystring.get(i+1);

                for (Relationship relationship : a.getRelationships(nextInPolygonRel, Direction.OUTGOING)) {
                    if (relationship.isType(nextInPolygonRel)) {
                        continue pairwise;
                    }
                }

                //Mutually exclusive execution branches

                boolean nextFound = false;
                for (Relationship relationship : a.getRelationships(nextRel)) {
                    long otherId = relationship.getOtherNodeId(a.getId());
                    if (otherId == b.getId()) {
                        nextFound = true;
                        break;
                    }
                }

                if (!nextFound) {
                    a.createRelationshipTo(b, nextInPolygonRel);
                }
            }
        }
    }

    private ForestRoot buildPolygonTree() {
        ForestRoot root = new ForestRoot();

        Polygon.SimplePolygon[] polygons = new Polygon.SimplePolygon[polystrings.size()];

        for (int i = 0; i < polystrings.size(); i++) {
            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphPolygon(polystrings.get(i).get(0), "location", new RelationshipType[]{nextRel, nextInPolygonRel});
            polygons[i] = polygon;
        }

        for (int i = 0; i < polygons.length; i++) {
            root.addPolygon(new TreeNode(polygons[i], getWay(polystrings.get(i))));
        }

        return root;
    }

    private void buildGraphPolygon(Node previous, TreeNode root, int depth) {
        Label label = depth % 2 == 0 ? SHELL_LABEL : HOLE_LABEL;
        Node polygonNode = db.createNode(POLYGON_LABEL, label);

        previous.createRelationshipTo(polygonNode, POLYGON_STRUCTURE_REL);
        polygonNode.createRelationshipTo(root.getStartWay(), POLYGON_START_REL);

        for (TreeNode child : root.getChildren()) {
            buildGraphPolygon(polygonNode, child, depth+1);
        }
    }

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
