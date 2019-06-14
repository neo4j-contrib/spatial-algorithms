package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

public class WGSUtil {
    public final static Vector NORTH_POLE = new Vector(0, 0, 1);
    public final static double RADIUS = 6371e3;;

    public static double initialBearing(Point start, Point end) {
        Vector a = new Vector(start);
        Vector b = new Vector(end);

        Vector c1 = a.cross(b);
        Vector c2 = a.cross(NORTH_POLE);

        double angle = angleTo(c1, a, c2);

        return (angle * 180) / Math.PI;
    }

    public static double angleTo(Vector c1, Vector p, Vector c2) {
        double sign = Math.signum(c1.cross(c2).dot(p));
        double sinTheta = c1.cross(c2).magnitude() * sign;
        double cosTheta = c1.dot(c2);
        return Math.atan2(sinTheta, cosTheta);
    }

    public static double finalBearing(Point start, Point end) {
        return (WGSUtil.initialBearing(end, start) + 180) % 360;
    }

    public static double courseDelta(Point[] points) {
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
        return sum + angleDelta(initialBearing, previous);
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

    public static Point mean(Point... points) {
        Vector mean = new Vector(0, 0, 0);
        for (Point p : points) {
            Vector v = new Vector(p);

            mean = mean.add(v);
        }

        return mean.normalize().toPoint();
    }

    public static Point intersect(LineSegment a, LineSegment b) {
        Vector u1 = new Vector(a.getPoints()[0]);
        Vector u2 = new Vector(a.getPoints()[1]);
        Vector v1 = new Vector(b.getPoints()[0]);
        Vector v2 = new Vector(b.getPoints()[1]);

        //Great circles
        Vector gc1 = u1.cross(u2);
        Vector gc2 = v1.cross(v2);

        //Intersection
        Vector i1 = gc1.cross(gc2);
        Vector i2 = gc2.cross(gc1);

        Vector mid = u1.add(u2).add(v1).add(v2);

        Vector intersect;
        double u1u2Distance = distance(u1, u2);
        double v1v2Distance = distance(v1, v2);
        if (mid.dot(i1) > 0) {
            intersect = i1;
        } else {
            intersect = i2;
        }

        if (AlgoUtil.lessOrEqual(distance(u1, intersect), u1u2Distance) && AlgoUtil.lessOrEqual(distance(u2, intersect), u1u2Distance)
                && AlgoUtil.lessOrEqual(distance(v1, intersect), v1v2Distance) && AlgoUtil.lessOrEqual(distance(v2, intersect), v1v2Distance)) {
            return intersect.toPoint();
        }

        return null;
    }

    /**
     * @param u
     * @param v
     * @return The minimum distance between two vectors representing points
     */
    public static double distance(Vector u, Vector v) {
        //Distance (in meters)
        return WGSUtil.RADIUS * Math.atan2(u.cross(v).magnitude(), u.dot(v));
    }
}
