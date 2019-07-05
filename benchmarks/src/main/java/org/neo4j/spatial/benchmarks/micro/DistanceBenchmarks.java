package org.neo4j.spatial.benchmarks.micro;

import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.cartesian.CartesianDistance;
import org.neo4j.spatial.algo.wgs84.WGS84Distance;
import org.neo4j.spatial.benchmarks.JfrProfiler;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
public class DistanceBenchmarks {
    private Distance cartesianCalculator = new CartesianDistance();
    private Distance geographicCalculator = new WGS84Distance();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DistanceBenchmarks.class.getSimpleName())
                .forks(1)
                .addProfiler(JfrProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testCartesianDistance(Blackhole bh) {
        for (double x = -1000.0; x < 1000.0; x += 1.0) {
            for (double y = -1000.0; y < 1000.0; y += 1.0) {
                bh.consume(cartesianCalculator.distance(Point.point(CRS.Cartesian, x, y), Point.point(CRS.Cartesian, -x, -y)));
            }
        }
    }

    @Benchmark
    public void testGeographicDistance(Blackhole bh) {
        for (double x = -10.0; x < 10.0; x += 0.01) {
            for (double y = -10.0; y < 10.0; y += 0.01) {
                bh.consume(geographicCalculator.distance(Point.point(CRS.WGS84, x, y), Point.point(CRS.WGS84, -x, -y)));
            }
        }
    }
}
