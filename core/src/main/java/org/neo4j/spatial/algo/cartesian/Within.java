package org.neo4j.spatial.algo.cartesian;

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
     * Copyright (c) 1970-2003, Wm. Randolph Franklin
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
     *
     * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimers.
     * 2. Redistributions in binary form must reproduce the above copyright notice in the documentation and/or other materials provided with the distribution.
     * 3. The name of W. Randolph Franklin may not be used to endorse or promote products derived from this Software without specific prior written permission.
     *
     * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
