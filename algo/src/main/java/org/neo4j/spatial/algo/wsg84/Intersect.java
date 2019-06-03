package org.neo4j.spatial.algo.wsg84;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

import java.util.Arrays;

public interface Intersect {
    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    static Point intersect(LineSegment a, LineSegment b) {
        //To radians
        double[] a1 = Arrays.stream(a.getPoints()[0].getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] a2 = Arrays.stream(a.getPoints()[1].getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] b1 = Arrays.stream(b.getPoints()[0].getCoordinate()).map(p -> p * Math.PI/180).toArray();
        double[] b2 = Arrays.stream(b.getPoints()[1].getCoordinate()).map(p -> p * Math.PI/180).toArray();

        //To n-vector
        Vector u1 = new Vector(Math.cos(a1[1]) * Math.cos(a1[0]), Math.cos(a1[1]) * Math.sin(a1[0]), Math.sin(a1[1]));
        Vector u2 = new Vector(Math.cos(a2[1]) * Math.cos(a2[0]), Math.cos(a2[1]) * Math.sin(a2[0]), Math.sin(a2[1]));
        Vector v1 = new Vector(Math.cos(b1[1]) * Math.cos(b1[0]), Math.cos(b1[1]) * Math.sin(b1[0]), Math.sin(b1[1]));
        Vector v2 = new Vector(Math.cos(b2[1]) * Math.cos(b2[0]), Math.cos(b2[1]) * Math.sin(b2[0]), Math.sin(b2[1]));

        //Great circles
        Vector gc1 = u1.cross(u2);
        Vector gc2 = v1.cross(v2);

        //Intersection
        Vector i1 = gc1.cross(gc2);
        Vector i2 = gc2.cross(gc1);

        Vector mid = u1.add(u2).add(v1).add(v2);

        if (mid.dot(i1) > 0) {
            return Point.point(Math.atan2(i1.getCoordinate(1), i1.getCoordinate(0)) * 180 / Math.PI, Math.atan2(i1.getCoordinate(2), Math.sqrt(Math.pow(i1.getCoordinate(0), 2) + Math.pow(i1.getCoordinate(1), 2))) * 180 / Math.PI);
        } else {
            return Point.point(Math.atan2(i2.getCoordinate(1), i2.getCoordinate(0)) * 180 / Math.PI, Math.atan2(i2.getCoordinate(2), Math.sqrt(Math.pow(i2.getCoordinate(0), 2) + Math.pow(i2.getCoordinate(1), 2))) * 180 / Math.PI);
        }
    }
}
