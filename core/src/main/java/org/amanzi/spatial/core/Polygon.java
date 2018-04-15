package org.amanzi.spatial.core;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
        return new SimplePolygon(points);
    }

    static Point[] closeRing(Point... points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("Cannot close ring of less than 2 points");
        }
        Point first = points[0];
        Point last = points[points.length - 1];
        if (first.equals(last)) {
            return points;
        } else {
            Point[] closed = Arrays.copyOf(points, points.length + 1);
            closed[points.length] = points[0];
            return closed;
        }
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

    class SimplePolygon implements Polygon {
        Point[] points;

        private SimplePolygon(Point... points) {
            this.points = Polygon.closeRing(points);
            if (points.length < 3) {
                throw new IllegalArgumentException("Polygon cannot have less than 3 points");
            }
            Polygon.assertAllSameDimension(this.points);
        }

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
        public SimplePolygon[] getShells() {
            return new SimplePolygon[]{this};
        }

        @Override
        public SimplePolygon[] getHoles() {
            return new SimplePolygon[0];
        }

        @Override
        public MultiPolygon withShell(Polygon shell) {
            return new MultiPolygon(Polygon.allShells(this, shell), new SimplePolygon[0]);
        }

        @Override
        public MultiPolygon withHole(Polygon hole) {
            return new MultiPolygon(new SimplePolygon[]{this}, Polygon.allShells(hole));
        }

        @Override
        public String toString() {
            return format("SimplePolygon%s", Arrays.toString(points));
        }
    }

    class MultiPolygon implements Polygon {
        SimplePolygon[] shells;
        SimplePolygon[] holes;

        private MultiPolygon(SimplePolygon[] shells, SimplePolygon[] holes) {
            this.shells = shells;
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
