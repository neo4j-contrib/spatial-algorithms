package org.neo4j.spatial.neo4j;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.Values;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class UserDefinedFunctionsTest {
    private GraphDatabaseService db;

    @Before
    public void setUp() throws KernelException {
        db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().setConfig(GraphDatabaseSettings.procedure_unrestricted, "neo4j.*").newGraphDatabase();
        registerUDFClass(db, UserDefinedFunctions.class);
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    @Test
    public void shouldCreatePolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 1, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 1));
        testCall(db, "RETURN spatial.polygon($points)", map("points", points), result -> {
            assertThat("Should have one polygon", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get list of points", record, instanceOf(List.class));
            List<Point> polygon = (List<Point>) record;
            assertThat("Should have 4 points", polygon.size(), equalTo(4));
            assertThat("Should be closed polygon", polygon.get(0), equalTo(polygon.get(polygon.size() - 1)));
        });
    }

    @Test
    public void shouldCreateOSMGraphPolygonOneDirectionOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            main.setProperty("relation_osm_id", 1L);

            int x = 4;
            int y = 3;

            Node[] ways = new Node[x];
            Node[][] wayNodes = new Node[x][y];
            Node[][] nodes = new Node[x][y];
            Node[] connectors = new Node[x];

            Relationship rel;

            for (int i = 0; i < x; i++) {
                ways[i] = db.createNode(Label.label("OSMWay"));
                main.createRelationshipTo(ways[i], Relation.MEMBER);

                for (int j = 0; j < y; j++) {
                    wayNodes[i][j] = db.createNode(Label.label("OSMWayNode"));

                    if (j == 0) {
                        ways[i].createRelationshipTo(wayNodes[i][j], Relation.FIRST_NODE);
                    } else {
                        rel = wayNodes[i][j - 1].createRelationshipTo(wayNodes[i][j], Relation.NEXT);
                        rel.setProperty("relation_osm_id", 1L);
                    }
                }

                for (int j = 0; j < y; j++) {
                    nodes[i][j] = db.createNode(Label.label("OSMNode"));
                    nodes[i][j].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, i, j));

                    wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
                }
            }

            for (int i = 0; i < x; i++) {
                connectors[i] = db.createNode(Label.label("OSMWayNode"));
            }

            rel = wayNodes[0][y - 1].createRelationshipTo(connectors[0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1L);
            connectors[0].createRelationshipTo(nodes[1][0], Relation.NODE);

            rel = wayNodes[1][y - 1].createRelationshipTo(connectors[1], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1L);
            connectors[1].createRelationshipTo(nodes[2][0], Relation.NODE);

            rel = wayNodes[2][y - 1].createRelationshipTo(connectors[2], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1L);
            connectors[2].createRelationshipTo(nodes[3][0], Relation.NODE);

            rel = wayNodes[3][y - 1].createRelationshipTo(connectors[3], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1L);
            connectors[3].createRelationshipTo(nodes[0][0], Relation.NODE);

            db.execute("CALL spatial.osm.graph.createPolygon($main)", map("main", main));
            Result result = db.execute("MATCH (m)-[:POLYGON_STRUCTURE]->(a:Shell)-[:POLYGON_START]->() WHERE id(m) = $mainId RETURN a", map("mainId", main.getId()));

            assertThat(result.hasNext(), equalTo(true));

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMGraphPolygonRandomDirectionOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            main.setProperty("relation_osm_id", 1l);

            int x = 4;
            int y = 3;

            Node[] ways = new Node[x];
            Node[][] wayNodes = new Node[x][y];
            Node[][] nodes = new Node[x][y];
            Node[] connectors = new Node[x];

            Relationship rel;

            for (int i = 0; i < x; i++) {
                ways[i] = db.createNode(Label.label("OSMWay"));
                main.createRelationshipTo(ways[i], Relation.MEMBER);

                for (int j = 0; j < y; j++) {
                    wayNodes[i][j] = db.createNode(Label.label("OSMWayNode"));

                    if (j > 0) {
                        rel = wayNodes[i][j - 1].createRelationshipTo(wayNodes[i][j], Relation.NEXT);
                        rel.setProperty("relation_osm_id", 1l);
                    }
                }

                for (int j = 0; j < y; j++) {
                    nodes[i][j] = db.createNode(Label.label("OSMNode"));
                    nodes[i][j].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, i, j));

                    wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
                }
            }

            for (int i = 0; i < x; i++) {
                connectors[i] = db.createNode(Label.label("OSMWayNode"));
            }

            ways[0].createRelationshipTo(wayNodes[0][0], Relation.FIRST_NODE);
            rel = wayNodes[0][y - 1].createRelationshipTo(connectors[0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[0].createRelationshipTo(nodes[1][y - 1], Relation.NODE);

            ways[1].createRelationshipTo(connectors[1], Relation.FIRST_NODE);
            rel = connectors[1].createRelationshipTo(wayNodes[1][0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[1].createRelationshipTo(nodes[2][0], Relation.NODE);

            ways[2].createRelationshipTo(wayNodes[2][0], Relation.FIRST_NODE);
            rel = wayNodes[2][y - 1].createRelationshipTo(connectors[2], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[2].createRelationshipTo(nodes[3][y - 1], Relation.NODE);

            ways[3].createRelationshipTo(connectors[3], Relation.FIRST_NODE);
            rel = connectors[3].createRelationshipTo(wayNodes[3][0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[3].createRelationshipTo(nodes[0][0], Relation.NODE);

            db.execute("CALL spatial.osm.graph.createPolygon($main)", map("main", main));
            Result result = db.execute("MATCH (m)-[:POLYGON_STRUCTURE]->(a:Shell)-[:POLYGON_START]->() WHERE id(m) = $mainId RETURN a", map("mainId", main.getId()));

            assertThat(result.hasNext(), equalTo(true));

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMGraphPolygonOneDirectionNoOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            main.setProperty("relation_osm_id", 1l);

            int x = 4;
            int y = 3;

            Node[] ways = new Node[x];
            Node[][] wayNodes = new Node[x][y];
            Node[][] nodes = new Node[x][y];
            Node[] connectors = new Node[x];

            Relationship rel;

            for (int i = 0; i < x; i++) {
                ways[i] = db.createNode(Label.label("OSMWay"));
                main.createRelationshipTo(ways[i], Relation.MEMBER);

                for (int j = 0; j < y; j++) {
                    wayNodes[i][j] = db.createNode(Label.label("OSMWayNode"));

                    if (j == 0) {
                        ways[i].createRelationshipTo(wayNodes[i][j], Relation.FIRST_NODE);
                    } else {
                        rel = wayNodes[i][j - 1].createRelationshipTo(wayNodes[i][j], Relation.NEXT);
                        rel.setProperty("relation_osm_id", 1l);
                    }
                }

                for (int j = 0; j < y; j++) {
                    nodes[i][j] = db.createNode(Label.label("OSMNode"));

                    int yCoord = (i * y) + j;
                    if (i < x / 2) {
                        nodes[i][j].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 1 * 1e-3, yCoord * 1e-3));
                    } else {
                        nodes[i][j].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 0, ((y * x) - yCoord - 1) * 1e-3));
                    }

                    wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
                }
            }

            for (int i = 0; i < x; i++) {
                connectors[i] = db.createNode(Label.label("OSMWayNode"));
            }

            Node[] connectorNodes = new Node[x];

            rel = wayNodes[0][y - 1].createRelationshipTo(connectors[0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectorNodes[0] = db.createNode(Label.label("OSMNode"));
            connectorNodes[0].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 1 * 1e-3, 2.5 * 1e-3));
            connectors[0].createRelationshipTo(connectorNodes[0], Relation.NODE);

            rel = wayNodes[1][y - 1].createRelationshipTo(connectors[1], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectorNodes[1] = db.createNode(Label.label("OSMNode"));
            connectorNodes[1].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 0.5 * 1e-3, 5 * 1e-3));
            connectors[1].createRelationshipTo(connectorNodes[1], Relation.NODE);

            rel = wayNodes[2][y - 1].createRelationshipTo(connectors[2], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectorNodes[2] = db.createNode(Label.label("OSMNode"));
            connectorNodes[2].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 2.5 * 1e-3));
            connectors[2].createRelationshipTo(connectorNodes[2], Relation.NODE);

            rel = wayNodes[3][y - 1].createRelationshipTo(connectors[3], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectorNodes[3] = db.createNode(Label.label("OSMNode"));
            connectorNodes[3].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, 0.5 * 1e-3, 0));
            connectors[3].createRelationshipTo(connectorNodes[3], Relation.NODE);

            db.execute("CALL spatial.osm.graph.createPolygon($main)", map("main", main));
            Result result = db.execute("MATCH (m)-[:POLYGON_STRUCTURE]->(a:Shell)-[:POLYGON_START]->() WHERE id(m) = $mainId RETURN a", map("mainId", main.getId()));

            assertThat(result.hasNext(), equalTo(true));

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMGraphPolylineOneDirectionOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            main.setProperty("relation_osm_id", 1l);

            int x = 4;
            int y = 3;

            Node[] ways = new Node[x];
            Node[][] wayNodes = new Node[x][y];
            Node[][] nodes = new Node[x][y];
            Node[] connectors = new Node[x];

            Relationship rel;

            for (int i = 0; i < x; i++) {
                ways[i] = db.createNode(Label.label("OSMWay"));
                main.createRelationshipTo(ways[i], Relation.MEMBER);

                for (int j = 0; j < y; j++) {
                    wayNodes[i][j] = db.createNode(Label.label("OSMWayNode"));

                    if (j == 0) {
                        ways[i].createRelationshipTo(wayNodes[i][j], Relation.FIRST_NODE);
                    } else {
                        rel = wayNodes[i][j - 1].createRelationshipTo(wayNodes[i][j], Relation.NEXT);
                        rel.setProperty("relation_osm_id", 1l);
                    }
                }

                for (int j = 0; j < y; j++) {
                    nodes[i][j] = db.createNode(Label.label("OSMNode"));
                    nodes[i][j].setProperty("location", Values.pointValue(CoordinateReferenceSystem.WGS84, i, j));

                    System.out.printf("%s; [%d, %d]\n", wayNodes[i][j], i, j);

                    wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
                }
            }

            for (int i = 0; i < x; i++) {
                connectors[i] = db.createNode(Label.label("OSMWayNode"));
            }

            rel = wayNodes[0][y - 1].createRelationshipTo(connectors[0], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[0].createRelationshipTo(nodes[1][0], Relation.NODE);

            rel = wayNodes[1][y - 1].createRelationshipTo(connectors[1], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[1].createRelationshipTo(nodes[2][0], Relation.NODE);

            rel = wayNodes[2][y - 1].createRelationshipTo(connectors[2], Relation.NEXT);
            rel.setProperty("relation_osm_id", 1l);
            connectors[2].createRelationshipTo(nodes[3][0], Relation.NODE);

            db.execute("CALL spatial.osm.graph.createPolygon($main)", map("main", main));
            Result result = db.execute("MATCH (m)-[:POLYLINE_STRUCTURE]->(a:Polyline)-[:POLYLINE_START]->() WHERE id(m) = $mainId RETURN a", map("mainId", main.getId()));

            assertThat(result.hasNext(), equalTo(true));

            result = db.execute("MATCH (m) WHERE id(m) = $mainId RETURN spatial.osm.graph.polylineAsWKT(m) AS WKT", map("mainId", main.getId()));

            if (result.hasNext()) {
                String WKT = (String) result.next().get("WKT");
                assertThat(WKT, equalTo("MULTILINESTRING((3.0 2.0,3.0 1.0,3.0 0.0,2.0 2.0,2.0 1.0,2.0 0.0,1.0 2.0,1.0 1.0,1.0 0.0,0.0 2.0,0.0 1.0,0.0 0.0))"));
            }

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMGraphPolygon() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            main.setProperty("relation_osm_id", 1L);
            Node[] ways = new Node[4];
            Node[][] wayNodes = new Node[ways.length][4];
            Node[][] nodes = new Node[ways.length][4];

            createNestedSquareOSM(main, ways, wayNodes, nodes);

            testCall(db, "CALL spatial.osm.graph.createPolygon($main)", map("main", main), result -> {
            });

            List<Node> list = new MonoDirectionalTraversalDescription().breadthFirst()
                    .relationships(Relation.FIRST_NODE, Direction.OUTGOING)
                    .relationships(Relation.NEXT)
                    .relationships(RelationshipType.withName("NEXT_IN_POLYGON"), Direction.OUTGOING)
                    .relationships(Relation.NODE, Direction.OUTGOING)
                    .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(Relation.NODE))
                    .traverse(ways[0]).nodes().stream().collect(Collectors.toList());

            List<Point> points = list.stream().map(node -> (Point) node.getProperty("location")).collect(Collectors.toList());

            ArrayList<Point> expected = new ArrayList<>();
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, -10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, -10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 10.0, -10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, -10.0, -10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 10.0, 10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, -10.0, 10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, 10.0));
            expected.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, 10.0));

            assertThat(points, equalTo(expected));

            tx.success();
        }
    }

    private void createNestedSquareOSM(Node main, Node[] ways, Node[][] wayNodes, Node[][] nodes) {
        Label wayLabel = Label.label("OSMWay");
        Label wayNodeLabel = Label.label("OSMWayNode");
        Label nodeLabel = Label.label("OSMNode");

        Point[] points = new Point[]{
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, -10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 10, -10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 10, 10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, 10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, -10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -10, -10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -10, 10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, 10),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, -100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 100, -100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 100, 100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, 0.001, 100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, -100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -100, -100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -100, 100),
                Values.pointValue(CoordinateReferenceSystem.Cartesian, -0.001, 100)
        };


        for (int i = 0; i < ways.length; i++) {
            ways[i] = db.createNode(wayLabel);
            main.createRelationshipTo(ways[i], Relation.MEMBER);

            for (int j = 0; j < wayNodes[i].length; j++) {
                wayNodes[i][j] = db.createNode(wayNodeLabel);
            }

            ways[i].createRelationshipTo(wayNodes[i][0], Relation.FIRST_NODE);
            for (int j = 0; j < wayNodes[i].length - 1; j++) {
                wayNodes[i][j].createRelationshipTo(wayNodes[i][j + 1], Relation.NEXT);
            }

            for (int j = 0; j < nodes[i].length; j++) {
                nodes[i][j] = db.createNode(nodeLabel);

                wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
            }
        }

        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes[i].length; j++) {
                nodes[i][j].setProperty("location", points[i * 4 + j]);
            }
        }
    }

    @Test
    public void shouldFailToMakePolygonFromNullField() {
        testCallFails(db, "RETURN spatial.polygon($points)", map("points", null), "Invalid 'points', should be a list of at least 3, but was: null");
    }

    @Test
    public void shouldFailToMakePolygonFromEmptyField() {
        testCallFails(db, "RETURN spatial.polygon($points)", map("points", new ArrayList<Point>()), "Invalid 'points', should be a list of at least 3, but was: 0");
    }

    @Test
    public void shouldFailToMakePolygonFromInvalidPoints() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 1, 0));
        testCallFails(db, "RETURN spatial.polygon($points)", map("points", points), "Invalid 'points', should be a list of at least 3, but was: 2");
    }

    public static void testCall(GraphDatabaseService db, String call, Consumer<Map<String, Object>> consumer) {
        testCall(db, call, null, consumer);
    }

    @Test
    public void shouldFindConvexHullForArrayPolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -10, -10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, -10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, 10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 20));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -10, 10));
        testCall(db, "WITH spatial.polygon($points) AS polygon RETURN spatial.algo.convexHull(polygon) AS convexHull", map("points", points), result -> {
            assertThat("Should have one result", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get convexHull as list", record, instanceOf(List.class));
            List convexHull = (List) record;
            assertThat("expected polygon of size 6", convexHull.size(), equalTo(6));
        });
    }

    @Test
    public void shouldFindBBoxForPolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 10));
        testCall(db, "WITH spatial.polygon($points) AS polygon RETURN spatial.boundingBox(polygon) AS bbox", map("points", points), result -> {
            assertThat("Should have one result", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get bbox as map", record, instanceOf(Map.class));
            Map bbox = (Map) record;
            assertThat("Should have min key", bbox.containsKey("min"), equalTo(true));
            assertThat("Should have max key", bbox.containsKey("max"), equalTo(true));
            assertThat("Should have correct bbox.min", bbox.get("min"), equalTo(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0)));
            assertThat("Should have correct bbox.max", bbox.get("max"), equalTo(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, 10)));
        });
    }

    @Test
    public void shouldFindPointInPolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 10));
        Point a = Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1);
        Point b = Values.pointValue(CoordinateReferenceSystem.WGS84, 9, 9);
        testCall(db, "WITH spatial.polygon($points) AS polygon RETURN spatial.algo.withinPolygon($a,polygon) AS a, spatial.algo.withinPolygon($b,polygon) AS b", map("points", points, "a", a, "b", b), result -> {
            assertThat("Should have 'a' key", result.containsKey("a"), equalTo(true));
            assertThat("Should have 'b' key", result.containsKey("b"), equalTo(true));
            assertThat("'a' should be inside polygon", result.get("a"), equalTo(true));
            assertThat("'b' should be outside polygon", result.get("b"), equalTo(false));
        });
    }

    private static final double circumference = 40000000;
    private static final double oneDegreeDistance = circumference / 360.0;
    private static final double oneByOneDiagonal = Math.sqrt(2 * oneDegreeDistance * oneDegreeDistance);
    private static final double oneByOneArea = oneDegreeDistance * oneDegreeDistance;

    @Test
    public void shouldFindAreaOfSimplePolygon() {
        // square about the equator-utm
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, 1));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, -1));

        testCall(db, "WITH spatial.polygon($points) AS polygon RETURN spatial.algo.area(polygon) AS area", map("points", points), result -> {
            assertThat("Should have 'area' key", result.containsKey("area"), equalTo(true));
            assertThat("'area' should be within 1% of a 2x2 degree square in m^2", (double) result.get("area"), closeTo(4 * oneByOneArea, oneByOneArea / 100.0));
        });
    }

    @Test
    public void shouldFindAreaOfSimplePolygonFarFromEquator() {
        // square about the equator-utm
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 60));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 60));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 61));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 61));

        testCall(db, "WITH spatial.polygon($points) AS polygon RETURN spatial.algo.area(polygon) AS area", map("points", points), result -> {
            assertThat("Should have 'area' key", result.containsKey("area"), equalTo(true));
            double shrinkFactor = Math.cos(Math.PI * 60.5 / 180.0);
            assertThat("'area' far from equator should be smaller by a factor of 'cos(latitude)' than a 1x1 degree square in m^2", (double) result.get("area"), closeTo(oneByOneArea * shrinkFactor, oneByOneArea / 1000.0));
        });
    }

    @Test
    public void shouldFindDistanceBetweenTwoPolygons() {
        // square above-right of equator-utm
        ArrayList<Point> a = new ArrayList<>();
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 2, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 2, 2));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 2));

        // square below-left of equator-utm
        ArrayList<Point> b = new ArrayList<>();
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -2, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -2, -2));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -2));

        testCall(db, "WITH spatial.polygon($a) AS a, spatial.polygon($b) AS b RETURN spatial.algo.distance(a,b) AS distance", map("a", a, "b", b), result -> {
            assertThat("Should have 'distance' key", result.containsKey("distance"), equalTo(true));
            assertThat("'distance' should be close to a 1x1 degree diagonal in meters", (double) result.get("distance"), closeTo(2 * oneByOneDiagonal, 250));
        });
    }

    @Test
    public void shouldFindDistanceAndEndPointsBetweenTwoPolygons() {
        // square above-right of equator-utm
        ArrayList<Point> a = new ArrayList<>();
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 2, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 2, 2));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 2));

        // square below-left of equator-utm
        ArrayList<Point> b = new ArrayList<>();
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -2, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -2, -2));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -2));

        testCall(db, "WITH spatial.polygon($a) AS a, spatial.polygon($b) AS b RETURN spatial.algo.distance.ends(a,b) AS dist", map("a", a, "b", b), result -> {
            assertThat("Should have 'dist' key", result.containsKey("dist"), equalTo(true));
            assertThat("Should get distance result as map", result.get("dist"), instanceOf(Map.class));
            Map dist = (Map) result.get("dist");
            assertThat("'distance' should be close to a 2x2 degree diagonal in meters", (double) dist.get("distance"), closeTo(2 * oneByOneDiagonal, 250));
            assertThat(Arrays.asList(dist.get("start"), dist.get("end")),
                    containsInAnyOrder(
                            Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1),
                            Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1)));
        });
    }

    @Test
    public void shouldFindDistanceBetweenTwoPolygonsWithInterpolation() {
        // square above equator-utm
        ArrayList<Point> a = new ArrayList<>();
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 2));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, 2));

        // triangle below, but touching equator
        ArrayList<Point> b = new ArrayList<>();
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, -1));

        testCall(db, "WITH spatial.polygon($a) AS a, spatial.polygon($b) AS b RETURN spatial.algo.distance(a,b) AS distance", map("a", a, "b", b), result -> {
            assertThat("Should have 'distance' key", result.containsKey("distance"), equalTo(true));
            assertThat("'distance' should be close to one degree in meters", (double) result.get("distance"), closeTo(oneDegreeDistance, 115));
        });
    }

    @Test
    public void shouldFindDistanceAndEndPointsBetweenTwoPolygonsWithInterpolation() {
        // square above equator-utm
        ArrayList<Point> a = new ArrayList<>();
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 1));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 2));
        a.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, 2));

        // triangly below, but touching equator
        ArrayList<Point> b = new ArrayList<>();
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -1, -1));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0));
        b.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, -1));

        testCall(db, "WITH spatial.polygon($a) AS a, spatial.polygon($b) AS b RETURN spatial.algo.distance.ends(a,b) AS dist", map("a", a, "b", b), result -> {
            assertThat("Should have 'dist' key", result.containsKey("dist"), equalTo(true));
            assertThat("Should get distance result as map", result.get("dist"), instanceOf(Map.class));
            Map dist = (Map) result.get("dist");
            assertThat("'distance' should be close to one degree in meters", (double) dist.get("distance"), closeTo(oneDegreeDistance, 115));
            assertThat("Start point x-coord should be interpolated to zero (UTM)", ((Point) dist.get("start")).getCoordinate().getCoordinate().get(0), closeTo(0, 0.000001));
            assertThat("Start point y-coord should be interpolated a little above the one degree line due to great-circle", ((Point) dist.get("start")).getCoordinate().getCoordinate().get(1), closeTo(1, 0.001));
            assertThat(dist.get("end"), equalTo(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0)));
        });
    }

    public static Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }

    public static void testCall(GraphDatabaseService db, String call, Map<String, Object> params, Consumer<Map<String, Object>> consumer) {
        testCall(db, call, params, consumer, true);
    }

    public static void testCallFails(GraphDatabaseService db, String call, Map<String, Object> params, String error) {
        try {
            testResult(db, call, params, (res) -> {
                while (res.hasNext()) {
                    res.next();
                }
            });
            fail("Expected an exception containing '" + error + "', but no exception was thrown");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(error));
        }
    }

    public static void testCall(GraphDatabaseService db, String call, Map<String, Object> params, Consumer<Map<String, Object>> consumer, boolean onlyOne) {
        testResult(db, call, params, (res) -> {
            if (res.hasNext()) {
                Map<String, Object> row = res.next();
                consumer.accept(row);
            }
            if (onlyOne) {
                Assert.assertFalse(res.hasNext());
            }
        });
    }

    public static void testCallCount(GraphDatabaseService db, String call, Map<String, Object> params, int count) {
        testResult(db, call, params, (res) -> {
            int numLeft = count;
            while (numLeft > 0) {
                assertTrue("Expected " + count + " results but found only " + (count - numLeft), res.hasNext());
                res.next();
                numLeft--;
            }
            Assert.assertFalse("Expected " + count + " results but there are more", res.hasNext());
        });
    }

    public static void testResult(GraphDatabaseService db, String call, Consumer<Result> resultConsumer) {
        testResult(db, call, null, resultConsumer);
    }

    private static void testResult(GraphDatabaseService db, String call, Map<String, Object> params, Consumer<Result> resultConsumer) {
        try (Transaction tx = db.beginTx()) {
            Map<String, Object> p = (params == null) ? map() : params;
            resultConsumer.accept(db.execute(call, p));
            tx.success();
        }
    }

    private static void registerUDFClass(GraphDatabaseService db, Class<?> udfClass) throws KernelException {
        Procedures procedures = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(Procedures.class);
        procedures.registerProcedure(udfClass);
        procedures.registerFunction(udfClass);
    }

    private long execute(String statement) {
        return Iterators.count(db.execute(statement));
    }

    private long execute(String statement, Map<String, Object> params) {
        return Iterators.count(db.execute(statement, params));
    }

}
