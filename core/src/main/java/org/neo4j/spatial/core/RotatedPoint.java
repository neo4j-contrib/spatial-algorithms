package org.neo4j.spatial.core;

import org.neo4j.spatial.algo.AlgoUtil;

import java.util.Arrays;

import static java.lang.String.format;

public class RotatedPoint implements Point {
    private final Point point;
    private final double angle;

    public RotatedPoint(Point point, double angle) {
        this.point = point;
        this.angle = angle;
    }

    @Override
    public CRS getCRS() {
        return this.point.getCRS();
    }

    @Override
    public double[] getCoordinate() {
        return AlgoUtil.rotate(point, angle);
    }

    @Override
    public String toString() {
        return format("InMemoryPoint%s", Arrays.toString(getCoordinate()));
    }
}