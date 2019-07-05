package org.neo4j.spatial.benchmarks.macro;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.algo.Intersect;
import org.neo4j.spatial.algo.IntersectCalculator;
import org.neo4j.spatial.algo.cartesian.CartesianArea;
import org.neo4j.spatial.algo.wgs84.WGS84Area;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.neo4j.UserDefinedFunctions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.util.Arrays;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class IntersectMacroBenchmarks {

    private Node[] nodes;
    private Node polylineNode;
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
        db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("benchmarks/data/sweden"));

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
                nodes[i] = db.findNode(label, "relation_osm_id", ids[i]);

                if (nodes[i] == null) {
                    throw new IllegalStateException("OSMRelation not found for relation: " + ids[i]);
                }
            }
            polylineNode = db.findNode(label, "relation_osm_id", polylineId);

            tx.success();
        }
    }

    @TearDown
    public void tearDown() {
        db.shutdown();
    }

    @Benchmark
    public  void testCartesianIntersectNaiveArray(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);
                MultiPolyline polyLine = UserDefinedFunctions.getArrayPolyline(polylineNode);
                bh.consume(cartesianNaiveCalculator.intersect(polygon, polyLine));

            }
            tx.success();
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
            tx.success();
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
                tx.success();
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
            tx.success();
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
            tx.success();
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
            tx.success();
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
                tx.success();
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
            tx.success();
        }
    }
}
