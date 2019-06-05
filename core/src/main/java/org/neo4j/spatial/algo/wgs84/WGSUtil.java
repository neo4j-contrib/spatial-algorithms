package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public class WGSUtil {
    public final static Vector NORTH_POLE = new Vector(0, 0, 1);

    public static double initialBearing(Point start, Point end) {
        Vector a = new Vector(start);
        Vector b = new Vector(end);

        Vector c1 = a.cross(b);
        Vector c2 = a.cross(NORTH_POLE);

        double sign = Math.signum(c1.cross(c2).dot(a));
        double sinTheta = c1.cross(c2).magnitude() * sign;
        double cosTheta = c1.dot(c2);
        double angle = Math.atan2(sinTheta, cosTheta);

        return (angle * 180) / Math.PI;
    }

    public static double finalBearing(Point start, Point end) {
        return (WGSUtil.initialBearing(end, start) + 180) % 360;
    }

    public static Point mean(Point... points) {
        Vector mean = new Vector(0, 0, 0);
        for (Point p : points) {
            Vector v = new Vector(p);

            mean = mean.add(v);
        }

        return mean.normalize().toPoint();
    }
}
