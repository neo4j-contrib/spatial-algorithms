package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;

public class Within {

    public static boolean within(Polygon polygon, Point point) {
        return Arrays.stream(polygon.getShells()).filter(s -> within(s, point)).count() > Arrays.stream(polygon.getHoles()).filter(h -> within(h, point)).count();
    }

    /**
     * Checks if a point is inside the given polygon.
     * The logic is based on: https://web.archive.org/web/20161108113341/https://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     *
     * @param polygon
     * @param point
     * @return True iff the points is inside the polygon (not on the edge)
     */
    public static boolean within(Polygon.SimplePolygon polygon, Point point) {
        Point[] points = polygon.getPoints();
        boolean result = false;
        for (int i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].getCoordinate()[1] > point.getCoordinate()[1]) != (points[j].getCoordinate()[1] > point.getCoordinate()[1]) &&
                    (point.getCoordinate()[0] < (points[j].getCoordinate()[0] - points[i].getCoordinate()[0]) * (point.getCoordinate()[1] - points[i].getCoordinate()[1]) / (points[j].getCoordinate()[1] - points[i].getCoordinate()[1]) + points[i].getCoordinate()[0])) {
                result = !result;
            }
        }
        return result;
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
