package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public class WGS84LinearReference extends LinearReference {
    @Override
    public Point reference(LineSegment lineSegment, double d) {
        if (d < 0) {
            return null;
        }

        Point p = lineSegment.getPoints()[0];
        Point q = lineSegment.getPoints()[1];
        double length = DistanceCalculator.distance(p, q);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        Vector u = new Vector(p);
        Vector v = new Vector(q);

        return u.add(v.subtract(u).multiply(fraction)).toPoint();
    }
}
