package org.neo4j.spatial.algo.wsg84;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public interface Intersect {
    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    static Point intersect(LineSegment a, LineSegment b) {
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

        if (mid.dot(i1) > 0) {
            return i1.toPoint();
        } else {
            return i2.toPoint();
        }
    }
}
