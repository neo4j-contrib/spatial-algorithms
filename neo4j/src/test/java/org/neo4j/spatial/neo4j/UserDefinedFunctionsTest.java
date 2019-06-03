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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
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
        testCall(db, "CALL neo4j.polygon($points)", map("points", points), result -> {
            assertThat("Should have one polygon", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get list of points", record, instanceOf(List.class));
            List<Point> polygon = (List<Point>) record;
            assertThat("Should have 4 points", polygon.size(), equalTo(4));
            assertThat("Should be closed polygon", polygon.get(0), equalTo(polygon.get(polygon.size() - 1)));
        });
    }

    @Test
    public void shouldCreateOSMArrayPolygonNormalDirectionOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            Node[] ways = new Node[4];
            Node[][] wayNodes = new Node[ways.length][3];
            Node[][] nodes = new Node[ways.length][wayNodes[0].length - 2];
            Node[] connectors = createOSMPolygon(main, ways, wayNodes, nodes, ways.length);

            int last = wayNodes[0].length - 1;
            int first = 0;

            wayNodes[0][last].createRelationshipTo(connectors[0], Relation.NODE);
            wayNodes[1][first].createRelationshipTo(connectors[0], Relation.NODE);

            wayNodes[1][last].createRelationshipTo(connectors[1], Relation.NODE);
            wayNodes[2][first].createRelationshipTo(connectors[1], Relation.NODE);

            wayNodes[2][last].createRelationshipTo(connectors[2], Relation.NODE);
            wayNodes[3][first].createRelationshipTo(connectors[2], Relation.NODE);

            wayNodes[3][last].createRelationshipTo(connectors[3], Relation.NODE);
            wayNodes[0][first].createRelationshipTo(connectors[3], Relation.NODE);

            testCall(db, "CALL neo4j.createOSMArrayPolygon($main)",
                    map("main", main), result -> {
                    });

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMArrayPolygonRandomDirectionOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            Node[] ways = new Node[4];
            Node[][] wayNodes = new Node[ways.length][3];
            Node[][] nodes = new Node[ways.length][wayNodes[0].length-2];
            Node[] connectors = createOSMPolygon(main, ways, wayNodes, nodes, ways.length);

            int last = wayNodes[0].length - 1;
            int first = 0;

            wayNodes[0][last].createRelationshipTo(connectors[0], Relation.NODE);
            wayNodes[1][first].createRelationshipTo(connectors[0], Relation.NODE);

            wayNodes[1][last].createRelationshipTo(connectors[1], Relation.NODE);
            wayNodes[2][last].createRelationshipTo(connectors[1], Relation.NODE);

            wayNodes[2][first].createRelationshipTo(connectors[2], Relation.NODE);
            wayNodes[3][first].createRelationshipTo(connectors[2], Relation.NODE);

            wayNodes[3][last].createRelationshipTo(connectors[3], Relation.NODE);
            wayNodes[0][first].createRelationshipTo(connectors[3], Relation.NODE);

            testCall(db, "CALL neo4j.createOSMArrayPolygon($main)",
                    map("main", main), result -> {
                    });

            tx.success();
        }
    }

    @Test
    public void shouldCreateOSMArrayPolygonNormalDirectionNoOverlap() {
        try (Transaction tx = db.beginTx()) {
            Node main = db.createNode(Label.label("OSMRelation"));
            Node[] ways = new Node[4];
            Node[][] wayNodes = new Node[ways.length][2];
            Node[][] nodes = new Node[ways.length][wayNodes[0].length-2];
            Node[] connectors = createOSMPolygon(main, ways, wayNodes, nodes, 2*ways.length);

            int last = wayNodes[0].length - 1;
            int first = 0;

            for (int i = 0; i < connectors.length; i++) {
                double p = (i/2);

                if (i % 2 == 1) {
                    p += 0.1;
                }

                connectors[i].setProperty("location", Values.pointValue(CoordinateReferenceSystem.Cartesian, p, p));
            }

            for (int i = 0; i < ways.length; i++) {
                wayNodes[i][last].createRelationshipTo(connectors[i*2], Relation.NODE);
                wayNodes[(i + 1) % ways.length][first].createRelationshipTo(connectors[(i*2)+1], Relation.NODE);
            }

            testCall(db, "CALL neo4j.createOSMArrayPolygon($main)",
                    map("main", main), result -> {
                    });

            tx.success();
        }
    }

    private Node[] createOSMPolygon(Node main, Node[] ways, Node[][] wayNodes, Node[][] nodes, int connectorsLength) {
        Point placeholderPoint = Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0);

        Label wayLabel = Label.label("OSMWay");
        Label wayNodeLabel = Label.label("OSMWayNode");
        Label nodeLabel = Label.label("OSMNode");

        for (int i = 0; i < ways.length; i++) {
            ways[i] = db.createNode(wayLabel);
            main.createRelationshipTo(ways[i], Relation.MEMBER);

            for (int j = 0; j < wayNodes[i].length; j++) {
                wayNodes[i][j] = db.createNode(wayNodeLabel);
            }

            ways[i].createRelationshipTo(wayNodes[i][0], Relation.FIRST_NODE);
            for (int j = 0; j < wayNodes[i].length - 1; j++) {
                wayNodes[i][j].createRelationshipTo(wayNodes[i][j+1], Relation.NEXT);
            }

            for (int j = 0; j < nodes[i].length; j++) {
                nodes[i][j] = db.createNode(nodeLabel);
                nodes[i][j].setProperty("location", placeholderPoint);

                wayNodes[i][j+1].createRelationshipTo(nodes[i][j], Relation.NODE);
            }
        }

        Node[] connectors = new Node[connectorsLength];
        for (int i = 0; i < connectors.length; i++) {
            connectors[i] = db.createNode(nodeLabel);
            connectors[i].setProperty("location", placeholderPoint);
        }

        return connectors;
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

            testCall(db, "CALL neo4j.createOSMGraphPolygon($main)",
                    map("main", main), result -> {
                    });

            List<Node> list = new MonoDirectionalTraversalDescription().breadthFirst()
                    .relationships(Relation.FIRST_NODE, Direction.OUTGOING)
                    .relationships(Relation.NEXT)
                    .relationships(RelationshipType.withName("NEXT_IN_POLYGON"), Direction.OUTGOING)
                    .relationships(Relation.NODE, Direction.OUTGOING)
                    .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(Relation.NODE))
                    .traverse(ways[0]).nodes().stream().collect(Collectors.toList());

            for (Node node : list) {
                System.out.println(node);
                System.out.println(node.getProperty("location"));
            }

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
                wayNodes[i][j].createRelationshipTo(wayNodes[i][j+1], Relation.NEXT);
            }

            for (int j = 0; j < nodes[i].length; j++) {
                nodes[i][j] = db.createNode(nodeLabel);

                wayNodes[i][j].createRelationshipTo(nodes[i][j], Relation.NODE);
            }
        }

        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes[i].length; j++) {
                nodes[i][j].setProperty("location", points[i*4+j]);
            }
        }
    }

    @Test
    public void shouldFailToMakePolygonFromNullField() {
        testCallFails(db, "CALL neo4j.polygon($points)", map("points", null), "Invalid 'points', should be a list of at least 3, but was: null");
    }

    @Test
    public void shouldFailToMakePolygonFromEmptyField() {
        testCallFails(db, "CALL neo4j.polygon($points)", map("points", new ArrayList<Point>()), "Invalid 'points', should be a list of at least 3, but was: 0");
    }

    @Test
    public void shouldFailToMakePolygonFromInvalidPoints() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.Cartesian, 1, 0));
        testCallFails(db, "CALL neo4j.polygon($points)", map("points", points), "Invalid 'points', should be a list of at least 3, but was: 2");
    }

    public static void testCall(GraphDatabaseService db, String call, Consumer<Map<String, Object>> consumer) {
        testCall(db, call, null, consumer);
    }

    @Test
    public void shouldFindConvexHullForArrayPolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -10,-10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10,-10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 1, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10,10));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0,20));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, -10,10));
        testCall(db, "CALL neo4j.polygon($points) YIELD polygon WITH neo4j.convexHullPoints(polygon) as convexHull RETURN convexHull", map("points", points), result -> {
            assertThat("Should have one result", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get convexHull as list", record, instanceOf(List.class));
            List<Point> convexHull = (List<Point>) record;
            assertThat("expected polygon of size 6", convexHull.size(), equalTo(6));
        });
    }

    @Test
    public void shouldFindBBoxForPolygon() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 10, 0));
        points.add(Values.pointValue(CoordinateReferenceSystem.WGS84, 0, 10));
        testCall(db, "CALL neo4j.polygon($points) YIELD polygon WITH neo4j.boundingBoxFor(polygon) as bbox RETURN bbox", map("points", points), result -> {
            assertThat("Should have one result", result.size(), equalTo(1));
            Object record = result.values().iterator().next();
            assertThat("Should get bbox as map", record, instanceOf(Map.class));
            Map<String, Object> bbox = (Map<String, Object>) record;
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
        testCall(db, "CALL neo4j.polygon($points) YIELD polygon WITH polygon, neo4j.boundingBoxFor(polygon) as bbox RETURN neo4j.withinPolygon($a,polygon) as a, neo4j.withinPolygon($b,polygon) as b", map("points", points, "a", a, "b", b), result -> {
            assertThat("Should get result as map", result, instanceOf(Map.class));
            Map<String, Object> results = (Map<String, Object>) result;
            assertThat("Should have 'a' key", results.containsKey("a"), equalTo(true));
            assertThat("Should have 'b' key", results.containsKey("b"), equalTo(true));
            assertThat("'a' should be inside polygon", results.get("a"), equalTo(true));
            assertThat("'b' should be outside polygon", results.get("b"), equalTo(false));
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
