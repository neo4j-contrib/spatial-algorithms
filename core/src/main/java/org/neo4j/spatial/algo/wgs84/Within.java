package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;

public class Within {

    public static boolean within(Polygon polygon, Point point) {
        return Arrays.stream(polygon.getShells()).filter(s -> within(s, point)).count() > Arrays.stream(polygon.getHoles()).filter(h -> within(h, point)).count();
    }

    public static boolean within(Polygon.SimplePolygon polygon, Point point) {
        Point[] points = polygon.getPoints();

        double sum = 0;
        double previous = 0;
        boolean first = true;
        for (int i = 0; i < points.length - 1; i++) {
            int j = (i + 1) % (points.length - 1);

            double initialBearing = WGSUtil.initialBearing(points[i], points[j]);
            double finalBearing = WGSUtil.finalBearing(points[i], points[j]);

            if (first) {
                first = false;
            } else {
                sum = sum + angleDelta(initialBearing, previous);
            }

            sum = sum + angleDelta(finalBearing, initialBearing);

            previous = finalBearing;

        }
        double initialBearing = WGSUtil.initialBearing(points[0], points[1]);
        sum = sum + angleDelta(initialBearing, previous);

        boolean result = false;
        if (sum > 270) {
            //The polygon does not contain a pole
            for (int i = 0; i < points.length - 1; i++) {
                int j = i + 1;
                boolean cond1 = (points[i].getCoordinate()[0] <= point.getCoordinate()[0]) && (point.getCoordinate()[0] < points[j].getCoordinate()[0]);
                boolean cond2 = (points[j].getCoordinate()[0] <= point.getCoordinate()[0]) && (point.getCoordinate()[0] < points[i].getCoordinate()[0]);
                if (cond1 || cond2) {
                    double vt = (point.getCoordinate()[0]  - points[i].getCoordinate()[0]) / (points[j].getCoordinate()[0] - points[i].getCoordinate()[0]);
                    if (point.getCoordinate()[1] <  points[i].getCoordinate()[1] + vt * (points[j].getCoordinate()[1] - points[i].getCoordinate()[1])) {
                        result = !result;
                    }
                }
            }
        } else {
            //The polygon contains at least one pole
            throw new IllegalArgumentException("Polygon contains at least one pole");
        }
        return result;
    }

    private static double angleDelta(double a, double b) {
        if (b < a) {
            b += 360;
        }

        double result = b - a;

        if (result > 180) {
            result -= 360;
        }

        return result;
    }
}
