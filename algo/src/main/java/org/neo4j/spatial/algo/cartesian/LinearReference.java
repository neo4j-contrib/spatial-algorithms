package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

public class LinearReference extends org.neo4j.spatial.algo.LinearReference {
    public Point reference(LineSegment lineSegment, double d) {
        if (d < 0) {
            return null;
        }

        double[] p = lineSegment.getPoints()[0].getCoordinate();
        double[] q = lineSegment.getPoints()[1].getCoordinate();
        double length = Distance.distance(p, q);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        return Point.point(CRS.Cartesian, p[0] + fraction * (q[0] - p[0]), p[1] + fraction * (q[1] - p[1]));
    }
}
