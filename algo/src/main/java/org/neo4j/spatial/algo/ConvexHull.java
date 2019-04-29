package org.neo4j.spatial.algo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class ConvexHull {
    /**
     * Computes the convex hull of a polygon using Graham's scan
     *
     * @param polygon
     * @return A polygon which is the convex hull of the input polygon
     */
    public static Polygon.SimplePolygon convexHull(Polygon polygon) {
        Point[] polygonPoints = polygon.getPoints();
        Point reference = getLowestPoint(polygonPoints);

        List<Point> sortedPoints = sortPoints(polygonPoints, reference);

        Stack<Point> stack = new Stack<>();

        for (Point point : sortedPoints) {
            //Remove last point from the stack if we make a clockwise turn (that point makes the hull concave)
            while (stack.size() > 1 && ccw(stack.get(stack.size()-2), stack.peek(), point) <= 0) {
                stack.pop();
            }
            stack.push(point);
        }

        return Polygon.simple(stack.toArray(new Point[0]));
    }

    /**
     * Sorts the points based on their polar angle with respect to the reference point.
     * Ties are broken based on the distance to the reference point
     *
     * @param points
     * @param reference
     * @return Sorted list of points
     */
    private static List<Point> sortPoints(Point[] points, Point reference) {
        List<Point> sortedPoints = new ArrayList<>(Arrays.asList(points));
        sortedPoints.sort((a, b) -> comparePoints(reference, a, b));

        //Remove points with same polar angle but shorter distance to reference
        List<Integer> toDelete = new ArrayList<>();
        for (int i = 1; i < sortedPoints.size() - 1; i++) {
            Point a = sortedPoints.get(i);
            Point b = sortedPoints.get(i+1);

            double angleA = getPolarAngle(reference, a);
            double angleB = getPolarAngle(reference, b);
            if (angleA == angleB) {
                toDelete.add(i - toDelete.size());
            }
        }
        for (Integer index: toDelete) {
            sortedPoints.remove((int) index);
        }
        return sortedPoints;
    }

    /**
     * Computes the direction of the of the a-b-c turn by computing the z-component of ab x ac
     *
     * @param a starting point
     * @param b turning point
     * @param c ending point
     * @return Integer value with the following property:
     *          ccw < 0: clockwise turn;
     *          ccw = 0: collinear;
     *          ccw > 1: counterclockwise turn
     */
    private static int ccw(Point a, Point b, Point c) {
        double z = (b.getCoordinate()[0] - a.getCoordinate()[0]) * (c.getCoordinate()[1] - a.getCoordinate()[1]) - (b.getCoordinate()[1] - a.getCoordinate()[1]) * (c.getCoordinate()[0] - a.getCoordinate()[0]);
        return z == 0 ? 0 : (z < 0 ? -1 : 1);
    }

    /**
     * Returns the point with lowest y-value. If multiple points have the same y-value, return the one of those points with the lowest x-value
     *
     * @param inputPoints the array of points from which we will pick the lowest poin
     * @return Point with lowest y-value (and lowest x-value of the points with the same y-value)
     */
    private static Point getLowestPoint(Point[] inputPoints) {
        Point outer = inputPoints[0];

        for (Point point : inputPoints) {
            if (point.getCoordinate()[1] < outer.getCoordinate()[1]) {
                outer = point;
            } else if (point.getCoordinate()[1] == outer.getCoordinate()[1]) {
                if (point.getCoordinate()[0] < outer.getCoordinate()[0]) {
                    outer = point;
                }
            }
        }

        return outer;
    }

    private static int comparePoints(Point reference, Point a, Point b) {
        if (a.equals(b)) {
            return 0;
        }

        double angleA = getPolarAngle(reference, a);
        double angleB = getPolarAngle(reference, b);

        if (angleA == angleB) {
            return Double.compare(Distance.distance(reference, a), Distance.distance(reference, b));
        }

        return Double.compare(angleA, angleB);
    }

    /**
     * Computes the polar angle for point a with respect to the reference point
     *
     * @param reference the reference point which will act as the origin
     * @param a         the point for which we want to know the polar angle
     * @return The polar angle of a with respect to the reference point
     */
    private static double getPolarAngle(Point reference, Point a) {
        return Math.atan2(a.getCoordinate()[1] - reference.getCoordinate()[1], a.getCoordinate()[0] - reference.getCoordinate()[0]);
    }
}
