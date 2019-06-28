package org.neo4j.spatial.benchmarks.micro;

import org.neo4j.spatial.algo.Area;
import org.neo4j.spatial.algo.AreaCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class AreaBenchmarks {

    private Polygon.SimplePolygon[] polygons;
    private Area geographicCalculator = AreaCalculator.getCalculator(CRS.WGS84);
    private Area cartesianCalculator = AreaCalculator.getCalculator(CRS.Cartesian);

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AreaBenchmarks.class.getSimpleName())
                .forks(0)
                .mode(Mode.SingleShotTime)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        int n_US = 1000;
        int n_EU = 1000;
        int n_OZ = 1000;

        Random random = new Random(0);
        polygons = new Polygon.SimplePolygon[n_US + n_EU + n_OZ];

        Point originUS = Point.point(CRS.WGS84, -122.31, 37.56);    // San Francisco
        for (int i = 0; i < n_US; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originUS, 0.1, 1.0, 0.1, 1.1).first();
        }
        Point originEU = Point.point(CRS.WGS84, 12.99, 55.61);      // Malmo (Neo4j)
        for (int i = n_US; i < n_US + n_EU; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originEU, 0.1, 1.0, 0.1, 1.1).first();
        }
        Point originOZ = Point.point(CRS.WGS84, 151.17, -33.90);    // Sydney
        for (int i = n_US + n_EU; i < n_US + n_EU + n_OZ; i++) {
            polygons[i] = MicroBenchmarkUtil.createPolygon(random, originOZ, 0.1, 1.0, 0.1, 1.1).first();
        }
    }

    @Benchmark
    public void testCartesianArea(Blackhole bh) {
        for (Polygon.SimplePolygon polygon : polygons) {
            bh.consume(cartesianCalculator.area(polygon));
        }
    }

    @Benchmark
    public void testGeographicArea(Blackhole bh) {
        for (Polygon.SimplePolygon polygon : polygons) {
            bh.consume(geographicCalculator.area(polygon));
        }
    }
}
