package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public class WGSUtil {
    public final static Vector NORTH_POLE = new Vector(0, 0, 1);
    public final static double RADIUS = 6371e3;;

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

        double u1u2Distance = distance(u1, u2);
        double v1v2Distance = distance(v1, v2);
        if (mid.dot(i1) > 0) {
            if (u1u2Distance >= distance(u1, i1) && u1u2Distance >= distance(u2, i1)
                    && v1v2Distance >= distance(v1, i1) && v1v2Distance >= distance(v2, i1)) {
                return i1.toPoint();
            }
        } else {
            if (u1u2Distance >= distance(u1, i2) && u1u2Distance >= distance(u2, i2)
                    && v1v2Distance >= distance(v1, i2) && v1v2Distance >= distance(v2, i2)) {
                return i2.toPoint();
            }
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
