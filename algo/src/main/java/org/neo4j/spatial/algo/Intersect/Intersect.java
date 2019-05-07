package org.neo4j.spatial.algo.Intersect;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.core.Line;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public interface Intersect {
    /**
     * Given two simple polygons, returns all points for which the two polygons intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    Point[] intersect(Polygon.SimplePolygon a, Polygon.SimplePolygon b);

    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    static Point intersect(LineSegment a, LineSegment b) {
        Point shared = LineSegment.sharedPoint(a, b);
        if (shared != null) {
            return shared;
        }

        Line l1 = new Line(a);
        Line l2 = new Line(b);

        Point a0 = a.getPoints()[0];
        Point a1 = a.getPoints()[1];
        Point b0 = b.getPoints()[0];
        Point b1 = b.getPoints()[1];

        //Two vertical line segments only intersect if they overlap
        if (l1.isVertical() && l2.isVertical()) {
            if (a0.getCoordinate()[0] == b0.getCoordinate()[0]) {
                Double y = overlaps(
                        new double[]{a0.getCoordinate()[1], a1.getCoordinate()[1]},
                        new double[]{b0.getCoordinate()[1], b1.getCoordinate()[1]
                        });
                if (y != null) {
                    return Point.point(a0.getCoordinate()[0], y);
                }
            }
            return null;
        }

        if (l1.isVertical()) {
            return intersectionWithVertical(a0, a1, l2, b0, b1);
        } else if (l2.isVertical()) {
            return intersectionWithVertical(b0, b1, l1, a0, a1);
        }

        if (l1.getSign(b0) * l1.getSign(b1) > 0 && l2.getSign(a0) * l2.getSign(a1) > 0) {
            return null;
        }

        //Two line segments with the same slope only intersect if they have the same offset and overlap (in one point)
        if (AlgoUtil.equal(l1.getA(), l2.getA())) {
            if (l1.getB() != l2.getB()) {
                return null;
            }

            Double x = overlaps(
                    new double[]{a0.getCoordinate()[0], a1.getCoordinate()[0]},
                    new double[]{b0.getCoordinate()[0], b1.getCoordinate()[0]});
            Double y = overlaps(
                    new double[]{a0.getCoordinate()[1], a1.getCoordinate()[1]},
                    new double[]{b0.getCoordinate()[1], b1.getCoordinate()[1]});

            return Point.point(x, y);
        }

        double[] coordinates = new double[2];

        coordinates[0] = (l2.getB() - l1.getB())/(l1.getA() - l2.getA());
        coordinates[1] = l1.getY(coordinates[0]);

        for (int i = 0; i < 2; i++) {
            if (!inInterval(new double[]{a0.getCoordinate()[i], a1.getCoordinate()[i]}, coordinates[i])) {
                return null;
            }
            if (!inInterval(new double[]{b0.getCoordinate()[i], b1.getCoordinate()[i]}, coordinates[i])) {
                return null;
            }
        }

        return Point.point(coordinates);
    }


    /**
     * Computes the intersection of one vertical and one non-vertical line segment
     *
     * @param v0 The first point of the vertical line segment
     * @param v1 The second point of the vertical line segment
     * @param line The line extending from the non-vertical line segment
     * @param other0 The first point of the other line segment
     * @param other1 The second point of the other line segment
     * @return The point of intersection, or null if it does not exist
     */
    static Point intersectionWithVertical(Point v0, Point v1, Line line, Point other0, Point other1) {
        double x = v0.getCoordinate()[0];
        double y = line.getY(x);

        if (!inInterval(new double[]{other0.getCoordinate()[0], other1.getCoordinate()[0]}, x)) {
            return null;
        }

        if (!inInterval(new double[]{other0.getCoordinate()[1], other1.getCoordinate()[1]}, y)) {
            return null;
        }

        if (!inInterval(new double[]{v0.getCoordinate()[1], v1.getCoordinate()[1]}, y)) {
            return null;
        }


        return Point.point(x, y);
    }

    /**
     * Takes two intervals and returns the lowest value for which they overlap, otherwise returns null.
     * The two values inside the interval do not have to be sorted.
     *
     * @param inter1 Array of size two
     * @param inter2 Array of size two
     * @return The lowest value the two intervals have in common, otherwise null
     */
    static Double overlaps(double[] inter1, double[] inter2) {
        double lowest1;
        double lowest2;
        double highest1;
        double highest2;

        if (inter1[0] < inter1[1]) {
            lowest1 = inter1[0];
            highest1 = inter1[1];
        } else {
            lowest1 = inter1[1];
            highest1 = inter1[0];
        }

        if (inter2[0] < inter2[1]) {
            lowest2 = inter2[0];
            highest2 = inter2[1];
        } else {
            lowest2 = inter2[1];
            highest2 = inter2[0];
        }

        if (lowest1 < lowest2) {
            if (highest1 >= lowest2) {
                return lowest2;
            }
        } else {
            if (highest2 >= lowest1) {
                return lowest1;
            }
        }

        return null;
    }

    /**
     * Checks whether the value is in the given interval
     *
     * @param inter The (unordered) interval of size 2
     * @param value The value to be tested
     * @return True iff value is lower or equal than one of values of interval and higher or equal than other
     */
    static boolean inInterval(double[] inter, double value) {
        double lowest;
        double highest;

        if (inter[0] < inter[1]) {
            lowest = inter[0];
            highest = inter[1];
        } else {
            lowest = inter[1];
            highest = inter[0];
        }

        if (AlgoUtil.lessOrEqual(lowest, value) && AlgoUtil.lessOrEqual(value, highest)) {
            return true;
        }

        return false;
    }
}