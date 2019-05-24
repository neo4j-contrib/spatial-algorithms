package org.neo4j.spatial.core;

import java.util.Arrays;
import java.util.StringJoiner;

import static java.lang.String.format;

public interface Polygon {
    static SimplePolygon simple(Point... points) {
        return new InMemorySimplePolygon(points);
    }

    static void assertAllSameDimension(Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }

    SimplePolygon[] getShells();

    SimplePolygon[] getHoles();

    int dimension();

    boolean isSimple();

    /**
     * Outputs the WKT string of the polygon
     *
     * @return The WKT string describing the polygon
     */
    String toWKT();

    interface SimplePolygon extends Polygon {

        /**
         * Converts the given polygon into an array of LineSegments describing this polygon
         *
         * @param polygon
         * @return Array of line segments describing the given polygon
         */
        static LineSegment[] toLineSegments(SimplePolygon polygon) {
            LineSegment[] output = new LineSegment[polygon.getPoints().length - 1];

            for (int i = 0; i < output.length - 1; i++) {
                Point a = polygon.getPoints()[i];
                Point b = polygon.getPoints()[i + 1];
                output[i] = LineSegment.lineSegment(a, b);
            }

            Point a = polygon.getPoints()[polygon.getPoints().length - 2];
            Point b = polygon.getPoints()[0];
            output[polygon.getPoints().length - 2] = LineSegment.lineSegment(a, b);

            return output;
        }

        static boolean areEqual(SimplePolygon one, SimplePolygon other) {
            Point[] a = PolygonUtil.openRing(one.getPoints());
            Point[] b = PolygonUtil.openRing(other.getPoints());

            if (a.length != b.length) {
                return false;
            }

            if (areEqualWithOffset(a, b)) {
                return true;
            }

            //Reverse b
            for (int i = 0; i < b.length / 2; i++) {
                Point temp = b[i];
                b[i] = b[b.length - i - 1];
                b[b.length - i - 1] = temp;
            }

            return areEqualWithOffset(a, b);

        }

        static boolean areEqualWithOffset(Point[] a, Point[] b) {
            Point start = a[0];
            int offset = -1;
            for (int i = 0; i < b.length; i++) {
                if (b[i].equals(start)) {
                    offset = i;
                    break;
                }
            }

            if (offset == -1) {
                return false;
            }

            for (int i = 1; i < a.length; i++) {
                int index = (i + offset) % a.length;
                if (!a[i].equals(b[index])) {
                    return false;
                }
            }
            return true;
        }

        Point[] getPoints();

        @Override
        default SimplePolygon[] getShells() {
            return new SimplePolygon[]{this};
        }

        @Override
        default SimplePolygon[] getHoles() {
            return new SimplePolygon[0];
        }

        /**
         * @return The area of the polygon using the shoelace algorithm
         */
        default double area() {
            Point[] points = getPoints();
            double sum = 0;

            for (int i = 0; i < points.length; i++) {
                double[] a = points[i].getCoordinate();
                double[] b = points[(i + 1) % points.length].getCoordinate();

                sum += (b[0] - a[0]) * (b[1] + a[0]);
            }
            return sum;
        }

        /**
         * @return True iff the points are in clockwise order
         */
        default boolean inClockwiseOrder() {
            return area() > 0;
        }

        @Override
        default String toWKT() {
            return "POLYGON(" + toWKTPointString(false) + ")";
        }

        /**
         * @param hole True if the polygon represents a hole
         * @return Produces a WKT-representation of the polygon without the suffix and in the correct order
         */
        default String toWKTPointString(boolean hole) {
            Point[] points = getPoints();
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            if (hole) {
                if (inClockwiseOrder()) {
                    for (int i = 0; i < points.length; i++) {
                        joiner.add(points[i].getCoordinate()[0] + " " + points[i].getCoordinate()[1]);
                    }
                } else {
                    for (int i = points.length - 1; i >= 0; i--) {
                        joiner.add(points[i].getCoordinate()[0] + " " + points[i].getCoordinate()[1]);
                    }
                }
            } else {
                if (inClockwiseOrder()) {
                    for (int i = points.length - 1; i >= 0; i--) {
                        joiner.add(points[i].getCoordinate()[0] + " " + points[i].getCoordinate()[1]);
                    }
                } else {
                    for (int i = 0; i < points.length; i++) {
                        joiner.add(points[i].getCoordinate()[0] + " " + points[i].getCoordinate()[1]);
                    }
                }
            }

            return joiner.toString();
        }
    }

    class InMemorySimplePolygon implements SimplePolygon {
        Point[] points;

        private InMemorySimplePolygon(Point... points) {
            this.points = PolygonUtil.closeRing(points);
            if (this.points.length < 4) {
                throw new IllegalArgumentException("Polygon cannot have less than 4 points");
            }
            Polygon.assertAllSameDimension(this.points);
        }

        @Override
        public int dimension() {
            return this.points[0].dimension();
        }

        @Override
        public Point[] getPoints() {
            return this.points;
        }

        @Override
        public boolean isSimple() {
            return true;
        }

        @Override
        public String toString() {
            return format("InMemorySimplePolygon%s", Arrays.toString(points));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SimplePolygon && this.equals((SimplePolygon) other);
        }

        public boolean equals(SimplePolygon other) {
            return SimplePolygon.areEqual(this, other);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(points);
        }
    }
}
