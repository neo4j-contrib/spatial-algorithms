package org.neo4j.spatial.benchmarks.macro;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.algo.cartesian.CartesianArea;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.wgs84.WGS84Area;
import org.neo4j.spatial.algo.wgs84.WGS84ConvexHull;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.MultiPolygon;
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

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class ConvexHullMacroBenchmarks {

    private Node[] nodes;
    private GraphDatabaseService db;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConvexHullMacroBenchmarks.class.getSimpleName())
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
        Label label = Label.label("OSMRelation");

        try (Transaction tx = db.beginTx()) {
            for (int i = 0; i < ids.length; i++) {
                nodes[i] = db.findNode(label, "relation_osm_id", ids[i]);

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
    public void testCartesianConvexHullGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);

                bh.consume(CartesianConvexHull.convexHull(polygon));

            }
            tx.success();
        }
    }

    @Benchmark
    public void testGeographicConvexHullGraph(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getGraphNodePolygon(osmRelation);

                bh.consume(WGS84ConvexHull.convexHull(polygon));
            }
            tx.success();
        }
    }

    @Benchmark
    public void testCartesianConvexHullProperty(Blackhole bh) {
        try {
            try (Transaction tx = db.beginTx()) {
                for (Node osmRelation : nodes) {
                    MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);

                    bh.consume(CartesianConvexHull.convexHull(polygon));
                }
                tx.success();
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Benchmark
    public void testGeographicConvexHullProperty(Blackhole bh) {
        try (Transaction tx = db.beginTx()) {
            for (Node osmRelation : nodes) {
                MultiPolygon polygon = UserDefinedFunctions.getArrayPolygon(osmRelation);

                bh.consume(WGS84ConvexHull.convexHull(polygon));
            }
            tx.success();
        }
    }
}
