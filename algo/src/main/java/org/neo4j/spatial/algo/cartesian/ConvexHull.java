package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class ConvexHull {
    /**
     * Computes the convex hull of a multipolygon using Graham's scan
     *
     * @param polygon
     * @return A polygon which is the convex hull of the input polygon
     */
    public static Polygon.SimplePolygon convexHull(MultiPolygon polygon) {
        Polygon.SimplePolygon[] convexHulls = new Polygon.SimplePolygon[polygon.getChildren().size()];

        for (int i = 0; i < polygon.getChildren().size(); i++) {
            convexHulls[i] = convexHull(polygon.getChildren().get(i).getPolygon());
        }

        return convexHull(Stream.of(convexHulls).map(Polygon.SimplePolygon::getPoints).flatMap(Stream::of).toArray(Point[]::new));
    }
    /**
     * Computes the convex hull of a polyline polygon using Graham's scan
     *
     * @param polygon
     * @return A polygon which is the convex hull of the input polygon
     */
    public static Polygon.SimplePolygon convexHull(Polygon.SimplePolygon polygon) {
        return convexHull(polygon.getPoints());
    }

    /**
     * Computes the convex hull of a set of points using Graham's scan
     *
     * @param points
     * @return A polygon which is the convex hull of the input points
     */
    public static Polygon.SimplePolygon convexHull(Point[] points) {
        Point reference = getLowestPoint(points);
        List<Point> sortedPoints = sortPoints(points, reference);

        Stack<Point> stack = new Stack<>();

        for (Point point : sortedPoints) {
            //Remove last point from the stack if we make a clockwise turn (that point makes the hull concave)
            while (stack.size() > 1 && AlgoUtil.ccw(stack.get(stack.size()-2), stack.peek(), point) <= 0) {
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
            if (AlgoUtil.equal(angleA, angleB)) {
                toDelete.add(i - toDelete.size());
            }
        }
        for (Integer index: toDelete) {
            sortedPoints.remove((int) index);
        }
        return sortedPoints;
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

        if (AlgoUtil.equal(angleA, angleB)) {
            return Double.compare(DistanceCalculator.distance(reference, a), DistanceCalculator.distance(reference, b));
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
