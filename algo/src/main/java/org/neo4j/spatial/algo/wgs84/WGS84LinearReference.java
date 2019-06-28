package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Vector;

public class WGS84LinearReference extends LinearReference {
    @Override
    protected CRS getCRS() {
        return CRS.WGS84;
    }

    @Override
    public Point reference(LineSegment lineSegment, double d) {
        return reference(lineSegment.getPoints()[0], lineSegment.getPoints()[1], d);
    }

    @Override
    protected Point reference(Point a, Point b, double d) {
        if (d < 0) {
            return null;
        }

        Distance calculator = DistanceCalculator.getCalculator(CRS.WGS84);

        double length = calculator.distance(a, b);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        Vector u = new Vector(a);
        Vector v = new Vector(b);

        // Angular distance between the two points
        double sinTheta = u.cross(v).magnitude();
        double cosTheta = u.dot(v);
        double delta = Math.atan2(sinTheta, cosTheta);

        // Interpolate the angular distance on straight line between the two points
        double deltaInter = delta * fraction;
        double sinDeltaInter = Math.sin(deltaInter);
        double cosDeltaInter = Math.cos(deltaInter);

        // The direction vector (perpendicular to u in plane of v)
        Vector direction = u.cross(v).normalize().cross(u); // unit(u x v) x u

        // The interpolated position
        Vector inter = u.multiply(cosDeltaInter).add(direction.multiply(sinDeltaInter)); // u * cosDeltaInter + d * sinDeltaInter

        return inter.toPoint();
    }
}
