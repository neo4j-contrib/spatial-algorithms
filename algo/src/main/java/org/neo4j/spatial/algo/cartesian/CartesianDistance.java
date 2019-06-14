package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

import static java.lang.String.format;

public class CartesianDistance extends Distance {
    public double distance(Polygon a, Polygon b) {
        boolean intersects = new CartesianMCSweepLineIntersect().doesIntersect(a, b);

        //Check if one polygon is (partially) contained by the other
        if (intersects) {
            return 0;
        } else  if (CartesianWithin.within(a, b.getShells()[0].getPoints()[0]) || CartesianWithin.within(b, a.getShells()[0].getPoints()[0])) {
            return 0;
        }

        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        double minDistance = getMinDistance(aLS, bLS);

        return minDistance;
    }

    @Override
    public double distance(Polygon polygon, LineSegment lineSegment) {
        LineSegment[] lineSegments = polygon.toLineSegments();

        double minDistance = Double.MAX_VALUE;

        for (LineSegment currentLineSegment : lineSegments) {
            double current = distance(currentLineSegment, lineSegment);

            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    @Override
    public double distance(Polygon polygon, Point point) {
        if (CartesianWithin.within(polygon, point)) {
            return 0;
        }

        LineSegment[] lineSegments = polygon.toLineSegments();

        double minDistance = Double.MAX_VALUE;

        for (LineSegment lineSegment : lineSegments) {
            double current = distance(lineSegment, point);

            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    @Override
    public double distance(Polygon polygon, Polyline polyline) {
        boolean intersects = new CartesianMCSweepLineIntersect().doesIntersect(polygon, polyline);

        //Check if one polygon is (partially) contained by the other
        if (intersects) {
            return 0;
        } else  if (CartesianWithin.within(polygon, polyline.getPoints()[0])) {
            return 0;
        }

        LineSegment[] aLS = polygon.toLineSegments();
        LineSegment[] bLS = polyline.toLineSegments();

        double minDistance = getMinDistance(aLS, bLS);

        return minDistance;
    }

    @Override
    public double distance(Polyline a, Polyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        double minDistance = getMinDistance(aLS, bLS);

        return minDistance;
    }

    @Override
    public double distance(Polyline polyline, LineSegment lineSegment) {
        double minDistance = Double.MAX_VALUE;

        LineSegment[] aLS = polyline.toLineSegments();

        for (LineSegment aLineSegment : aLS) {
            double current = distance(aLineSegment, lineSegment);
            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    @Override
    public double distance(Polyline polyline, Point point) {
        double minDistance = Double.MAX_VALUE;

        LineSegment[] aLS = polyline.toLineSegments();

        for (LineSegment aLineSegment : aLS) {
            double current = distance(aLineSegment, point);
            if (current < minDistance) {
                minDistance = current;
            }
        }

        return minDistance;
    }

    @Override
    public double distance(LineSegment lineSegment, Point point) {
        Point u = lineSegment.getPoints()[0];
        Point v = lineSegment.getPoints()[1];
        double[] a = new double[]{
                v.getCoordinate()[0] - u.getCoordinate()[0],
                v.getCoordinate()[1] - u.getCoordinate()[1],
        };
        double[] b = new double[]{
                point.getCoordinate()[0] - u.getCoordinate()[0],
                point.getCoordinate()[1] - u.getCoordinate()[1],
        };

        double dotProduct = AlgoUtil.dotProduct(a, b);
        double lengthSquared = a[0] * a[0] + a[1] * a[1];

        double t = Math.max(0, Math.min(1, dotProduct/lengthSquared));

        Point projection = v.subtract(u.getCoordinate()).multiply(t).add(u.getCoordinate());

        return distance(projection, point);
    }

    @Override
    public double distance(LineSegment a, LineSegment b) {
        Point intersect = CartesianIntersect.lineSegmentIntersect(a, b);
        if (intersect != null) {
            return 0;
        }

        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < a.getPoints().length; i++) {
            double distance = distance(b, a.getPoints()[i]);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        for (int i = 0; i < b.getPoints().length; i++) {
            double distance = distance(a, b.getPoints()[i]);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }

    @Override
    public double distance(Point p1, Point p2) {
        double[] c1 = p1.getCoordinate();
        double[] c2 = p2.getCoordinate();
        return CartesianUtil.distance(c1, c2);
    }
}
