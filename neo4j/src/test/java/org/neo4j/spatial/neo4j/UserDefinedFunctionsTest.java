package org.neo4j.spatial.neo4j;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.internal.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.Values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UserDefinedFunctionsTest {
    private GraphDatabaseService db;

    @Before
    public void setUp() throws KernelException {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
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
