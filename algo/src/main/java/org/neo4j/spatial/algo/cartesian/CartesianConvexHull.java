package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CartesianConvexHull {
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
     * Computes the convex hull of a set of coordinates using Graham's scan
     *
     * @param coordinates of points
     * @return Ordered list of indices of the input which together form the convex hull
     */
    public static int[] convexHullByIndex(double[][] coordinates) {
        double[] reference = getLowestCoordinate(coordinates);
        List<Integer> sortedPoints = sortIndices(coordinates, reference);

        Stack<Integer> stack = new Stack<>();

        for (int current : sortedPoints) {
            //Remove last point from the stack if we make a clockwise turn (that point makes the hull concave)
            while (stack.size() > 1 && AlgoUtil.ccw(coordinates[stack.get(stack.size()-2)], coordinates[stack.peek()], coordinates[current]) <= 0) {
                stack.pop();

            }
            stack.push(current);
        }

        return stack.stream().mapToInt(i -> i).toArray();
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
        sortedPoints.sort((a, b) -> comparePoints(reference.getCoordinate(), a.getCoordinate(), b.getCoordinate()));

        //Remove points with same polar angle but shorter distance to reference
        List<Integer> toDelete = new ArrayList<>();
        for (int i = 1; i < sortedPoints.size() - 1; i++) {
            Point a = sortedPoints.get(i);
            Point b = sortedPoints.get(i+1);

            double angleA = getPolarAngle(reference.getCoordinate(), a.getCoordinate());
            double angleB = getPolarAngle(reference.getCoordinate(), b.getCoordinate());
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
     * Sorts the points based on their polar angle with respect to the reference point.
     * Ties are broken based on the distance to the reference point
     *
     * @param coordinates
     * @param reference
     * @return Sorted list of the indices of the array of input coordinates
     */
    private static List<Integer> sortIndices(double[][] coordinates, double[] reference) {
        List<Integer> sortedIndices = IntStream.range(0, coordinates.length).boxed().collect(Collectors.toList());;
        sortedIndices.sort((a, b) -> comparePoints(reference, coordinates[a], coordinates[b]));

        //Remove points with same polar angle but shorter distance to reference
        List<Integer> toDelete = new ArrayList<>();
        for (int i = 1; i < sortedIndices.size() - 1; i++) {
            int a = sortedIndices.get(i);
            int b = sortedIndices.get(i+1);

            double angleA = getPolarAngle(reference, coordinates[a]);
            double angleB = getPolarAngle(reference, coordinates[b]);
            if (AlgoUtil.equal(angleA, angleB)) {
                toDelete.add(i - toDelete.size());
            }
        }
        for (Integer index: toDelete) {
            sortedIndices.remove((int) index);
        }
        return sortedIndices;
    }

    /**
     * Returns the point with lowest y-value. If multiple points have the same y-value, return the one of those points with the lowest x-value
     *
     * @param inputPoints the array of points from which we will pick the lowest point
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

    // TODO: Reduce duplicated code
    private static double[] getLowestCoordinate(double[][] inputCoordinates) {
        double[] outer = inputCoordinates[0];

        for (double[] coordinate : inputCoordinates) {
            if (coordinate[1] < outer[1]) {
                outer = coordinate;
            } else if (coordinate[1] == outer[1]) {
                if (coordinate[0] < outer[0]) {
                    outer = coordinate;
                }
            }
        }

        return outer;
    }

    private static int comparePoints(double[] reference, double[] a, double[] b) {
        if (Arrays.equals(a, b)) {
            return 0;
        }

        double angleA = getPolarAngle(reference, a);
        double angleB = getPolarAngle(reference, b);

        Distance calculator = DistanceCalculator.getCalculator(CRS.Cartesian);
        if (AlgoUtil.equal(angleA, angleB)) {
            return Double.compare(calculator.distance(reference, a), calculator.distance(reference, b));
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
    private static double getPolarAngle(double[] reference, double[] a) {
        return Math.atan2(a[1] - reference[1], a[0] - reference[0]);
    }
}
