package org.neo4j.spatial.algo.cartesian;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;

public class CartesianWithin {
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
}
