package org.neo4j.spatial.benchmarks.macro;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.spatial.algo.Intersect;
import org.neo4j.spatial.algo.IntersectCalculator;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.neo4j.UserDefinedFunctions;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Path;

import static org.neo4j.configuration.GraphDatabaseInternalSettings.databases_root_path;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class IntersectMacroBenchmarks {

    private Node[] nodes;
    private Node polylineNode;
    private DatabaseManagementService databases;
    private GraphDatabaseService db;
    private Intersect geographicNaiveCalculator = IntersectCalculator.getCalculator(CRS.WGS84, IntersectCalculator.AlgorithmVariant.Naive);
    private Intersect geographicSweepCalculator = IntersectCalculator.getCalculator(CRS.WGS84, IntersectCalculator.AlgorithmVariant.MCSweepLine);
    private Intersect cartesianNaiveCalculator = IntersectCalculator.getCalculator(CRS.Cartesian, IntersectCalculator.AlgorithmVariant.Naive);
    private Intersect cartesianSweepCalculator = IntersectCalculator.getCalculator(CRS.Cartesian, IntersectCalculator.AlgorithmVariant.MCSweepLine);


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IntersectMacroBenchmarks.class.getSimpleName())
                .forks(1)
                .addProfiler(JfrProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        databases = new TestDatabaseManagementServiceBuilder().setConfig(databases_root_path, Path.of("benchmarks/data")).build();
        db = databases.database("sweden");

        long[] ids = new long[]{
                54367,
                54417,
                54412,
        };
        long polylineId = 35969;

        nodes = new Node[ids.length];
        Label label = Label.label("OSMRelation");

        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < ids.length; i++) {
                nodes[i] = tx.findNode(label, "relation_osm_id", ids[i]);

                if (nodes[i] == null) {
                    throw new IllegalStateException("OSMRelation not found for relation: " + ids[i]);
                }
            }
            polylineNode = tx.findNode(label, "relation_osm_id", polylineId);

            tx.commit();
        }
    }

    @TearDown
    public void tearDown() {
        databases.shutdown();
    }

    @Benchmark
    public  void testCartesianIntersectNaiveArray(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getArrayPolyline(polylineNode);
                bh.consume(cartesianNaiveCalculator.intersect(polygon, polyLine));

            }
            tx.commit();
        }
    }

    @Benchmark
    public  void testCartesianIntersectSweepArray(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getArrayPolyline(polylineNode);
                bh.consume(cartesianSweepCalculator.intersect(polygon, polyLine));
            }
            tx.commit();
        }
    }

    @Benchmark
    public  void testGeographicIntersectNaiveArray(Blackhole bh) {
        try {
            try (Transaction tx = db.beginTx()) {
                for (Node osmRelation : nodes) {
                    MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);
                    MultiPolyline polyLine = UserDefinedFunctions.getArrayPolyline(polylineNode);
                    bh.consume(geographicNaiveCalculator.intersect(polygon, polyLine));
                }
                tx.commit();
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Benchmark
    public  void testGeographicIntersectSweepArray(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getArrayPolyline(polylineNode);

                bh.consume(geographicSweepCalculator.intersect(polygon, polyLine));
            }
            tx.commit();
        }
    }

    @Benchmark
    public  void testCartesianIntersectNaiveGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getGraphNodePolyline(polylineNode);
                bh.consume(cartesianNaiveCalculator.intersect(polygon, polyLine));

            }
            tx.commit();
        }
    }

    @Benchmark
    public  void testCartesianIntersectSweepGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getGraphNodePolyline(polylineNode);
                bh.consume(cartesianSweepCalculator.intersect(polygon, polyLine));
            }
            tx.commit();
        }
    }

    @Benchmark
    public  void testGeographicIntersectNaiveGraph(Blackhole bh) {
        try {
            try (Transaction tx = db.beginTx()) {
                for (Node osmRelation : nodes) {
                    MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);
                    MultiPolyline polyLine = UserDefinedFunctions.getGraphNodePolyline(polylineNode);
                    bh.consume(geographicNaiveCalculator.intersect(polygon, polyLine));
                }
                tx.commit();
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Benchmark
    public  void testGeographicIntersectSweepGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getGraphNodePolyline(polylineNode);
                bh.consume(geographicSweepCalculator.intersect(polygon, polyLine));
            }
            tx.commit();
        }
    }
}
