package org.neo4j.spatial.benchmarks.micro;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MicroBenchmarkUtil {

    static Polygon.SimplePolygon createPolygon(Random random, Point origin, double minAngleStep, double maxAngleStep, double minRadius, double maxRadius) {
        List<Point> points = new ArrayList<>();
        double originX = origin.getCoordinate()[0] + maxRadius * (2 * random.nextDouble() - 1);
        double originY = origin.getCoordinate()[1] + maxRadius * (2 * random.nextDouble() - 1);
        double wobbleA = 2.0 + random.nextInt(10);
        double wobbleB = 1.0 + random.nextInt(5);
        for (double angle = 0; angle < 360.0; angle += minAngleStep + random.nextDouble() * (maxAngleStep - minAngleStep)) {
            double radians = Math.PI * (90.0 - angle) / 180.0;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius) + (Math.cos(radians * wobbleA)) / 10.0 + (Math.cos(radians * wobbleB)) / 5.0;
            double x = radius * Math.cos(radians);
            double y = radius * Math.sin(radians);
            Point point = Point.point(origin.getCRS(), originX + x, originY + y);
            points.add(point);
        }

        return Polygon.simple(points.toArray(new Point[0]));
    }
}