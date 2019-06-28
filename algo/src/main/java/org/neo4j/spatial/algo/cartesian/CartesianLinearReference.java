package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.LinearReference;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;

public class CartesianLinearReference extends LinearReference {
    @Override
    protected CRS getCRS() {
        return CRS.Cartesian;
    }

    public Point reference(LineSegment lineSegment, double d) {
        return reference(lineSegment.getPoints()[0], lineSegment.getPoints()[1], d);
    }

    @Override
    protected Point reference(Point a, Point b, double d) {
        if (d < 0) {
            return null;
        }

        double[] p = a.getCoordinate();
        double[] q = b.getCoordinate();
        double length = CartesianUtil.distance(p, q);

        if (length < d) {
            return null;
        }

        double fraction = d / length;

        return Point.point(CRS.Cartesian, p[0] + fraction * (q[0] - p[0]), p[1] + fraction * (q[1] - p[1]));
    }
}
