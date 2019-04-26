package org.neo4j.spatial.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringJoiner;

import static java.lang.String.format;

public interface Polygon {
    int dimension();

    Point[] getPoints();

    boolean isSimple();

    SimplePolygon[] getShells();

    SimplePolygon[] getHoles();

    MultiPolygon withShell(Polygon shell);

    MultiPolygon withHole(Polygon hole);

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

    static void assertAllSameDimension(SimplePolygon... polygons) {
        for (int i = 1; i < polygons.length; i++) {
            if (polygons[0].dimension() != polygons[i].dimension()) {
                throw new IllegalArgumentException(format("Polygon[%d] has different dimension to Polygon[%d]: %d != %d", i, 0, polygons[i].dimension(), polygons[0].dimension()));
            }
        }
    }

    static SimplePolygon[] allShells(Polygon... polygons) {
        ArrayList<SimplePolygon> shells = new ArrayList<>();
        for (Polygon polygon : polygons) {
            if (polygon.isSimple()) {
                shells.add(((SimplePolygon) polygon));
            } else {
                Collections.addAll(shells, polygon.getShells());
            }
        }
        return shells.toArray(new SimplePolygon[shells.size()]);
    }

    static SimplePolygon[] allHoles(Polygon... polygons) {
        ArrayList<SimplePolygon> holes = new ArrayList<>();
        for (Polygon polygon : polygons) {
            if (!polygon.isSimple()) {
                Collections.addAll(holes, polygon.getHoles());
            }
        }
        return holes.toArray(new SimplePolygon[holes.size()]);
    }

    static Point[] allPoints(Polygon... polygons) {
        ArrayList<Point> points = new ArrayList<>();
        for (Polygon polygon : polygons) {
            if (polygon.isSimple()) {
                Collections.addAll(points, polygon.getPoints());
            } else {
                for (SimplePolygon shell : polygon.getShells()) {
                    Collections.addAll(points, shell.getPoints());
                }
                for (SimplePolygon hole : polygon.getHoles()) {
                    Collections.addAll(points, hole.getPoints());
                }
            }
        }
        return points.toArray(new Point[points.size()]);
    }

    interface SimplePolygon extends Polygon {
        @Override
        default boolean isSimple() {
            return true;
        }

        @Override
        default SimplePolygon[] getHoles() {
            return new SimplePolygon[0];
        }

        @Override
        default SimplePolygon[] getShells() {
            return new SimplePolygon[]{this};
        }

        @Override
        default MultiPolygon withShell(Polygon shell) {
            return new MultiPolygon(Polygon.allShells(this, shell), new SimplePolygon[0]);
        }

        @Override
        default MultiPolygon withHole(Polygon hole) {
            return new MultiPolygon(new SimplePolygon[]{this}, Polygon.allShells(hole));
        }

        static boolean areEqual(SimplePolygon one, SimplePolygon other) {
            PolygonUtil<Point> utils = new PolygonUtil<>();
            Point[] a = utils.openRing(one.getPoints());
            Point[] b = utils.openRing(other.getPoints());

            if (a.length != b.length) {
                return false;
            }

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

        /**
         * Outputs the WKT string of the polygon
         * @return The WKT string describing the polygon
         */
        String toWKT();
    }

    class InMemorySimplePolygon implements SimplePolygon {
        Point[] points;

        private InMemorySimplePolygon(Point... points) {
            this.points = new PolygonUtil<Point>().closeRing(points);
            if (points.length < 4) {
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
        public String toString() {
            return format("InMemorySimplePolygon%s", Arrays.toString(points));
        }

        @Override
        public String toWKT() {
            StringJoiner viewer = new StringJoiner(",", "POLYGON((", "))");
            for (Point point : points) {
                viewer.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
            }
            return viewer.toString();
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

    class MultiPolygon implements Polygon {
        SimplePolygon[] shells;
        SimplePolygon[] holes;

        /* TODO
            Tree structure with root an array of polygons
            Each polygon can contain array of holes
            Each hole can contain array of polygons etc...
        */
        private MultiPolygon(SimplePolygon[] shells, SimplePolygon[] holes) {
            this.shells = shells;
            //TODO Check: holes are contained; shells don't intersect; holes don't intersect; shells contain shells (no hole); holes contain holes (no shell);
            this.holes = holes;
            Polygon.assertAllSameDimension(this.shells);
            Polygon.assertAllSameDimension(this.holes);
            if (holes.length > 0) {
                Polygon.assertAllSameDimension(this.shells[0], this.holes[0]);
            }
        }

        public int dimension() {
            return this.shells[0].dimension();
        }

        @Override
        public Point[] getPoints() {
            return Polygon.allPoints(this);
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public SimplePolygon[] getShells() {
            return this.shells;
        }

        @Override
        public SimplePolygon[] getHoles() {
            return this.holes;
        }

        @Override
        public MultiPolygon withShell(Polygon shell) {
            return new MultiPolygon(Polygon.allShells(this, shell), holes);
        }

        @Override
        public MultiPolygon withHole(Polygon hole) {
            SimplePolygon[] otherHoles = Polygon.allShells(hole);
            SimplePolygon[] newHoles = new SimplePolygon[this.holes.length + otherHoles.length];
            System.arraycopy(this.holes, 0, newHoles, 0, this.holes.length);
            System.arraycopy(otherHoles, 0, newHoles, this.holes.length, otherHoles.length);
            return new MultiPolygon(shells, newHoles);
        }

        @Override
        public String toString() {
            return format("MultiPolygon( shells:%s, holes:%s )", Arrays.toString(shells), Arrays.toString(holes));
        }
    }
}
