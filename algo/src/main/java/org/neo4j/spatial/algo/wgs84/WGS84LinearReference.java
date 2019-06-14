package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public class WGS84LinearReference extends LinearReference {
    @Override
    public Point reference(LineSegment lineSegment, double d) {
        return reference(lineSegment.getPoints()[0], lineSegment.getPoints()[1], d);
    }

    @Override
    protected Point reference(Point a, Point b, double d) {
        if (d < 0) {
            return null;
        }

        double length = DistanceCalculator.distance(a, b);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        Vector u = new Vector(a);
        Vector v = new Vector(b);

        return u.add(v.subtract(u).multiply(fraction)).toPoint();
    }
}
