package org.amanzi.spatial.algo;

import org.amanzi.spatial.core.Point;
import org.amanzi.spatial.core.Polygon;

import java.util.ArrayList;

public class Within {
    public static boolean within(Polygon polygon, Point point) {
        return within(polygon, point, false);
    }

    public static boolean within(Polygon polygon, Point point, boolean touching) {
        for (Polygon.SimplePolygon shell : polygon.getShells()) {
            if (!within(shell, point, touching)) {
                return false;
            }
        }
        for (Polygon.SimplePolygon hole : polygon.getHoles()) {
            if (within(hole, point, touching)) {
                return false;
            }
        }
        return true;
    }

    public static boolean within(Polygon.SimplePolygon shell, Point point) {
        return within(shell, point, false);
    }

    public static boolean within(Polygon.SimplePolygon shell, Point point, boolean touching) {
        int fixedDim = 0;
        int compareDim = 1;
        Point[] points = shell.getPoints();
        ArrayList<Point[]> sides = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            Point p1 = points[i];
            Point p2 = points[i + 1];
            Integer compare1 = ternaryComparePointsIgnoringOneDimension(p1.getCoordinate(), point.getCoordinate(), fixedDim);
            Integer compare2 = ternaryComparePointsIgnoringOneDimension(p2.getCoordinate(), point.getCoordinate(), fixedDim);
            if (compare1 == null || compare2 == null) {
                // Ignore?
            } else if (compare1 * compare2 > 0) {
                // both on same side - ignore
            } else if (compare1 * compare2 == 0 && !touching) {
                // point touches one or both end points, but we are ignoring touching points
            } else {
                Integer compare = ternaryComparePointsIgnoringOneDimension(p1.getCoordinate(), p2.getCoordinate(), fixedDim);
                if (compare < 0) {
                    sides.add(new Point[]{p1, p2});
                } else {
                    sides.add(new Point[]{p2, p1});
                }
            }
        }
        int intersections = 0;
        for (Point[] side : sides) {
            double crossingValue = crossingAt(side, point, fixedDim, compareDim);
            if (touching && crossingValue == 0) {
                return true;
            }
            if (crossingValue >= 0) {
                intersections += 1;
            }
        }
        return intersections % 2 == 1;
    }

    static double crossingAt(Point[] side, Point point, int fixedDim, int compareDim) {
        double[] c = point.getCoordinate();
        double[] min = new double[]{side[0].getCoordinate()[fixedDim], side[0].getCoordinate()[compareDim]};
        double[] max = new double[]{side[1].getCoordinate()[fixedDim], side[1].getCoordinate()[compareDim]};
        double[] diff = new double[]{max[0] - min[0], max[1] - min[1]};
        if (diff[1] == 0) {
            // touching a line that runs along the fixed dimension
            return 0;
        } else {
            double ratio = (c[1] - min[1]) / diff[1];
            double offset = ratio * diff[0];
            double crossingValue = min[0] + offset;
            return crossingValue - c[0];
        }
    }

    public static Integer ternaryComparePointsIgnoringOneDimension(double[] c1, double[] c2, int ignoreDim) {
        Integer result = null;
        if (c1.length != c2.length) return null;
        for (int i = 0; i < c1.length; i++) {
            if (i != ignoreDim) {
                double diff = c1[i] - c2[i];
                int ans = (diff > 0) ? 1 : ((diff < 0) ? -1 : 0);
                if (result == null) {
                    result = ans;
                } else {
                    if (result != ans) {
                        if (result * ans != 0) return null;
                        else if (result == 0) result = ans;
                    }
                }
            }
        }
        return result;
    }
}
