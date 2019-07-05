package org.neo4j.spatial.benchmarks.macro;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReferenceCalculator;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
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
import java.util.Random;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class LinearReferenceMacroBenchmarks {

    private Node[] nodes;
    private Point[] start;
    private Point[] direction;
    private double[] cartesianDistance;
    private double[] geographicDistance;
    private GraphDatabaseService db;
    private Distance geographicDistanceCalc= DistanceCalculator.getCalculator(CRS.WGS84);
    private Distance cartesianDistanceCalc = DistanceCalculator.getCalculator(CRS.Cartesian);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LinearReferenceMacroBenchmarks.class.getSimpleName())
                .forks(1)
                .addProfiler(JfrProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("benchmarks/data/sweden"));

        long[] ids = new long[]{
                54413,
                52834,
                941530,
                52832,
                54403,
                52826,
                54374,
                54417,
                54412,
                52824,
                54409,
                54391,
                54386,
                54220,
                54223,
                52825,
                52827,
                54221,
                54367,
                54222,
                940675
        };

        nodes = new Node[ids.length];
        start = new Point[ids.length];
        direction = new Point[ids.length];
        cartesianDistance = new double[ids.length];
        geographicDistance = new double[ids.length];
        Label label = Label.label("OSMRelation");

        Random r = new Random(0);
        double geographicDist = 0;
        double cartesianDist = 0;

        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < ids.length; i++) {
                nodes[i] = db.findNode(label, "relation_osm_id", ids[i]);

                geographicDist = 0;
                cartesianDist = 0;

                Polygon.SimplePolygon polygon = UserDefinedFunctions.getArrayPolygon(nodes[i]).getChildren().get(0).getPolygon();

                for (int j = 0; j < polygon.getPoints().length - 1; j++) {
                    geographicDist += geographicDistanceCalc.distance(polygon.getPoints()[j], polygon.getPoints()[j+1]);
                }

                for (int j = 0; j < polygon.getPoints().length - 1; j++) {
                    cartesianDist += cartesianDistanceCalc.distance(polygon.getPoints()[j], polygon.getPoints()[j+1]);
                }

                polygon.startTraversal();
                start[i] = polygon.getNextPoint();
                direction[i] = polygon.getNextPoint();
                geographicDistance[i] = geographicDist + r.nextDouble() * 1.5;
                cartesianDistance[i] = cartesianDist + r.nextDouble() * 1.5;

                if (nodes[i] == null) {
                    throw new IllegalStateException("OSMRelation not found for relation: " + ids[i]);
                }
            }
            tx.success();
        }
    }

    @TearDown
    public void tearDown() {
        db.shutdown();
    }

    @Benchmark
    public void testCartesianLinearReferenceGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < nodes.length; i++) {
                Polygon.SimplePolygon polygon = UserDefinedFunctions.getGraphNodePolygon(nodes[i]).getChildren().get(0).getPolygon();

                bh.consume(LinearReferenceCalculator.getCalculator(CRS.Cartesian).reference(polygon, start[i], direction[i], cartesianDistance[i]));
            }
            tx.success();
        }
    }

    @Benchmark
    public void testGeographicLinearReferenceGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < nodes.length; i++) {
                Polygon.SimplePolygon polygon = UserDefinedFunctions.getGraphNodePolygon(nodes[i]).getChildren().get(0).getPolygon();

                bh.consume(LinearReferenceCalculator.getCalculator(CRS.WGS84).reference(polygon, start[i], direction[i], geographicDistance[i]));
            }
            tx.success();
        }
    }

    @Benchmark
    public void testCartesianLinearReferenceProperty(Blackhole bh) {
        try {
            try (Transaction tx = db.beginTx()) {
                for (int i = 0; i < nodes.length; i++) {
                    Polygon.SimplePolygon polygon = UserDefinedFunctions.getArrayPolygon(nodes[i]).getChildren().get(0).getPolygon();

                    bh.consume(LinearReferenceCalculator.getCalculator(CRS.Cartesian).reference(polygon, start[i], direction[i], cartesianDistance[i]));
                }
                tx.success();
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Benchmark
    public void testGeographicLinearReferenceProperty(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < nodes.length; i++) {
                Polygon.SimplePolygon polygon = UserDefinedFunctions.getArrayPolygon(nodes[i]).getChildren().get(0).getPolygon();

                bh.consume(LinearReferenceCalculator.getCalculator(CRS.WGS84).reference(polygon, start[i], direction[i], geographicDistance[i]));
            }
            tx.success();
        }
    }
}
